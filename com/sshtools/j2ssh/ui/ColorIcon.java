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
package com.sshtools.j2ssh.ui;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


/**
 * A simple colored implementation of an <code>Icon</code>
 *
 * @author Brett Smith
 */
public class ColorIcon implements Icon {
    //  Private instance variables
    private Dimension size;
    private Color color;
    private Color borderColor;

    /**
     * Construct a new 16x16 <code>ColorIcon</code> black icon
     *
     * @param none
     */
    public ColorIcon() {
        this(null);
    }

    /**
     * Construct a new 16x16 <code>ColorIcon</code> given a color
     *
     * @param color the icon color
     */
    public ColorIcon(Color color) {
        this(color, null);
    }

    /**
     * Construct a new 16x16 <code>ColorIcon</code> given a color and border
     * color
     *
     * @param color the icon color
     * @param borderColor the border color
     */
    public ColorIcon(Color color, Color borderColor) {
        this(color, null, borderColor);
    }

    /**
     * Construct a new <code>ColorIcon</code> given a size, color and border
     * color
     *
     * @param color the icon color
     * @param size the icon size
     * @param borderColor the border color
     */
    public ColorIcon(Color color, Dimension size, Color borderColor) {
        setColor(color);
        setSize(size);
        setBorderColor(borderColor);
    }

    /**
     * Draw the icon at the specified location.  Icon implementations
     * may use the Component argument to get properties useful for
     * painting, e.g. the foreground or background color.
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor((color == null) ? Color.black : color);
        g.fillRect(x, y, getIconWidth(), getIconHeight());

        if(borderColor != null) {
            g.setColor(borderColor);
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        }
    }

    /**
     * Set the icon size. <code>null</code> means 16x16
     *
     * @param size the icon size
     */
    public void setSize(Dimension size) {
        this.size = size;
    }

    /**
     * Set the icon color. <code>null</code> means black
     *
     * @param color icon color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Set the border color. <code>null</code> means no border
     *
     * @param color border color
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * Returns the icon's width.
     *
     * @return an int specifying the fixed width of the icon.
     */
    public int getIconWidth() {
        return (size == null) ? 16 : size.width;
    }

    /**
     * Returns the icon's height.
     *
     * @return an int specifying the fixed height of the icon.
     */
    public int getIconHeight() {
        return (size == null) ? 16 : size.height;
    }
}
