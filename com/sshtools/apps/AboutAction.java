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

import javax.swing.Action;
import com.sshtools.apps.*;
import com.sshtools.j2ssh.ui.*;
import com.sshtools.j2ssh.util.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;

/**
 *  Describes the Abvout action for the SshTerminalFrame
 *
 *@author     Brett Smith
 *@created    31 August 2002
 *@version    $Id: AboutAction.java,v 1.2 2003/02/23 13:43:56 martianx Exp $
 */
public class AboutAction extends StandardAction {

    private final static String ACTION_COMMAND_KEY_ABOUT = "about-command";
    private final static String NAME_ABOUT = "About";
    private final static String SMALL_ICON_ABOUT = "/com/sshtools/apps/about.png";
    private final static String LARGE_ICON_ABOUT = "";
    private final static int MNEMONIC_KEY_ABOUT = 'A';


    /**
     *  Constructor
     */
    public AboutAction(Component parent, SshToolsApplication application) {
        this.application = application;
        this.parent = parent;
        putValue(Action.NAME, NAME_ABOUT);
        putValue(Action.SMALL_ICON, getIcon(SMALL_ICON_ABOUT));
        putValue(LARGE_ICON, getIcon(LARGE_ICON_ABOUT));
        putValue(Action.SHORT_DESCRIPTION, "About "  + application.getApplicationName());
        putValue(Action.LONG_DESCRIPTION, "Show information about "  + application.getApplicationName());
        putValue(Action.MNEMONIC_KEY, new Integer(MNEMONIC_KEY_ABOUT));
        putValue(Action.ACTION_COMMAND_KEY, ACTION_COMMAND_KEY_ABOUT);
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_NAME, "Help");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(90));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(90));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(true));
        putValue(StandardAction.TOOLBAR_GROUP, new Integer(90));
        putValue(StandardAction.TOOLBAR_WEIGHT, new Integer(0));
    }

    /**
     * Show the about dialog
     */
    public void actionPerformed(ActionEvent evt) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        GridBagConstraints gBC = new GridBagConstraints();
        gBC.anchor = GridBagConstraints.CENTER;
        gBC.fill = GridBagConstraints.HORIZONTAL;
        gBC.insets = new Insets(1, 1, 1, 1);
        JLabel a = new JLabel(application.getApplicationName());
        a.setFont(a.getFont().deriveFont(24f));
        UIUtil.jGridBagAdd(p, a, gBC, GridBagConstraints.REMAINDER);
        JLabel v = new JLabel(ConfigurationLoader.getVersionString(application.getApplicationName(),
            application.getApplicationVersion()));
        v.setFont(v.getFont().deriveFont(10f));
        UIUtil.jGridBagAdd(p, v, gBC, GridBagConstraints.REMAINDER);
        MultilineLabel x = new MultilineLabel(application.getAboutAuthors());
        x.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        x.setFont(x.getFont().deriveFont(12f));
        UIUtil.jGridBagAdd(p, x, gBC, GridBagConstraints.REMAINDER);
        MultilineLabel c = new MultilineLabel(application.getAboutLicenseDetails());
        c.setFont(c.getFont().deriveFont(10f));
        UIUtil.jGridBagAdd(p, c, gBC, GridBagConstraints.REMAINDER);
        final JLabel h = new JLabel(application.getAboutURL());
        h.setForeground(Color.blue);
        h.setFont(new Font(h.getFont().getName(), Font.BOLD, 10));
        h.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        h.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                try {
                    BrowserLauncher.openURL(application.getAboutURL());
                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        UIUtil.jGridBagAdd(p, h, gBC, GridBagConstraints.REMAINDER);
        JOptionPane.showMessageDialog(parent, p, "About", JOptionPane.PLAIN_MESSAGE,
            application.getApplicationLargeIcon());
    }

    private SshToolsApplication application;
    private Component parent;

}
