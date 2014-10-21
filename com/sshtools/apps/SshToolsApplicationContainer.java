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



/**
 *  Constainer
 */

public interface SshToolsApplicationContainer  {

    /**
     * Initialise the container
     *
     * @param application application
     * @param panel application panel
     */
    public void init(SshToolsApplication application,
                    SshToolsApplicationPanel panel)
                    throws SshToolsApplicationException ;

    /**
     * Return the application panel in this continer
     *
     * @param application
     */
    public SshToolsApplicationPanel getApplicationPanel();

    /**
     * Invoked when the container is closed
     */
    public void closeContainer();


    /**
     * Set the container visible (called after <code>init()</code)
     *
     * @param visibile visible
     */
    public void setContainerVisible(boolean visible);

    /**
     * Return if the container is visible
     *
     * @return visible
     */
    public boolean isContainerVisible();
}

