/*
 *  Sshtools - Java SSH2 API
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.sshtools.j2ssh.ui;

import java.awt.*;
import com.sshtools.apps.*;
import java.net.*;
import javax.swing.*;
import java.security.*;
import java.awt.event.*;
import com.sshtools.j2ssh.authentication.*;
import java.util.*;
import com.sshtools.j2ssh.configuration.*;
import com.sshtools.j2ssh.util.*;
import com.sshtools.j2ssh.ui.*;

/**
 *  <p>GUI component for creating a connection profile</p>
 *
 *@author     Brett Smith (<A HREF="mailto:t_magicthize@users.sourceforge.net">
 *      t_magicthize@users.sourceforge.net</A> )
 *@created    8th February 2003
 *@version    $Id: SshToolsConnectionPanel.java,v 1.2 2003/02/21 12:08:03 martianx Exp $
 */

public class SshToolsConnectionPanel extends JPanel {

    //  Strings
    final static String DEFAULT = "<Default>";
    final static int DEFAULT_PORT = 22;

    //
    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(SshToolsConnectionPanel.class);

    /**
     *  Constructs the SshToolsConnectionPanel object
     */
    public SshToolsConnectionPanel() {
        super();

        tabber = new Tabber();

    //  Add the common tabs
        addTab(new SshToolsConnectionHostTab());
        addTab(new SshToolsConnectionProtocolTab());

    //  Build this panel
        setLayout(new GridLayout(1, 1));
        add(tabber);
    }

    /**
     * Validate the tabs
     *
     * @return tabs ok
     */
    public boolean validateTabs() {
        return tabber.validateTabs();
    }

    /**
     * Apply the tabs
     */
    public void applyTabs() {
        tabber.applyTabs();
    }

    /**
     * Add a tab
     *
     * @param tab tab to add
     */
    public void addTab(SshToolsConnectionTab tab) {
        tabber.addTab(tab);
    }

    /**
     * Set the connection profile
     *
     * @param profile profile to set
     */
    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;
        for(int i = 0 ; i < tabber.getTabCount() ; i++)
            ((SshToolsConnectionTab)tabber.getTabAt(i)).setConnectionProfile(profile);
    }

    /**
     * Shows the connection dialog and returns the connection profile
     * selected.
     *
     * @param parent parent component
     * @param optionalTabs any optional tabs (or <code>null</code>)
     * @return the connection profile
     */
    public static SshToolsConnectionProfile showConnectionDialog(Component parent, SshToolsConnectionTab[] optionalTabs) {
        return showConnectionDialog(parent, null, optionalTabs);
    }

    /**
     *  Shows the connection dialog and returns the connection properties
     *  selected.
     *
     * @param parent parent component
     * @param profile profile or <code>null</code> to create a new one
     * @param optionalTabs any optional tabs (or <code>null</code>)
     * @return profile
     */
    public static SshToolsConnectionProfile showConnectionDialog(Component parent,
                            SshToolsConnectionProfile profile, SshToolsConnectionTab[] optionalTabs) {

        //  If no properties are provided, then use the default
        if(profile == null) {
            profile = new SshToolsConnectionProfile();
            profile.setHost(PreferencesStore.get(SshToolsApplication.PREF_CONNECTION_LAST_HOST, ""));
            profile.setPort(PreferencesStore.getInt(
                SshToolsApplication.PREF_CONNECTION_LAST_PORT ,DEFAULT_PORT));
            profile.setUsername(PreferencesStore.get(SshToolsApplication.PREF_CONNECTION_LAST_USER, ""));
        }

        final SshToolsConnectionPanel conx = new SshToolsConnectionPanel();

        if(optionalTabs != null) {
            for(int i = 0 ; i < optionalTabs.length ; i++) {
                conx.addTab(optionalTabs[i]);
            }
        }
        conx.setConnectionProfile(profile);

        JDialog d = null;
        Window w = (Window)SwingUtilities.getAncestorOfClass(Window.class, parent);
        if(w instanceof JDialog)
            d = new JDialog((JDialog)w, "Connection Profile", true);
        else if(w instanceof JFrame)
            d = new JDialog((JFrame)w, "Connection Profile", true);
        else
            d = new JDialog((JFrame)null, "Connection Profile", true);
        final JDialog dialog = d;


        class UserAction {
            boolean connect;
        }
        final UserAction userAction  = new UserAction();

    //  Create the bottom button panel
        final JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dialog.setVisible(false);
            }
        });
        final JButton connect = new JButton("Connect");
        connect.setMnemonic('t');
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(conx.validateTabs()) {
                    userAction.connect = true;
                    dialog.setVisible(false);
                }
            }
        });
        dialog.getRootPane().setDefaultButton(connect);
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(6, 6, 0, 0);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(buttonPanel, connect, gbc, GridBagConstraints.RELATIVE);
        UIUtil.jGridBagAdd(buttonPanel, cancel, gbc, GridBagConstraints.REMAINDER);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.add(buttonPanel);

    //
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(conx, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);


        // Show the dialog
        dialog.getContentPane().setLayout(new GridLayout(1, 1));
        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        dialog.setResizable(false);
        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        dialog.setVisible(true);

        if(!userAction.connect)
          return null;

        conx.applyTabs();

        // Make sure we didn't cancel
        PreferencesStore.put(SshToolsApplication.PREF_CONNECTION_LAST_HOST,
                profile.getHost());
        PreferencesStore.put(SshToolsApplication.PREF_CONNECTION_LAST_USER,
                profile.getUsername());
        PreferencesStore.putInt(SshToolsApplication.PREF_CONNECTION_LAST_PORT,
                profile.getPort());

        // Return the connection properties
        return profile;
    }

    //
    private Tabber tabber;
    private SshToolsConnectionProfile profile;
}
