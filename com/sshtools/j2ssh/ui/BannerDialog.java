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

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import com.sshtools.j2ssh.ui.*;

/**
 *  <p>Displays an authentication banner</p>
 *
 *@author     Richard Pernavas (<A HREF="mailto:richard@sshtools.com">
 *      richard@sshtools.com</A> )
 *@author     Brett Smith
 *@created    31 August 2002
 *@version    $Id: BannerDialog.java,v 1.1 2003/02/11 21:34:08 t_magicthize Exp $
 */

public class BannerDialog extends JDialog {

    //  Statics
    final static String BANNER_ICON = "/com/sshtools/j2ssh/ui/largebanner.png";

    //  Private instance variables
    private JTextArea text;

    /**
     *  Constructs the BannerDialog object
     *
     *@param  bannerText  The SSH banner message text
     */
    public BannerDialog(String bannerText) {
        super((Frame)null, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    /**
     *  Constructs the BannerDialog object
     *
     *@param  parent      Parent frame
     *@param  bannerText  The SSH banner message text
     */
    public BannerDialog(Frame parent, String bannerText) {
        super(parent, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    /**
     *  Constructs the BannerDialog object
     *
     *@param  parent      Parent dialog
     *@param  bannerText  The SSH banner message text
     */
    public BannerDialog(Dialog parent, String bannerText) {
        super(parent, "SSH Authentication - Banner Message", true);
        init(bannerText);
    }

    /**
     * Initialise the dialog
     */
    void init(String bannerText) {
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    //
        setText(bannerText);
    }

    /**
     * Convenience method to work out the parent window given a component and
     * show the dialog
     *
     * @param parent parent component
     * @param text banner text
     */
    public static void showBannerDialog(Component parent, String bannerText) {
        Window w = (Window)SwingUtilities.getAncestorOfClass(Window.class, parent);
        BannerDialog dialog = null;
        if(w instanceof Frame)
            dialog = new BannerDialog((Frame)w, bannerText);
        else if(w instanceof Dialog)
            dialog = new BannerDialog((Dialog)w, bannerText);
        else
            dialog = new BannerDialog(bannerText);
        UIUtil.positionComponent(SwingConstants.CENTER, dialog);
        dialog.toFront();
        dialog.setVisible(true);
    }

    /**
     * Set the text for the banner
     *
     * @param text the text to show in the banner
     */
    public void setText(String text) {
        this.text.setText(text);
        this.repaint();
    }

    /**
     *  Initializes the dialogs components
     *
     *@exception  Exception
     */
    void jbInit() throws Exception {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    //  Create the component to display the banner text
        text = new JTextArea();
        text.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        /** @todo make optional - was this changed back? */
//        text.setLineWrap(true);
        text.setEditable(false);
        Font f = new Font("MonoSpaced",
            text.getFont().getStyle(), text.getFont().getSize());
        text.setFont(f);
        JScrollPane textScroller = new JScrollPane(text);

    //  Create the center banner panel
        IconWrapperPanel centerPanel = new IconWrapperPanel(
                        new ResourceIcon(BANNER_ICON), textScroller);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    //  Create the south button panel
        JButton ok = new JButton("Ok");
        ok.setMnemonic('o');
        ok.setDefaultCapable(true);
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            //  I presume this component is not reused?
                dispose();
            }
        });
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(ok);
        getRootPane().setDefaultButton(ok);

    //  Build the main panel
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(centerPanel, BorderLayout.CENTER);
        getContentPane().add(southPanel, BorderLayout.SOUTH);

    //
        setSize(500,245);
        setResizable(false);
    }
}
