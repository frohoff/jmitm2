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

import org.apache.log4j.Logger;

import java.awt.Image;
import java.awt.Toolkit;

import java.io.FilePermission;

import java.security.AccessController;
import java.security.AccessControlException;

import java.net.URL;

import javax.swing.ImageIcon;


/**
 * <p>
 * Loads an image from the resources
 * </p>
 *
 * @author <A HREF="mailto:richard@sshtools.com">Richard Pernavas</A>
 * @version $Id: ResourceIcon.java,v 1.3 2003/02/22 00:49:04 t_magicthize Exp $
 */
public class ResourceIcon
    extends ImageIcon {
    private static Logger log = Logger.getLogger(ResourceIcon.class.getName());

    /**
     * Creates an ImageIcon from a resource image
     *
     * @param imageName The image filename
     */
    public ResourceIcon(String imageName) {
        super();

        Image image = null;
        URL url = this.getClass().getResource(imageName);

        if (url!=null) {
            log.debug(url.toString());
            image = Toolkit.getDefaultToolkit().getImage(url);
        } else {
            try {
                if(System.getSecurityManager() != null)
                    AccessController.checkPermission(new FilePermission(
                            imageName, "read"));
                image = Toolkit.getDefaultToolkit().getImage(imageName);
            } catch(AccessControlException ace) {
                log.error("Icon " + imageName + " could not be located as a " +
                    "resource, and the current security manager will not " +
                    "allow checking for a local file.");
            }
        }
        if(image != null)
            this.setImage(image);
    }
}
