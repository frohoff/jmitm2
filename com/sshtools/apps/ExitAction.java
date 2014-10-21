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
import javax.swing.Action;
import com.sshtools.apps.*;
import java.awt.event.*;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 *  Describes the Exit action for the Sshtools applications
 *
 *@author     Brett Smith
 *@created    31 August 2002
 *@version    $Id: ExitAction.java,v 1.2 2003/02/23 15:46:31 t_magicthize Exp $
 */
public class ExitAction extends StandardAction {

    /**
     *  Constructor
     */
    public ExitAction(SshToolsApplication application, SshToolsApplicationContainer container) {
        this.application = application;
        this.container = container;

        putValue(Action.NAME, "Exit");
        putValue(Action.SMALL_ICON, getIcon("exit.png"));
        putValue(Action.SHORT_DESCRIPTION, "Exit");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
            KeyEvent.VK_X, KeyEvent.ALT_MASK));
        putValue(Action.LONG_DESCRIPTION, "Exit this window");
        putValue(Action.MNEMONIC_KEY, new Integer('x'));
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_NAME, "File");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(90));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(90));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(false));
    }

    public void actionPerformed(ActionEvent evt) {
        application.closeContainer(container);
    }

    //

    private SshToolsApplication application;
    private SshToolsApplicationContainer container;
}
