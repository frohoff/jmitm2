/**
 *   Sshtools - Applications
 *
 *   Copyright (C) 2002 Lee David Painter
 *
 *   Written by: 2002 Brett Smith <t_magicthize@users.sourceforge.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.sshtools.apps;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import com.sshtools.j2ssh.ui.ResourceIcon;
import com.sshtools.j2ssh.ui.UIUtil;
import java.lang.reflect.*;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import org.apache.log4j.*;
import java.util.ArrayList;

/**
 *  <p>Provides some common features for SshTools applications such as SshTerm and
 *  ShiFT</p>. When constructing the instance of this class, the <code>Class</code>
 *  of an implementation of <code>SshToolsApplicationFrame</code> must be
 *  provided.
 *
 *@author     Brett Smith (<A HREF="mailto:t_magicthize@users.sourceforge.net">
 *              t_magicthize@users.sourceforge.net</A> )
 *@created    12 January 2002
 *@see com.sshtools.apps.SshToolsApplicationFrame
 *@version    $Id: SshToolsApplication.java,v 1.5 2003/02/22 00:49:03 t_magicthize Exp $
 */

public abstract class SshToolsApplication {

    public final static String PREF_CONNECTION_LAST_HOST = "apps.connection.lastHost";
    public final static String PREF_CONNECTION_LAST_USER = "apps.connection.lastUser";
    public final static String PREF_CONNECTION_LAST_PORT = "apps.connection.lastPort";
    public final static String PREF_CONNECTION_LAST_KEY = "apps.connection.lastKey";

    protected static Vector containers = new Vector();
    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(SshToolsApplication.class);
    protected Class panelClass, defaultContainerClass;

    /**
     * Construct a new application
     *
     * @param panelClass the class used to create the application panel
     * @param defaultContainerClass the class to use when creating a new container
     */
    public SshToolsApplication(Class panelClass, Class defaultContainerClass) {
        this.panelClass = panelClass;
        this.defaultContainerClass = defaultContainerClass;
    }

    /**
     * Return the application name
     *
     * @return application ame
     */
    public abstract String getApplicationName();

    /**
     * Return the application version
     *
     * @return application version
     */
    public abstract String getApplicationVersion();

    /**
     * Return the application large icon
     *
     * @return application large icon
     */
    public abstract Icon getApplicationLargeIcon();

    /**
     * Return the license details for the about box
     *
     * @return about license details
     */
    public abstract String getAboutLicenseDetails();

    /**
     * Return the URL for the about box
     *
     * @return about url
     */
    public abstract String getAboutURL();

    /**
     * Return the authros  for the about box
     *
     * @return authors
     */
    public abstract String getAboutAuthors();

    /**
     *  Exit
     */
    public void exit() {
        log.debug("Exiting application");
        PreferencesStore.savePreferences();
        System.exit(0);
    }

    /**
     * Return the number of containers this application currently has
     *
     * @param container count
     */
    public int getContainerCount() {
        return containers.size();
    }

    /**
     * Return the container at the given index in the stack
     *
     * @param idx index
     * @return container
     */
    public SshToolsApplicationContainer getContainerAt(int idx) {
        return (SshToolsApplicationContainer)containers.elementAt(idx);
    }

    /**
     * Return the container for a given panel
     *
     * @param panel panel
     * @return container
     */
    public SshToolsApplicationContainer getContainerForPanel(SshToolsApplicationPanel panel) {
        for(Iterator i = containers.iterator(); i.hasNext() ; ) {
            SshToolsApplicationContainer c = (SshToolsApplicationContainer)i.next();
            if(c.getApplicationPanel() == panel)
                return c;
        }
        return null;
    }

    /**
     * Check that a frame can close and exit when there are no more frames
     * to close
     *
     * @param frame frame to close
     */
    public void closeContainer(SshToolsApplicationContainer container) {
        log.debug("Asking " + container + " if it can close");
        if(container.getApplicationPanel().canClose()) {
            log.debug("Closing");
            for(Iterator i = containers.iterator(); i.hasNext() ; )
                log.debug(i.next() + " is currently open");
            container.getApplicationPanel().close();
            container.closeContainer();
            containers.removeElement(container);
            if(containers.size() == 0)
                exit();
            else {
                log.debug("Not closing completely because there are containers still open");
                for(Iterator i = containers.iterator(); i.hasNext() ; )
                    log.debug(i.next() + " is still open");
            }
        }
    }

    /**
     *  Create a new container of the default class
     *
     * @throws SshToolsApplicationException if the container can't be created for any reason
     */
    public void newContainer()
        throws SshToolsApplicationException {

        SshToolsApplicationContainer container = null;
        try {
            container = (SshToolsApplicationContainer)defaultContainerClass.newInstance();
            newContainer(container);
        }
        catch(Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }

    /**
     *  Create a new container of the default class
     *
     * @throws SshToolsApplicationException if the container can't be created for any reason
     */
    public void newContainer(SshToolsApplicationContainer container)
        throws SshToolsApplicationException {
        try {
            SshToolsApplicationPanel panel =
                    (SshToolsApplicationPanel)panelClass.newInstance();
            panel.init(this);
            container.init(this, panel);
            panel.setContainer(container);
            if(!container.isContainerVisible())
                container.setContainerVisible(true);
            containers.addElement(container);
        }
        catch(Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }

    /**
     *  Convert a container to a different container
     *
     * @param container container to convert
     * @param newContainerClass new container class
     * @return the new container
     * @throws SshToolsApplicationException if the container can't be converted for any reason
     */
    public SshToolsApplicationContainer convertContainer(SshToolsApplicationContainer container,
            Class newContainerClass)
        throws SshToolsApplicationException {

        log.info("Converting container of class " + container.getClass().getName() + " to " + newContainerClass.getName());
        int idx = containers.indexOf(container);
        if(idx == -1) {
            System.out.println("NOT FOUND CONTAINER = " + container);
            throw new SshToolsApplicationException(
                "Container is not being manager by the application.");
        }
        SshToolsApplicationContainer newContainer = null;
        try {
            container.closeContainer();
            SshToolsApplicationPanel panel = container.getApplicationPanel();
            newContainer = (SshToolsApplicationContainer)newContainerClass.newInstance();
            newContainer.init(this, panel);
            panel.setContainer(newContainer);
            if(!newContainer.isContainerVisible())
                newContainer.setContainerVisible(true);
            containers.setElementAt(newContainer, idx);
            return newContainer;
        }
        catch(Throwable t) {
            throw new SshToolsApplicationException(t);
        }
    }


    /**
     *  The main entry method for an SshTools application. The extending class
     *  should call this from its <code>main</code> method
     *
     * @param prefs the file to store preferencese in
     * @param args command line arguments
     * @param exception
     */
    public static void init(File prefs, String[] args)
                throws Exception {

       ConfigurationLoader.initialize();

       // Set up the nice Gnome icons
       UIManager.put("OptionPane.errorIcon", new ResourceIcon("/com/sshtools/apps/dialog-error4.png"));
       UIManager.put("OptionPane.informationIcon", new ResourceIcon("/com/sshtools/apps/dialog-information.png"));
       UIManager.put("OptionPane.warningIcon", new ResourceIcon("/com/sshtools/apps/dialog-warning2.png"));
       UIManager.put("OptionPane.questionIcon", new ResourceIcon("/com/sshtools/apps/dialog-question3.png"));

        //
        PreferencesStore.init(prefs);
    }
}
