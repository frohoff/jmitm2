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
import java.util.*;
import javax.swing.*;
import com.sshtools.j2ssh.ui.*;
import java.util.ArrayList;
import com.sshtools.j2ssh.authentication.*;
import com.sshtools.j2ssh.transport.*;
import com.sshtools.j2ssh.*;
import com.sshtools.j2ssh.util.*;

/**
 *  Implementation of a tab what allows editing of the host connection properties
 */
public class SshToolsConnectionHostTab extends JPanel implements SshToolsConnectionTab {

    //
    public final static int DEFAULT_PORT = 22;

    //
    protected XTextField jTextHostname = new XTextField();
    protected NumericTextField jTextPort = new NumericTextField(
            new Integer(0), new Integer(65535), new Integer(DEFAULT_PORT) );
    protected XTextField jTextUsername = new XTextField();
    protected JList jListAuths = new JList();
    protected java.util.List methods = new ArrayList();
    protected SshToolsConnectionProfile profile;
    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(SshToolsConnectionHostTab.class);

    //
    public final static String CONNECT_ICON = "/com/sshtools/j2ssh/ui/largeserveridentity.png";
    public final static String AUTH_ICON = "/com/sshtools/j2ssh/ui/largelock.png";
    public final static String SHOW_AVAILABLE = "<Show available methods>";


    public SshToolsConnectionHostTab() {
        super();

    //  Create the main connection details panel
        JPanel mainConnectionDetailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 2, 2, 2);
        gbc.weightx = 1.0;

    //  Host name
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            new JLabel("Hostname"), gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            jTextHostname, gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

    //  Port
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            new JLabel("Port"), gbc, GridBagConstraints.REMAINDER);
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            jTextPort, gbc, GridBagConstraints.REMAINDER);

    //  Username
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            new JLabel("Username"), gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(mainConnectionDetailsPanel,
            jTextUsername, gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.NONE;

    //
        IconWrapperPanel iconMainConnectionDetailsPanel = new IconWrapperPanel(
            new ResourceIcon(CONNECT_ICON), mainConnectionDetailsPanel);


    //  Authentication methods panel
        JPanel authMethodsPanel = new JPanel(new GridBagLayout());
        authMethodsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;

    //  Authentication methods
        UIUtil.jGridBagAdd(authMethodsPanel,
            new JLabel("Authentication Methods"), gbc, GridBagConstraints.REMAINDER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        jListAuths.setVisibleRowCount(5);
        UIUtil.jGridBagAdd(authMethodsPanel,
            new JScrollPane(jListAuths), gbc, GridBagConstraints.REMAINDER);

    //
        IconWrapperPanel iconAuthMethodsPanel = new IconWrapperPanel(
            new ResourceIcon(AUTH_ICON), authMethodsPanel);

    //  This panel
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        UIUtil.jGridBagAdd(this, iconMainConnectionDetailsPanel,
                                            gbc, GridBagConstraints.REMAINDER);
        gbc.weighty = 1.0;
        UIUtil.jGridBagAdd(this, iconAuthMethodsPanel,
                                        gbc, GridBagConstraints.REMAINDER);

    //  Set up the values in the various components
        addAuthenticationMethods();
    }

    public void setConnectionProfile(SshToolsConnectionProfile profile) {
        this.profile = profile;
        jTextHostname.setText(profile.getHost());
        jTextUsername.setText(profile.getUsername());
        jTextPort.setValue(new Integer(profile.getPort()));
    }

    public SshToolsConnectionProfile getConnectionProfile() {
        return profile;
    }

    /**
     *  Sets up the authentication method list
     */
    private void addAuthenticationMethods() {

        java.util.List methods = new ArrayList();

        methods.add(SHOW_AVAILABLE);
        methods.addAll(SshAuthenticationClientFactory.getSupportedMethods());
        jListAuths.setListData(methods.toArray());
        jListAuths.setSelectedIndex(0);

    }

    public String getTabContext() {
        return "Connection";
    }

    public Icon getTabIcon() {
        return null;
    }

    public String getTabTitle() {
        return "Host";
    }

    public String getTabToolTipText() {
        return "The main host connection details.";
    }

    public int getTabMnemonic() {
        return 'h';
    }

    public Component getTabComponent() {
        return this;
    }

    public boolean validateTab() {

        // Validate that we have enough information
        if (jTextHostname.getText().equals("")
                || jTextPort.getText().equals("")
                || jTextUsername.getText().equals("")) {
            JOptionPane.showMessageDialog(this, "Please enter all details!", "Connect", JOptionPane.OK_OPTION);
            return false;
        }

        // Setup the authentications selected
        java.util.List chosen = getChosenAuth();
        if (chosen != null) {

            Iterator it = chosen.iterator();
            while (it.hasNext()) {
                String method = (String)it.next();
                try {
                    SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(method);
                }
                catch(AlgorithmNotSupportedException anse) {
                    JOptionPane.showMessageDialog(this, method + " is not supported!");
                    return false;
                }
            }
        }

        return true;
    }

    private java.util.List getChosenAuth() {
        // Determine whether any authenticaiton methods we selected
        Object[] auths = jListAuths.getSelectedValues();
        String a;
        java.util.List l = new java.util.ArrayList();
        if (auths != null) {

            for (int i = 0; i < auths.length; i++) {
                a = (String) auths[i];
                if (a.equals(SHOW_AVAILABLE)) {
                    return null;
                } else {
                    l.add(a);
                }

            }
        } else {
            return null;
        }
        return l;
    }

    public void applyTab() {
        profile.setHost(jTextHostname.getText());
        profile.setPort(Integer.valueOf(jTextPort.getText()).intValue());
        profile.setUsername(jTextUsername.getText());

        java.util.List chosen = getChosenAuth();
        if (chosen != null) {
            Iterator it = chosen.iterator();
            while (it.hasNext()) {
                String method = (String)it.next();
                try {
                    SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(method);
                    auth.setUsername(jTextUsername.getText());
                    profile.addAuthenticationMethod(auth);
                }
                catch(AlgorithmNotSupportedException anse) {
                    log.error("This should have been caught by validateTab()?", anse);
                }
            }
        }
    }

    public void tabSelected() {
    }
}
