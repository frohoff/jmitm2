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


/**
 *
 */

public abstract class SshToolsApplicationPanel extends JPanel  {

    protected org.apache.log4j.Logger log =
            org.apache.log4j.Logger.getLogger(SshToolsApplicationPanel.class);
    protected SshToolsApplication application;
    protected JMenuBar menuBar;
    protected JToolBar toolBar;
    protected SshToolsApplicationContainer container;
    protected Vector actions = new Vector();
    private Vector actionMenus = new Vector();

    /**
     * Check if the application can close
     *
     * @return can close
     */
    public abstract boolean canClose();

    /**
     * Close the application panel (clean up should be done here)
     *
     * @return can close
     */
    public abstract void close();

    /**
     * Return the application container for this panel
     *
     * @return container
     */
    public SshToolsApplicationContainer getContainer() {
        return container;
    }

    /**
     * Invoked when the container changes
     *
     * @return container
     */
    public void setContainer(SshToolsApplicationContainer container) {
        this.container = container;
    }

    /**
     * Register a new action menu that may be used to group a whole load of
     * actions
     *
     * @param actionMenu action menu to register
     */
    public void registerActionMenu(ActionMenu actionMenu) {
        ActionMenu current = getActionMenu(actionMenu.name);
        if(current == null)
            actionMenus.addElement(actionMenu);
    }

    /**
     * Return the action menu was the specified name or <code>null</code> if
     * no action menu with that name can be found
     *
     * @param actionMenuName name of action menu
     */
    public ActionMenu getActionMenu(String actionMenuName) {
        return getActionMenu(actionMenus.iterator(), actionMenuName);
    }

    /**
     * Return the action menu was the specified name or <code>null</code> if
     * no action menu with that name can be found
     *
     * @param actions iterator of actions to search
     * @param actionMenuName name of action menu to find
     */
    private ActionMenu getActionMenu(Iterator actions, String actionMenuName) {
        while(actions.hasNext()) {
            ActionMenu a = (ActionMenu)actions.next();
            if(a.name.equals(actionMenuName))
                return a;
        }
        return null;
    }

    /**
     * Deregister an action.
     *
     * @param action action to deregister
     */
    public void deregisterAction(StandardAction action) {
        actions.removeElement(action);
    }

    /**
     * Register an action.
     *
     * @param action action to register
     */
    public void registerAction(StandardAction action) {
        actions.addElement(action);
    }

    /**
     * Invoked by the application framework when the frame is created
     *
     * @param application
     */
    public void init(SshToolsApplication application)
        throws SshToolsApplicationException {
        this.application = application;

        menuBar = new JMenuBar();

        // Creat the tool bar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE);

        rebuildActionComponents();
    }

    /**
     * Rebuild the components that provide a UI for actions
     *
     * @param action
     */
    public void rebuildActionComponents() {
    //  Clear the current state of the component
        toolBar.removeAll();

    //  Build the tool bar, grouping the actions
        Vector v = new Vector();
        for(Iterator i = actions.iterator(); i.hasNext(); ) {
            StandardAction a = (StandardAction)i.next();
            if(Boolean.TRUE.equals((Boolean)a.getValue(StandardAction.ON_TOOLBAR)))
                v.addElement(a);
        }
        Collections.sort(v, new ToolBarActionComparator());
        Integer grp = null;
        for(Iterator i  = v.iterator() ; i.hasNext() ; ) {
            StandardAction z = (StandardAction)i.next();
            if(grp != null && !grp.equals(
                (Integer)z.getValue(StandardAction.TOOLBAR_GROUP)))
                toolBar.add(new ToolBarSeparator());
            toolBar.add(new ToolButton(z));
            grp = (Integer)z.getValue(StandardAction.TOOLBAR_GROUP);
        }
        toolBar.revalidate();
        toolBar.repaint();

    //  Build the menu bar
        menuBar.removeAll();
        v.removeAllElements();
        for(Enumeration e = actions.elements();
                e.hasMoreElements(); )        {
            StandardAction a = (StandardAction)e.nextElement();
            if(Boolean.TRUE.equals((Boolean)a.getValue(StandardAction.ON_MENUBAR))) {
                log.debug("Adding action " + a.getName() + " to menubar");
                v.addElement(a);
            }
            else
                log.debug("Action " + a.getName() + " is not on the menubar");
        }
        Vector menus = (Vector)actionMenus.clone();
        Collections.sort(menus);
        HashMap map = new HashMap();
        for(Iterator i = v.iterator(); i.hasNext(); ) {
            StandardAction z = (StandardAction)i.next();
            String menuName = (String)z.getValue(StandardAction.MENU_NAME);
            log.debug("Adding action " + z.getName() + " to menu " + menuName);
            if(menuName == null)
                log.error("Action " + z.getName() +
                " doesnt specify a value for " + StandardAction.MENU_NAME);
            else {
                String m = (String)z.getValue(StandardAction.MENU_NAME);
                log.debug("Looking for menu  " + m);
                ActionMenu menu = getActionMenu(menus.iterator(), m);
                if(menu == null)
                    log.error("Action menu " + z.getName() +
                    " does not exist");
                else {
                    Vector x = (Vector)map.get(menu.name);
                    if(x == null) {
                        x = new Vector();
                        log.debug("Creating new menu " + menu.name);
                        map.put(menu.name, x);
                    }
                    x.addElement(z);
                }
            }
        }
        for(Iterator i = menus.iterator() ; i.hasNext(); ) {
            ActionMenu m = (ActionMenu)i.next();
            log.debug("Building menu " + m.name);
            Vector x = (Vector)map.get(m.name);
            if(x != null) {
                Collections.sort(x, new MenuItemActionComparator());
                JMenu menu = new JMenu(m.displayName);
                menu.setMnemonic(m.mnemonic);
                grp = null;
                for(Iterator j = x.iterator(); j.hasNext(); ) {
                    StandardAction a = (StandardAction)j.next();
                    log.debug("Adding action " + a.getName() + " to " + menu.getText());
                    Integer g = (Integer)a.getValue(StandardAction.MENU_ITEM_GROUP);
                    if(grp != null && !g.equals(grp))
                        menu.addSeparator();
                    grp = g;
                    JMenuItem item = new JMenuItem(a);
                    menu.add(item);
                }
                menuBar.add(menu);
            }
            else
                log.error("Can't find menu " + m.name);
        }
        menuBar.validate();
        menuBar.repaint();
    }

    /**
     * Return the tool bar
     *
     * @return tool bar
     */
    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * Return the menu bar
     *
     * @return tool bar
     */
    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    /**
     * Return the application framework that created the frame
     *
     * @param application
     */
    public SshToolsApplication getApplication() {
        return application;
    }

    public static class ActionMenu implements Comparable {
        public ActionMenu(String name, String displayName, int mnemonic, int weight) {
            this.name = name;
            this.displayName = displayName;
            this.mnemonic = mnemonic;
            this.weight = weight;
        }

        public int compareTo(Object o) {
            int i = new Integer(weight).compareTo(new Integer(((ActionMenu)o).weight));
            return i == 0 ? displayName.compareTo(((ActionMenu)o).displayName) : i;
        }

        int weight, mnemonic;
        String name, displayName;
    }

    class ToolBarActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i =((Integer)((StandardAction)o1).getValue(
                    StandardAction.TOOLBAR_GROUP)).compareTo(
                    (Integer)((StandardAction)o2).getValue(
                    StandardAction.TOOLBAR_GROUP));
            return i == 0 ? ((Integer)((StandardAction)o1).getValue(
                        StandardAction.TOOLBAR_WEIGHT)).compareTo(
                        (Integer)((StandardAction)o2).getValue(
                        StandardAction.TOOLBAR_WEIGHT)) : i;
        }
    }

    class MenuItemActionComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            int i =((Integer)((StandardAction)o1).getValue(
                    StandardAction.MENU_ITEM_GROUP)).compareTo(
                    (Integer)((StandardAction)o2).getValue(
                    StandardAction.MENU_ITEM_GROUP));
            return i == 0 ? ((Integer)((StandardAction)o1).getValue(
                        StandardAction.MENU_ITEM_WEIGHT)).compareTo(
                        (Integer)((StandardAction)o2).getValue(
                        StandardAction.MENU_ITEM_WEIGHT)) : i;
        }
    }
}

