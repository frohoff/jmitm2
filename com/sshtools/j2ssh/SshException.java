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
package com.sshtools.j2ssh;

import java.io.IOException;
/**
 * Base exception for all SSH API exceptions
 *
 * @author <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 * @version $Id: SshException.java,v 1.6 2003/01/02 00:43:47 martianx Exp $
 */
public class SshException
    extends IOException {
    /**
     * Constructor for the SshProtocolException object
     *
     * @param msg the error message
     */
    public SshException(String msg) {
        super(msg);
    }
}
