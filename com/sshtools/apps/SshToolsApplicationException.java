/*
 * Sshtools - Applications
 *
 * Copyright (C) 2002 Lee David Painter.
 *
 * Written by: 2002 Brett Smith <t_magicthize@users.sourceforge.net>
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
package com.sshtools.apps;

import java.io.IOException;
import java.lang.reflect.*;
/**
 * Base exception for all Ssh application exceptions
 *
 * @author <A HREF="mailto:t_magicthize@users.sourceforge.net">Brett Smith</A>
 * @version $Id: SshToolsApplicationException.java,v 1.1 2003/01/12 19:17:30 t_magicthize Exp $
 */
public class SshToolsApplicationException
    extends Exception {
    /**
     * Constructor for the SshToolsApplicationException object
     *
     * @param msg the error message
     */
    public SshToolsApplicationException() {
        this(null, null);
    }

    /**
     * Constructor for the SshToolsApplicationException object
     *
     * @param msg the error message
     */
    public SshToolsApplicationException(String msg) {
        this(msg, null);
    }

    /**
     * Constructor for the SshToolsApplicationException object
     *
     * @param cause cause
     */
    public SshToolsApplicationException(Throwable cause) {
        this(null, cause);
    }

    /**
     * Constructor for the SshToolsApplicationException object
     *
     * @param msg the error message
     * @param cause cause
     */
    public SshToolsApplicationException(String msg, Throwable cause) {
        super(msg);
        if(cause != null) {
            try {
                Method m = getClass().getMethod("initCause", new Class[]
                    { Throwable.class });
                m.invoke(this, new Object[] { cause } );
            }
            catch(Exception e) {
            }
        }
    }
}
