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

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.util.*;
import java.net.URL;
import com.sshtools.j2ssh.ui.ResourceIcon;
import com.sshtools.j2ssh.ui.UIUtil;

import org.apache.log4j.*;

/**
 *  <p>Provides a backing store for preferences using a simple properties
 *  file. The <code>init</code> method must be called first providing the
 *  file to store the preferences in.</p>
 *
 *@author     Brett Smith (<A HREF="mailto:t_magicthize@users.sourceforge.net">
 *                          t_magicthize@users.sourceforge.net</A> )
 *@created    12 January 2002
 *@version    $Id: PreferencesStore.java,v 1.3 2003/02/23 15:46:31 t_magicthize Exp $
 */

public class PreferencesStore {

    protected static Logger log = Logger.getLogger(PreferencesStore.class);

    /**  Save table column positions and sizes. Note, the table must have its
     *   auto resize mode set to off, i.e.
     *
     *   @param table tabl
     *   @param pref preference name
     */
    public static void saveTableMetrics(JTable table,
                                        String pref) {
        for(int i = 0 ; i < table.getColumnModel().getColumnCount() ; i ++) {
            int w = table.getColumnModel().getColumn(i).getWidth();
            put(pref + ".column." + i + ".width", String.valueOf(w));
            put(pref + ".column." + i + ".position", String.valueOf(
                        table.convertColumnIndexToModel(i)));
        }
    }

    /**  Restore table column positions and sizes. Note, the table must have its
     *   auto resize mode set to off, i.e.
     *
     *   @param table
     *   @param pref preferences
     *   @param defaultWidths default column widths
     */
    public static void restoreTableMetrics(JTable table,
                                           String pref,
                                           int[] defaultWidths) {
    //  Check the table columns may be resized correctly
        if(table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF)
            throw new IllegalArgumentException(
                "Table AutoResizeMode must be JTable.AUTO_RESIZE_OFF");

    //  Restore the table column widths and positions
        for(int i = 0 ; i < table.getColumnModel().getColumnCount() ; i ++) {
            try {
                table.moveColumn(table.convertColumnIndexToView(getInt(pref +
                        ".column." + i + ".position", i)), i);
                table.getColumnModel().getColumn(i).setPreferredWidth(
                    getInt(pref + ".column." + i + ".width",
                    defaultWidths == null ?
                    table.getColumnModel().getColumn(i).getPreferredWidth() :
                    defaultWidths[i]));
            }
            catch(NumberFormatException nfe) {
            }
        }
    }

    /**
     * Return if the preferences store is available. This will be <code>false</code
     * if the file could not be opened or created for some reason.
     *
     * @return store available
     */
    public static boolean isStoreAvailable() {
        return storeAvailable;
    }

    /**
     * Initialise the preferences store. The file provided will be created
     * if it doesn't exist (as will any requires parent directories)
     *
     * @param file file where preferences are to be kept.
     */
    public static void init(File file) {
        PreferencesStore.file = file;

    //  Make sure the preferences directory exists, creating it if it doesn't
        File dir = file.getParentFile();
        if(!dir.exists()) {
            log.info("Creating SSHTerm preferences directory " +
                dir.getAbsolutePath());
            if(!dir.mkdirs())
                log.error("Preferences directory " +
                    dir.getAbsolutePath() + " could not be created. " +
                    "Preferences will not be stored");
        }
        storeAvailable = dir.exists();

    //  If the preferences file exists, then load it
        if(storeAvailable) {
            if(file.exists()) {
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    preferences.load(in);
                    storeAvailable = true;
                }
                catch(IOException ioe) {
                    log.error(ioe);
                }
                finally {
                    if(in != null) {
                        try {
                            in.close();
                        }
                        catch(IOException ioe) {
                        }
                    }
                }
            }
    //  Otherwise create it
            else {
                savePreferences();
            }
        }
    }

    /**
     * Save the preferences, creating the file if required.
     */

    public static void savePreferences() {
        if(file == null)
            log.error("Preferences not saved as PreferencesStore has not been initialise.");
        else {
            OutputStream out = null;
            try {
                out = new FileOutputStream(file);
                preferences.store(out, "SSHTerm preferences");
                log.info("Preferences written to " +
                    file.getAbsolutePath() );
                storeAvailable = true;
            }
            catch(IOException ioe) {
                log.error(ioe);
            }
            finally {
                if(out != null) {
                    try {
                        out.close();
                    }
                    catch(IOException ioe) {
                    }
                }
            }
        }
    }

    /**
     * Return a string preference
     *
     * @param name name of preference
     * @param def default value if no preference is found
     * @return value
     */
    public static String get(String name, String def) {
        return preferences.getProperty(name, def);
    };

    /**
     * Store a string preference
     *
     * @param name name of preference
     * @param val value of preference
     */
    public static void put(String name, String val) {
        preferences.put(name, val);
    }

    /**
     * Return a <code>Rectangle</code> preference
     *
     * @param name name of preference
     * @param def default value if no preference is found
     * @return value
     */
    public static Rectangle getRectangle(String name, Rectangle def) {
        String s = preferences.getProperty(name);
        if(s == null || s.equals(""))
            return def;
        else {
            StringTokenizer st = new StringTokenizer(s, ",");
            Rectangle r = new Rectangle();
            try {
                r.x = Integer.parseInt(st.nextToken());
                r.y = Integer.parseInt(st.nextToken());
                r.width = Integer.parseInt(st.nextToken());
                r.height = Integer.parseInt(st.nextToken());
            }
            catch(NumberFormatException nfe) {
                log.warn("Preference is " + name +
                    " is badly formatted", nfe);
            }
            return r;
        }
    }

    /**
     * Store a rectangle preference
     *
     * @param name name of preference
     * @param val value of preference
     */
    public static void putRectangle(String name, Rectangle val) {
        preferences.put(name, val == null ? "" : (
            val.x + "," + val.y + "," + val.width + "," + val.height ) );
    }

    /**
     * Return a integer preference
     *
     * @param name name of preference
     * @param def default value if no preference is found
     * @return value
     */
    public static int getInt(String name, int def) {
        String s = preferences.getProperty(name);
        if(s != null && !s.equals("")) {
            try {
                return Integer.parseInt(s);
            }
            catch(NumberFormatException nfe) {
                log.warn("Preference is " + name +
                    " is badly formatted", nfe);
            }
        }
        return def;
    }
    /**
     * Return a double preference
     *
     * @param name name of preference
     * @param def default value if no preference is found
     * @return value
     */
    public static double getDouble(String name, double def) {
        String s = preferences.getProperty(name);
        if(s != null && !s.equals("")) {
            try {
                return Double.parseDouble(s);
            }
            catch(NumberFormatException nfe) {
                log.warn("Preference is " + name +
                    " is badly formatted", nfe);
            }
        }
        return def;
    }

    /**
     * Store a integer preference
     *
     * @param name name of preference
     * @param val value of preference
     */
    public static void putInt(String name, int val) {
        preferences.put(name, String.valueOf(val));
    }

    /**
     * Store a double preference
     *
     * @param name name of preference
     * @param val value of preference
     */
    public static void putDouble(String name, double val) {
        preferences.put(name, String.valueOf(val));
    }

    /**
     * Return a boolean preference
     *
     * @param name name of preference
     * @param def default value if no preference is found
     * @return value
     */

    public static boolean getBoolean(String name, boolean def) {
        return get(name, String.valueOf(def)).equals("true");
    }

    /**
     * Store a boolean preference
     *
     * @param name name of preference
     * @param val value of preference
     */

    public static void putBoolean(String name, boolean val) {
        preferences.put(name, String.valueOf(val));
    }

    /**
     * Determine if a preference exists
     *
     * @param name name of preference
     * @return exists
     */
    public static boolean preferenceExists(String name) {
        return preferences.containsKey(name);
    }

    //

    private static File file;
    private static boolean storeAvailable;
    private static Properties preferences;

    //  Intialise the preferences
    static
    {
        preferences = new Properties();
    }
}
