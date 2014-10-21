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
package com.sshtools.j2ssh.authentication;

import org.apache.log4j.Logger;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import java.io.IOException;

import java.util.Map;
import java.util.Properties;

import javax.swing.SwingUtilities;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.PlatformConfiguration;
import com.sshtools.j2ssh.platform.NativeAuthenticationProvider;
import com.sshtools.j2ssh.transport.ServiceOperationException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.SshMessage;

/**
 *  This class implements Password Authentication over keyboard interactive
 * for the SSH Authenticaiton Protocol.
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: PasswordAuthentication.java,v 1.15 2002/12/09 22:51:23
 *      martianx Exp $
 */
public class KBIPasswordAuthenticationServer
         extends SshAuthenticationServer {
    private static Logger log = Logger.getLogger(KBIPasswordAuthenticationServer.class);

    /**
     *  Returns the SSH User Authentication method name.
     *
     *@return    "password"
     */
    public final String getMethodName() {
        return "keyboard-interactive";
    }





    /**
     *  Called by the framework to set any authenticated tokens that might be
     *  needed by authenticated services. This implementation does nothing
     *
     *@param  tokens
     */
    public void setAuthenticatedTokens(Map tokens) { }


    /**
     *  Called to authenticate a users password; this implementation simply
     *  fails
     *
     *@param  msg
     *@param  nativeSettings               A Map of native settings containing
     *      platform specific information that could be required for
     *      authentication
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     *@throws  ServiceOperationException   if a critical error occurs in the
     *      service operation
     */
    public int authenticate(AuthenticationProtocolServer authentication,
                            SshMsgUserAuthRequest msg, Map nativeSettings)
             throws IOException {
        PlatformConfiguration platform =
                ConfigurationLoader.getPlatformConfiguration();

        if (platform == null) {
            log.info("Cannot authenticate, no platform configuration available");
            return AuthenticationProtocolState.FAILED;
        }

        NativeAuthenticationProvider authImpl =
                NativeAuthenticationProvider.getInstance();

        if (authImpl == null) {
            log.error("Cannot perfrom authentication witout native authentication provider");
            return AuthenticationProtocolState.FAILED;
        }

        authentication.registerMessage(
               SshMsgUserAuthInfoResponse.SSH_MSG_USERAUTH_INFO_RESPONSE,
                                       SshMsgUserAuthInfoResponse.class);
        /**
         *  Send a message to request the password
         */
        SshMsgUserAuthInfoRequest info =
            new SshMsgUserAuthInfoRequest("Password authentication", "", "");

        info.addPrompt(msg.getUsername() + "'s password", false);

        authentication.sendMessage(info);

        SshMessage response = authentication.readMessage();

        if(response instanceof SshMsgUserAuthInfoResponse ) {

          String[] responses = ((SshMsgUserAuthInfoResponse)response).getResponses();

          if(responses.length==1) {

            String password = responses[0];

            if (authImpl.logonUser(getUsername(), password, nativeSettings)) {
              log.info(getUsername() + " has passed password authentication");
              return AuthenticationProtocolState.COMPLETE;
            }
            else {
              log.info(getUsername() + " has failed password authentication");
              return AuthenticationProtocolState.FAILED;
            }
          } else {
           log.error("Client responded with too many values!");
           return AuthenticationProtocolState.FAILED;
          }
        }
        else {
          log.error("Client replied with an invalid message " + response.getMessageName());
          return AuthenticationProtocolState.FAILED;
        }
    }



}
