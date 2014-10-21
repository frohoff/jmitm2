/**
 *   Sshtools - Applications
 *
 *   Copyright (C) 2002 Lee David Painter
 *
 *   Written by: 2002 Lee David Painter <lee@sshtools.com>
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import com.sshtools.j2ssh.ui.*;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;

/**
 *
 */

public class SshToolsApplicationFrame extends JFrame
    implements SshToolsApplicationContainer {

    //  Preference names
    public final static String PREF_LAST_FRAME_GEOMETRY = "application.lastFrameGeometry";

    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(SshToolsApplicationFrame.class);

    protected StandardAction exitAction, aboutAction, newWindowAction;

    public void init(final SshToolsApplication application,
                    SshToolsApplicationPanel panel)
                    throws SshToolsApplicationException {

        this.panel = panel;
        this.application = application;

        setTitle(ConfigurationLoader.getVersionString(
        application.getApplicationName(),
        application.getApplicationVersion())); // + " " + application.getApplicationVersion());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        panel.registerActionMenu(new SshToolsApplicationPanel.ActionMenu("File", "File", 'f', 0));
        panel.registerActionMenu(new SshToolsApplicationPanel.ActionMenu("Help", "Help", 'h', 99));
        panel.registerAction(exitAction = new ExitAction(application, this));
        panel.registerAction(newWindowAction = new NewWindowAction(application));
        panel.registerAction(aboutAction = new AboutAction(this, application));
        getApplicationPanel().rebuildActionComponents();

        getContentPane().setLayout(new GridLayout(1, 1));
        getContentPane().add(panel);

            // Watch for the frame closing
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                application.closeContainer(SshToolsApplicationFrame.this);
            }
        });

        // If this is the first frame, center the window on the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        boolean found = false;
        if(application.getContainerCount() != 0) {
            for(int i = 0 ; i < application.getContainerCount() && !found; i++) {
                SshToolsApplicationContainer c = application.getContainerAt(i);
                if(c instanceof SshToolsApplicationFrame) {
                    SshToolsApplicationFrame f = (SshToolsApplicationFrame)c;
                    setSize(f.getSize());
                    Point newLocation = new Point(f.getX(), f.getY());
                    newLocation.x += 48;
                    newLocation.y += 48;
                    if(newLocation.x > screenSize.getWidth() - 64)
                        newLocation.x = 0;
                    if(newLocation.y > screenSize.getHeight() - 64)
                        newLocation.y = 0;
                    setLocation(newLocation);
                    found = true;
                }
            }
        }


        if(!found) {
            // Is there a previous stored geometry we can use?
            if(PreferencesStore.preferenceExists(PREF_LAST_FRAME_GEOMETRY)) {
                setBounds(PreferencesStore.getRectangle(PREF_LAST_FRAME_GEOMETRY,
                            getBounds()));
            }
            else {
                pack();
                UIUtil.positionComponent(SwingConstants.CENTER, this);
            }
        }
    }

    /**
     * Return the application
     *
     * @return application
     */
    public SshToolsApplication getApplication() {
        return application;
    }

    /**
     * Set the container visible (called after <code>init()</code)
     *
     * @param visibile visible
     */
    public void setContainerVisible(boolean visible) {
        setVisible(true);
    }

    /**
     * Return if the container is visible
     *
     * @return visible
     */
    public boolean isContainerVisible() {
        return isVisible();
    }

    /**
     * Return the application panel in this frame
     *
     * @param application
     */
    public SshToolsApplicationPanel getApplicationPanel() {
        return panel;
    }

    public void closeContainer() {
    /*  If this is the last frame to close, then store its geometry for use
        when the next frame opens */
        if(application.getContainerCount() == 1)
            PreferencesStore.putRectangle(PREF_LAST_FRAME_GEOMETRY, getBounds());
        dispose();
        getApplicationPanel().deregisterAction(newWindowAction);
        getApplicationPanel().deregisterAction(exitAction);
        getApplicationPanel().deregisterAction(aboutAction);
        getApplicationPanel().rebuildActionComponents();
    }

    //
    private SshToolsApplicationPanel panel;
    private SshToolsApplication application;
}

