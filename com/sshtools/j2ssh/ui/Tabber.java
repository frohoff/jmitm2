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
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *  Provides a container for implementations of the <code>Tab</code> interface.
 *  Tabs are added to this container using the <code>addTab(tab)</code> method.
 *
 * @author     Brett Smith (<A HREF="mailto:t_magicthize@users.sourceforge.net">
 *      t_magicthize@users.sourceforge.net</A> )
 * @created    8th February 2003
 * @version    $Id: Tabber.java,v 1.1 2003/02/11 21:34:08 t_magicthize Exp $
 */
public class Tabber extends JTabbedPane {
    /**
     *  Constructor for the Tabber object
     */
    public Tabber() {
        this(TOP);
    }

    /**
     *  Constructor for the Tabber object
     *
     *@param  tabPlacement  Description of the Parameter
     */
    public Tabber(int tabPlacement) {
        super(tabPlacement);
        addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if(getSelectedIndex() != -1)
                        getTabAt(getSelectedIndex()).tabSelected();
                }
            });
    }

    /**
     *  Gets the tabAt attribute of the Tabber object
     *
     *@param  i  Description of the Parameter
     *@return    The tabAt value
     */
    public Tab getTabAt(int i) {
        return ((TabPanel)getComponentAt(i)).getTab();
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean validateTabs() {
        for(int i = 0; i < getTabCount(); i++) {
            Tab tab = ((TabPanel)getComponentAt(i)).getTab();

            if(!tab.validateTab()) {
                setSelectedIndex(i);

                return false;
            }
        }

        return true;
    }

    /**
     *  Description of the Method
     */
    public void applyTabs() {
        for(int i = 0; i < getTabCount(); i++) {
            Tab tab = ((TabPanel)getComponentAt(i)).getTab();
            tab.applyTab();
        }
    }

    /**
     *  Adds a feature to the Tab attribute of the Tabber object
     *
     *@param  tab  The feature to be added to the Tab attribute
     */
    public void addTab(Tab tab) {
        addTab(tab.getTabTitle(), tab.getTabIcon(),  new TabPanel(tab), tab.getTabToolTipText());
    }

    class TabPanel extends JPanel {
        private Tab tab;

        /**
         *  Constructor for the TabPanel object
         *
         *@param  tab  Description of the Parameter
         */
        TabPanel(Tab tab) {
            super(new BorderLayout());
            this.tab = tab;
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(tab.getTabComponent(), BorderLayout.CENTER);
        }

        public Tab getTab() {
            return tab;
        }
    }
}
