/*
 * Sshtools - Java SSH2 API
 *
 * Copyright (C) 2002 Lee David Painter.
 *
 * Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.sshtools.j2ssh.util;


import java.awt.Color;
import java.awt.Font;
import java.util.StringTokenizer;

public class PropertyUtil {

    /**
     * Convert a string to an int
     *
     * @param number number as a string
     * @param defaultValue default value if number cannot be parse
     * @return integer
     */
    public static int stringToInt(String number, int defaultValue) {
        try {
            return number == null ? defaultValue : Integer.parseInt(number);
        }
        catch(NumberFormatException nfe) {
            return defaultValue;
        }
    }

    /**
     * Return a string representation of a color
     *
     * @param color
     * @return string representation
     */
    public static String colorToString(Color color) {
        StringBuffer buf = new StringBuffer();
        buf.append('#');
        buf.append(numberToPaddedHexString(color.getRed(), 2));
        buf.append(numberToPaddedHexString(color.getGreen(), 2));
        buf.append(numberToPaddedHexString(color.getBlue(), 2));

        return buf.toString();
    }

    /**
     * Convert a font to a string. The string will be in format of
     * name,type,points.
     *
     * @param font the font
     * @return the string
     */
    public static String fontToString(Font font) {
        StringBuffer b = new StringBuffer(font.getName());
        b.append(",");
        b.append(font.getStyle());
        b.append(",");
        b.append(font.getSize());

        return b.toString();
    }

    /**
     * Convert a string to a font. The string should be in format of
     * name,type,points. <code>null</code> will be returned if the string
     * is invalid
     *
     * @param fontString the font as a string
     * @return the font
     */
    public static Font stringToFont(String fontString) {
        StringTokenizer st = new StringTokenizer(fontString, ",");

        try {
            return new Font(st.nextToken(), Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()));
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Return a <code>Color</code> object given a string representation of it
     *
     * @param color
     * @return string representation
     * @throws IllegalArgumentException if string in bad format
     */
    public static Color stringToColor(String s) {
        try {
            return new Color(Integer.decode("0x" + s.substring(1, 3)).intValue(),
                Integer.decode("0x" + s.substring(3, 5)).intValue(),
                Integer.decode("0x" + s.substring(5, 7)).intValue());
        } catch(Exception e) {
            throw new IllegalArgumentException(
                "Bad color string format. Should be #rrggbb ");
        }
    }

    /**
     * Convert a number to a zero padded hex string
     *
     * @param int number
     * @return zero padded hex string
     * @throws IllegalArgumentException if number takes up more characters than
     *                                  <code>size</code>
     */
    public static String numberToPaddedHexString(int number, int size) {
        String s = Integer.toHexString(number);

        if(s.length() > size)
            throw new IllegalArgumentException(
                "Number too big for padded hex string");

        StringBuffer buf = new StringBuffer();

        for(int i = 0; i < (size - s.length()); i++)
            buf.append('0');

        buf.append(s);

        return buf.toString();
    }
}
