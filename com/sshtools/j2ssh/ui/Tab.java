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

/**
 *  Implementations of this interface are added to <code>Tabber</code>'s2
 */
public interface Tab {

    /**
     *  If the tabber can group tab's, use this method to provide a group name.
     *  This tab will then be grouped together with other tabs of the same name.
     *
     * @return context
     */
    public String getTabContext();

    /**
     *  Return a large icon to use for this tab. <code>null</code> may be
     *  returned
     *
     * @return icon
     */
    public Icon getTabIcon();

    /**
     *  Return the title of the tab
     */
    public String getTabTitle();

    /**
     *  Return the tool tip text
     *
     * @return tool tip text
     */
    public String getTabToolTipText();

    /**
     *  Return the mnenonic
     *
     * @return mnemonic
     */
    public int getTabMnemonic();

    /**
     *  Return the component used for rendering this tab.
     *
     * @return component
     */
    public Component getTabComponent();

    /**
     *  This may be used to make sure any user input on this tab is correct.
     *  The tab may for example show a dialog informing the user of what is
     *  wrong, then return false if something is wrong.
     *
     * @return tab is ok
     */
    public boolean validateTab();

    /**
     *  Apply any user input.
     */
    public void applyTab();

    /**
     *  Invoked when the tab is selected.
     */
    public void tabSelected();
}
