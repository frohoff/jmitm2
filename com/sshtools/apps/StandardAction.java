/*
 *  Gruntspud
 *
 *  Copyright (C) 2002 Brett Smith.
 *
 *  Written by: Brett Smith <t_magicthize@users.sourceforge.net>
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
package com.sshtools.apps;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.event.EventListenerList;


/**
 *  <p>
 *
 *  Extended Action class providing some useful methods.</p>
 *
 *@author     lee
 *@created    31 August 2002
 */
public abstract class StandardAction extends AbstractAction {
    /**
     * Use a type <code>Boolean</code> to determine if the action exists on
     * the tool bar
     */
    public final static String ON_TOOLBAR = "onToolBar";

    /**
     * Use a type <code>Integer</code> to determine the group number the action
     * resides in. On the tool bar, actions with the same group number are
     * grouped together between tool bar separators.
     */
    public final static String TOOLBAR_GROUP = "toolBarGroup";

    /**
     * When actions on the tool bar are within the same group, this value
     * of type <code>Integer</code> determines the actions weighting within
     * the group. The lower the weight value, the more to the left the action
     * will appear.
     */
    public final static String TOOLBAR_WEIGHT = "toolBarWeight";

    /**
     * Use a type <code>Boolean</code> to determine if the action exists on
     * the menu bat
     */
    public final static String ON_MENUBAR = "onMenuBar";

    /**
     * The name of the menu that the action should exist in. Other actions of
     * the same name will appear on the same menu
     */
    public final static String MENU_NAME = "menuName";

    /**
     * Use a type <code>Integer</code> to determine the group number the action
     * resides in. On the menu specified by <code>MENU_NAME</code> actions with
     * the same group number are  grouped together between menu separators.
     */
    public final static String MENU_ITEM_GROUP = "menuItemGroup";

    /**
     * When actions on the menu specified by <code>MENU_NAME</code> are
     * within the same group, this value of type <code>Integer</code>
     * determines the actions weighting within the group. The lower the
     * weight value, the more to the top the action will appear.
     */
    public final static String MENU_ITEM_WEIGHT = "menuItemWeight";

    /**
     *  The imge directory path
     */
    public final static String IMAGE_DIR = "/com/sshtools/sshterm/";

    /**
     *  The key used for storing a large icon for the action, used for toolbar
     *  buttons. <p>
     */
    public final static String LARGE_ICON = "LargeIcon";

    // The listener to action events (usually the main UI)
    private EventListenerList listeners;

    /**
     *  Gets the value from the key Action.ACTION_COMMAND_KEY
     *
     *@return    The actionCommand value
     */
    public String getActionCommand() {
        return (String)getValue(Action.ACTION_COMMAND_KEY);
    }

    /**
     *  Gets the value from the key Action.SHORT_DESCRIPTION
     *
     *@return    The shortDescription value
     */
    public String getShortDescription() {
        return (String)getValue(Action.SHORT_DESCRIPTION);
    }

    /**
     *  Gets the value from the key Action.LONG_DESCRIPTION
     *
     *@return    The longDescription value
     */
    public String getLongDescription() {
        return (String)getValue(Action.LONG_DESCRIPTION);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getName() {
        return (String)getValue(Action.NAME);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String getSmallIcon() {
        return (String)getValue(Action.SMALL_ICON);
    }

    /**
     *  Forwards the ActionEvent to the registered listener.
     *
     *@param  evt  Description of the Parameter
     */
    public void actionPerformed(ActionEvent evt) {
        if(listeners != null) {
            Object[] listenerList = listeners.getListenerList();

            // Recreate the ActionEvent and stuff the value of the ACTION_COMMAND_KEY
            ActionEvent e = new ActionEvent(evt.getSource(), evt.getID(),
                    (String)getValue(Action.ACTION_COMMAND_KEY));

            for(int i = 0; i <= (listenerList.length - 2); i += 2)
                ((ActionListener)listenerList[i + 1]).actionPerformed(e);
        }
    }

    /**
     *  Adds a feature to the ActionListener attribute of the StandardAction
     *  object
     *
     *@param  l  The feature to be added to the ActionListener attribute
     */
    public void addActionListener(ActionListener l) {
        if(listeners == null)
            listeners = new EventListenerList();

        listeners.add(ActionListener.class, l);
    }

    /**
     *  Description of the Method
     *
     *@param  l  Description of the Parameter
     */
    public void removeActionListener(ActionListener l) {
        if(listeners == null)
            return;

        listeners.remove(ActionListener.class, l);
    }

    /**
     *  Returns the Icon associated with the name from the resources. The
     *  resouce should be in the path.
     *
     *@param  name  Name of the icon file i.e., help16.gif
     *@return       the name of the image or null if the icon is not found.
     */
    public ImageIcon getIcon(String name) {
        String imagePath = name.startsWith("/") ? name : (IMAGE_DIR + name);
        URL url = this.getClass().getResource(imagePath);

        if(url != null)
            return new ImageIcon(url);

        return null;
    }
}
