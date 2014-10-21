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
import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.border.*;
import com.sshtools.j2ssh.ui.*;
import com.sshtools.j2ssh.util.*;
import com.sshtools.apps.*;

/**
 *  <p>
 *
 *  Displays the list of availble authentication methods and allows the user to
 *  select on or more to authenticate with.</p>
 *
 *@author     Lee David Painter (<A HREF="mailto:lee@sshtools.com">
 *      lee@sshtools.com</A> )
 *@author     Brett Smith
 *@created    31 August 2002
 *@version    $Id: AuthenticationDialog.java,v 1.2 2003/02/23 11:23:29 martianx Exp $
 */

public class AuthenticationDialog extends JDialog {

    /**
     *  The dialog components
     */
    JList jListAuths = new JList();
    JLabel messageLabel = new JLabel();
    boolean cancelled = false;


    /**
     *  The Constructor
     */
    public AuthenticationDialog() {
        super((Frame)null, "Select Authentication Method(s)", true);
        init();
    }


    /**
     *  The Constructor
     *
     *@param  frame  The parent frame
     */
    public AuthenticationDialog(Frame frame) {
        super(frame, "Select Authentication Method(s)", true);
        init();
    }


    /**
     *  The Constructor
     *
     *@param  dialog  The parent dialog
     */
    public AuthenticationDialog(Dialog dialog) {
        super(dialog, "Select Authentication Method(s)", true);
        init();
    }

    /**
     * Initialise
     */
    void init() {
        try {
            jbInit();
            pack();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  Sets the authentication list to be displayed
     *
     *@param  methods  A list of methods
     */
    private void setMethodList(java.util.List methods) {

        jListAuths.setListData(methods.toArray());
        if(methods.size() > 0)
            jListAuths.setSelectedIndex(0);
    }


    /**
     *  Initiates the dialog
     *
     *@throws  Exception
     */
    void jbInit() throws Exception {
    //
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    //
        messageLabel.setForeground(Color.red);
        messageLabel.setHorizontalAlignment(JLabel.CENTER);

    //  Create the list of available methods and put in in a scroll panel
        jListAuths = new JList();
        jListAuths.setVisibleRowCount(5);
        JPanel listPanel = new JPanel(new GridLayout(1, 1));
        listPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        listPanel.add(new JScrollPane(jListAuths));

    //  Main panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        centerPanel.add(new JLabel(
            "Please select an authentication method(s) to continue."),
            BorderLayout.NORTH);
        centerPanel.add(listPanel, BorderLayout.CENTER);

    //  Create the bottom button panel
        JButton proceed = new JButton("Proceed");
        proceed.setMnemonic('p');
        proceed.setDefaultCapable(true);
        proceed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            //  I presume this component **is** reused?
                hide();
            }
        });
        getRootPane().setDefaultButton(proceed);

        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic('c');
        cancel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            cancelled = true;
            hide();
          }
        });

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        southPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        southPanel.add(cancel);
        southPanel.add(proceed);


    //  Create the center banner panel
        IconWrapperPanel iconPanel = new IconWrapperPanel(
                        new ResourceIcon(SshToolsConnectionHostTab.AUTH_ICON), centerPanel);
        iconPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    //  The main panel contains everything and is surrounded by a border
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(messageLabel, BorderLayout.NORTH);
        mainPanel.add(iconPanel, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

    //  Build the main panel
        getContentPane().setLayout(new GridLayout(1, 1));
        getContentPane().add(mainPanel);
    }

    /**
     * Convenience method to work out the parent window given a component and
     * show the dialog
     *
     * @param parent parent component
     * @param support support auth. method
     * @return list selected auth. methods
     */
    public static java.util.List showAuthenticationDialog(Component parent, java.util.List support) {
        return showAuthenticationDialog(parent, support, null);
    }

    /**
     * Convenience method to work out the parent window given a component and
     * show the dialog
     *
     * @param parent parent component
     * @param support support auth. method
     * @param message message
     * @return list selected auth. methods
     */
    public static java.util.List showAuthenticationDialog(
        Component parent, java.util.List support, String message) {
        Window w = (Window)SwingUtilities.getAncestorOfClass(Window.class, parent);
        AuthenticationDialog dialog = null;
        if(w instanceof Frame)
            dialog = new AuthenticationDialog((Frame)w);
        else if(w instanceof Dialog)
            dialog = new AuthenticationDialog((Dialog)w);
        else
            dialog = new AuthenticationDialog();
        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        return dialog.showAuthenticationMethods(support, message);
    }

    /**
     *  Call this method to show the dialog and return the list of selected
     *  methods
     *
     *@param  supported
     *@return
     */
    public java.util.List showAuthenticationMethods(
                        java.util.List supported, String message) {

        // Set the list
        this.setMethodList(supported);

        // Show the dialog
        UIUtil.positionComponent(SwingConstants.CENTER, this);
        if(message != null) {
            messageLabel.setVisible(true);
            messageLabel.setText(message);
        }
        else {
            messageLabel.setVisible(false);
        }
        pack();
        toFront();
        setVisible(true);

        // Put the selected values into a new list and return
        java.util.List list = new ArrayList();

        if(!cancelled) {
          Object[] methods = jListAuths.getSelectedValues();

          if (methods != null) {
            for (int i = 0; i < methods.length; i++) {
              list.add(methods[i]);
            }
          }
        }

        return list;
    }


    /**
     *  Handles the proceed button event
     *
     *@param  e
     */
    void jButtonProceed_actionPerformed(ActionEvent e) {

        hide();
    }

}
