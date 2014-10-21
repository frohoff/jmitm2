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
package com.sshtools.j2ssh.io;

import java.io.*;
import java.util.*;


public class IOUtil {

    /**
     * Close an input stream and don't worry about any exceptions, but return
     * true or false instead. If <code>null</code> is supplied as they stream
     * then it is just ignored
     *
     * @param stream stream to close
     * @return closed ok
     */
    public static boolean closeStream(InputStream in) {
        try {
            if(in != null)
                in.close();

            return true;
        } catch(IOException ioe) {
            return false;
        }
    }

    /**
     * Close an output stream and don't worry about any exceptions, but return
     * true or false instead. If <code>null</code> is supplied as they stream
     * then it is just ignored
     *
     * @param stream stream to close
     * @return closed ok
     */
    public static boolean closeStream(OutputStream out) {
        try {
            if(out != null)
                out.close();

            return true;
        } catch(IOException ioe) {
            return false;
        }
    }
}
