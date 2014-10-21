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
package com.sshtools.j2ssh.transport;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sshtools.j2ssh.SshException;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.PlatformConfiguration;
import com.sshtools.j2ssh.util.InvalidStateException;

import java.io.IOException;

import com.sshtools.j2ssh.SshThread;

/**
 *  The service class provides a mechanisnm for the ssh services provided by the
 *  transport protocol.
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: AsyncService.java,v 1.3 2003/02/23 11:23:29 martianx Exp $
 */
public abstract class AsyncService extends Service
         implements Runnable  {
    private static Logger log = Logger.getLogger(Service.class);


    private Thread thread;

    /**
     *  Constructor for the SshService object
     *
     *@param  serviceName  The service name
     */
    public AsyncService(String serviceName) {
        super(serviceName);
    }

    protected void onStart() throws IOException {

      if(Thread.currentThread() instanceof SshThread) {
        thread = ((SshThread)Thread.currentThread()).cloneThread(this, getServiceName());
      } else {
        thread = new Thread(this,  getServiceName());
      }

      log.info("Starting " + getServiceName() +  " service thread");
      thread.start();


    }
    /**
     *  Provides the asynchronous message loop.
     */
    public final void run() {

        int messageFilter[] = getAsyncMessageFilter();

        state.setValue(ServiceState.SERVICE_STARTED);

        while (state.getValue() == ServiceState.SERVICE_STARTED) {
            try {
              // Get the next message from the message store
              SshMessage msg = messageStore.getMessage(messageFilter);

              if (state.getValue() == ServiceState.SERVICE_STOPPED) {
                  break;
              }

              log.info(getServiceName() + " is processing " + msg.getMessageName());
              onMessageReceived(msg);

            } catch (Exception ioe) {
              if (state.getValue() != ServiceState.SERVICE_STOPPED) {
                log.fatal("Service message loop failed!", ioe);
                stop();
              }
            }
        }

        log.info(getServiceName() + " thread is exiting");
        thread = null;

    }

    /**
     *  Overide to inform the service what message id's to provide asynchronous
     *  notification off.
     *
     *@return    an array of message id numbers
     */
    protected abstract int[] getAsyncMessageFilter();


    /**
     *  Abstract method called when a registered message ahs been received.
     *
     *@param  msg                             The message received
     *@exception  ServiceOperationException   if a critical service operation
     *      fails
     *@exception  TransportProtocolException  if an error occurs in the
     *      transport protocol
     */
    protected abstract void onMessageReceived(SshMessage msg)
             throws IOException;

}
