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

/**
 *  The service class provides a mechanisnm for the ssh services provided by the
 *  transport protocol.
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: Service.java,v 1.21 2003/02/22 13:35:53 martianx Exp $
 */
public abstract class Service {
    private static Logger log = Logger.getLogger(Service.class);

    /**
     *  the service is being requested
     */
    public final static int REQUESTING_SERVICE = 1;

    /**
     *  the service is being accepted
     */
    public final static int ACCEPTING_SERVICE = 2;

    /**
     *  native platform settings as defined either in the platform configuration
     *  file or through the native authentication provider
     */
    protected Map nativeSettings;

    /**
     *  The service's message store
     */
    protected SshMessageStore messageStore = new SshMessageStore();

    /**
     *  The transport layer to channel service messages through
     */
    protected TransportProtocol transport;
    protected Integer startMode = null;
    protected ServiceState state = new ServiceState();

    /**
     *  The name of this service
     */
    private String serviceName;

    /**
     *  Constructor for the SshService object
     *
     *@param  serviceName  The service name
     */
    public Service(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     *  Returns the name of the service.
     *
     *@return    The serviceName value
     */
    public final String getServiceName() {
        return serviceName;
    }



    /**
     *  This method is called by the framework to initialize the service after
     *  it has been created. Subclasses should implement the abstract method
     *  onServiceInit() which is called from this method. Depending upon the
     *  state of the service passed to the init method one of the abstract
     *  methods onServiceAccept() or onServiceRequest() is then called.
     *
     *@exception  ServiceOperationException  if a critical service operation
     *      fails
     *@throws  TransportProtocolException    if a transport protocol error
     *      occurs
     */
    public final void start()
             throws IOException {

        if (startMode == null) {
            throw new ServiceOperationException("Service must be initialized first!");
        }

        // Only load native settings if they have not already been set
        if (nativeSettings == null) {
            nativeSettings = new HashMap();

            PlatformConfiguration platform =
                    ConfigurationLoader.getPlatformConfiguration();

            if (platform != null) {
                log.info("Loading native settings from platform configuration");
                nativeSettings.putAll(platform.getNativeSettings());

                Iterator it = nativeSettings.entrySet().iterator();
                Map.Entry entry;
                while(it.hasNext()) {
                  entry = (Map.Entry)it.next();
                  log.debug("Native Setting: Name=" + entry.getKey().toString()
                                    + " Value=" + entry.getValue().toString());

                }
            }

        }

        // If were accepted (i.e. client) we will call onServiceAccept()
        if (startMode.intValue() == REQUESTING_SERVICE) {
            log.info(serviceName + " has been accepted");
            onServiceAccept();
        } else {
            // We've recevied a request instead
            log.info(serviceName + " has been requested");
            onServiceRequest();
        }

        onStart();

        state.setValue(ServiceState.SERVICE_STARTED);
    }

    protected abstract void onStart() throws IOException;


    /**
     *  Gets the state instance for this service
     *
     *@return    The services state
     */
    public ServiceState getState() {
        return state;
    }


    /**
     *  Method to initiate the service, processing such as registering messages
     *  should be untaken in here.
     *
     *@param  startMode                   is the service being requested or
     *      accepted?
     *@param  transport                   the connections transport protocol
     *@param  exchangeHash                the exchange hash from server
     *      authentication
     *@param  nativeSettings              native settings for potential platform
     *      specific operations
     *@throws  ServiceOperationException  if a critical service operation fails
     */
    public void init(int startMode, TransportProtocol transport, Map nativeSettings)
             throws IOException {
        if ((startMode != REQUESTING_SERVICE) && (startMode != ACCEPTING_SERVICE)) {
            throw new ServiceOperationException("Invalid start mode!");
        }

        this.transport = transport;
        this.startMode = new Integer(startMode);
        this.nativeSettings = nativeSettings;

        onServiceInit(startMode);

        transport.addMessageStore(messageStore);

    }


    /**
     *  Stops the message loop and causes the thread to exit
     */
    public final void stop() {

          state.setValue(ServiceState.SERVICE_STOPPED);
          messageStore.close();

    }


    /**
     *  <p>
     *
     *  Abstract method called when the service has been accepted by the remote
     *  computer. </p> <p>
     *
     *  NOTE: the message loop does not start until this method has completed.
     *  </p>
     *
     *@exception  ServiceOperationException  if a critical service operation
     *      fails
     */
    protected abstract void onServiceAccept()
             throws IOException;


    /**
     *  Called when the service is intialized
     *
     *@param  startMode                   the mode, either
     *      Service.ACCEPTING_SERVICE or Service REQUESTING_SERVICE
     *@throws  ServiceOperationException  if a critical service operation fails
     */
    protected abstract void onServiceInit(int startMode)
             throws IOException;


    /**
     *  Abstract method called when the service has been requested.
     *
     *@exception  ServiceOperationException   if a critical service operation
     *      fails
     *@exception  TransportProtocolException  Description of the Exception
     */
    protected abstract void onServiceRequest()
             throws IOException;


    /**
     *  Abstract method that is called by the framework whenever an exception
     *  occurs in a child thread. This is to avoid any exception being written
     *  to System.out.
     *
     *@throws  TransportProtocolException  if a transport protocol error occurs
     */
    /**
     *  Sends an SSH_MSG_SERVICE_ACCEPT message for this service instance
     *
     *@throws  TransportProtocolException  if a transport protocol error occurs
     */
    protected void sendServiceAccept()
             throws IOException {
        SshMsgServiceAccept msg = new SshMsgServiceAccept(serviceName);

        transport.sendMessage(msg, this);
    }

}
