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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.ServerConfiguration;
import com.sshtools.j2ssh.platform.NativeAuthenticationProvider;
import com.sshtools.j2ssh.transport.ServiceOperationException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.InvalidSshKeyException;
import com.sshtools.j2ssh.transport.publickey.InvalidSshKeySignatureException;
import com.sshtools.j2ssh.transport.publickey.SECSHPublicKeyFormat;
import com.sshtools.j2ssh.transport.publickey.SshPublicKeyFormatFactory;
import com.sshtools.j2ssh.transport.publickey.SshKeyPair;
import com.sshtools.j2ssh.transport.publickey.SshKeyPairFactory;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.j2ssh.transport.publickey.SshPublicKeyFile;
import com.sshtools.j2ssh.transport.publickey.SshtoolsPrivateKeyFormat;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;

import com.sshtools.j2ssh.configuration.AuthorizedKeys;

import com.sshtools.j2ssh.SshException;

/**
 *  This class implements the SSH Public Key authenticaiton method
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: PublicKeyAuthentication.java,v 1.12 2002/12/12 20:00:26
 *      martianx Exp $
 */
public class PublicKeyAuthenticationServer
         extends SshAuthenticationServer {
    private Logger log = Logger.getLogger(PublicKeyAuthenticationServer.class);

    /**
     *  Creates a new PublicKeyAuthentication object.
     */
    public PublicKeyAuthenticationServer() { }


    /**
     *  Returns the SSH Authentication method name
     *
     *@return    "publickey"
     */
    public String getMethodName() {
        return "publickey";
    }



    /**
     *  Performs server side public key authentication
     *
     *@param  msg                               The authentication request
     *      message
     *@param  nativeSettings                    A Map of native name/value pairs
     *      which may contain required platform specific information
     *@throws  TransportProtocolException       if an error occurs in the
     *      Transport Protocol
     *@throws  ServiceOperationException        if a critical error occurs in
     *      the service operation
     *@throws  AuthenticationProtocolException  if an authentication operation
     *      critically fails
     */
    public int authenticate(AuthenticationProtocolServer authentication,
                            SshMsgUserAuthRequest msg, Map nativeSettings)
             throws IOException {

            ByteArrayReader bar = new ByteArrayReader(msg.getRequestData());

            // If check == 0 then authenticate, otherwise just inform that
            // the authentication can continue with the key supplied
            int check = bar.read();
            String algorithm = bar.readString();
            byte encoded[] = bar.readBinaryString();
            byte signature[] = null;

            if (check == 0) {
                // Verify that the public key can be used for authenticaiton
                boolean ok = SshKeyPairFactory.supportsKey(algorithm);

                // Send the reply
                SshMsgUserAuthPKOK reply =
                        new SshMsgUserAuthPKOK(ok, algorithm, encoded);
                authentication.sendMessage(reply);

                return AuthenticationProtocolState.READY;
            } else {
                NativeAuthenticationProvider authProv =
                        NativeAuthenticationProvider.getInstance();

                if (authProv == null) {
                    log.error("Authentication failed because no native authentication provider is available");
                    return AuthenticationProtocolState.FAILED;
                }

                if(!authProv.logonUser(getUsername(), nativeSettings)) {
                    log.info("Authentication failed because "
                            + getUsername() + " is not a valid username");
                    return AuthenticationProtocolState.FAILED;
                }

                String userHome = authProv.getHomeDirectory(getUsername(), nativeSettings);

                if (userHome == null) {
                    log.error("Authentication failed because no home directory for "
                            + getUsername() + " is available");
                    return AuthenticationProtocolState.FAILED;
                } else {

                    // Replace '\' with '/' because when we use it in String.replaceAll
                    // for some reason it removes them?
                    userHome = userHome.replace('\\', '/');

                    ServerConfiguration config =
                            ConfigurationLoader.getServerConfiguration();
                    String authorizationFile;
                    String userConfigDir = config.getUserConfigDirectory();

                    // First replace any '\' with '/' (Becasue replaceAll removes them!)
                    userConfigDir = userConfigDir.replace('\\', '/');

                    // Replace any home directory tokens
                    userConfigDir = userConfigDir.replaceAll("%D", userHome);

                    // Replace any username tokens
                    userConfigDir =
                            userConfigDir.replaceAll("%U", getUsername());

                    // Replace the '/' with File.seperator and trim
                    userConfigDir =
                            userConfigDir.replace('/', File.separatorChar).trim();

                    if (!userConfigDir.endsWith(File.separator)) {
                        userConfigDir += File.separator;
                    }

                    authorizationFile =
                            userConfigDir + config.getAuthorizationFile();

                    // Load the authorization file
                    File file = new File(authorizationFile);

                    if (!file.exists()) {
        		log.info("authorizationFile: " + authorizationFile + " does not exist.");
                        log.info("Authentication failed because no authorization file is available");
                        return AuthenticationProtocolState.FAILED;
                    }

                    FileInputStream in = new FileInputStream(file);
                    AuthorizedKeys keys;
                    try {
                      keys = new AuthorizedKeys(in);
                    } catch(Exception e) {
                      throw new AuthenticationProtocolException("Failed to load authorized keys file " + authorizationFile);
                    }

                    Iterator it = keys.getAuthorizedKeys().iterator();

                    SshKeyPair pair = SshKeyPairFactory.newInstance(algorithm);
                    SshPublicKey authorizedKey = null;
                    SshPublicKey key = pair.decodePublicKey(encoded);
                    boolean valid = false;
                    String keyfile;

                    while (it.hasNext()) {
                        keyfile = (String) it.next();

                        // Look for the file in the user config dir first
                        file = new File(userConfigDir + keyfile);

                        // If it does not exist then look absolute
                        if (!file.exists()) {
                            file = new File(keyfile);
                        }

                        if (file.exists()) {

                          // Try to open the public key in the default file format
                          // otherwise attempt the supported key formats
                          SshPublicKeyFile pkf =
                                    SshPublicKeyFile.parse(file);
                            authorizedKey = pkf.toPublicKey();

                           if (authorizedKey.getFingerprint().equals(key.getFingerprint())) {
                                /**
                                 * Now determine ownership of the private key
                                 */
                                signature = bar.readBinaryString();

                                ByteArrayWriter data = new ByteArrayWriter();
                                data.writeBinaryString(authentication.getSessionIdentifier());
                                data.write(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST);
                                data.writeString(msg.getUsername());
                                data.writeString(msg.getServiceName());
                                data.writeString(getMethodName());
                                data.write(1);
                                data.writeString(key.getAlgorithmName());
                                data.writeBinaryString(key.getEncoded());

                                if (key.verifySignature(signature, data.toByteArray()))
                                    return AuthenticationProtocolState.COMPLETE;
                           }
                        } else {
                            log.info("Failed attempt to load key file "
                                    + keyfile);
                        }
                    }

                }
            }

            return AuthenticationProtocolState.FAILED;

    }


}
