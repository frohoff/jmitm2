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
import java.awt.event.*;
import com.sshtools.apps.*;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 *  Describes the New window action for the Sshtools applications
 *
 *@author     Brett Smith
 *@created    31 August 2002
 *@version    $Id: NewWindowAction.java,v 1.2 2003/02/23 15:46:31 t_magicthize Exp $
 */
public class NewWindowAction extends StandardAction {

    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(NewWindowAction.class);
    /**
     *  Constructor
     */
    public NewWindowAction(SshToolsApplication application) {
        this.application = application;

        putValue(Action.NAME, "New Window");
        putValue(Action.SMALL_ICON, getIcon("newwindow.png"));
        putValue(LARGE_ICON, getIcon("largenewwindow.png"));
        putValue(Action.SHORT_DESCRIPTION, "Create new window");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
            KeyEvent.VK_W, KeyEvent.ALT_MASK));
        putValue(Action.LONG_DESCRIPTION, "Create a new SSHTerm window");
        putValue(Action.MNEMONIC_KEY, new Integer('w'));
        putValue(Action.ACTION_COMMAND_KEY, "new-window");
        putValue(StandardAction.ON_MENUBAR, new Boolean(true));
        putValue(StandardAction.MENU_NAME, "File");
        putValue(StandardAction.MENU_ITEM_GROUP, new Integer(0));
        putValue(StandardAction.MENU_ITEM_WEIGHT, new Integer(90));
        putValue(StandardAction.ON_TOOLBAR, new Boolean(true));
        putValue(StandardAction.TOOLBAR_GROUP, new Integer(0));
        putValue(StandardAction.TOOLBAR_WEIGHT, new Integer(90));
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            application.newContainer();
        }
        catch(SshToolsApplicationException stae) {
            log.error(stae);
        }
    }

    //

    private SshToolsApplication application;
}
