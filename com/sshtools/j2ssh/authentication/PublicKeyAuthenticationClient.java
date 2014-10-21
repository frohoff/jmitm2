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
public class PublicKeyAuthenticationClient
         extends SshAuthenticationClient {
    private Logger log = Logger.getLogger(PublicKeyAuthenticationClient.class);
    protected SshPrivateKey key;
    private String privateKeyFile = null;
    private String passphrase = null;


    /**
     *  Creates a new PublicKeyAuthentication object.
     */
    public PublicKeyAuthenticationClient() { }


    /**
     *  Sets the SshPrivateKey instance for the authentication
     *
     *@param  key  An initialized SshPrivateKey instance
     */
    public void setKey(SshPrivateKey key) {
        this.key = key;
    }


    /**
     *  Returns the SSH Authentication method name
     *
     *@return    "publickey"
     */
    public String getMethodName() {
        return "publickey";
    }


    /**
     *  Performs client side public key authentication
     *
     *@param  serviceToStart                    The service to start upon
     *      successfull authentication
     *@throws  TransportProtocolException       if an error occurs in the
     *      Transport Protocol
     *@throws  ServiceOperationException        if a critical error occurs in
     *      the service operation
     *@throws  AuthenticationProtocolException  if an authentication error
     *      occurs
     */
    public void authenticate(AuthenticationProtocolClient authentication,
                             String serviceToStart)
             throws IOException,
            TerminatedStateException {
        if ((getUsername() == null) || (key == null)) {
            throw new AuthenticationProtocolException("You must supply a username and a key");
        }


            ByteArrayWriter baw = new ByteArrayWriter();

            log.info("Generating data to sign");

            SshPublicKey pub = key.getPublicKey();

            log.info("Preparing public key authentication request");

            // Now prepare and send the message
            baw.write(1);
            baw.writeString(pub.getAlgorithmName());
            baw.writeBinaryString(pub.getEncoded());

            // Create the signature data
            ByteArrayWriter data = new ByteArrayWriter();
            data.writeBinaryString(authentication.getSessionIdentifier());
            data.write(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST);
            data.writeString(getUsername());
            data.writeString(serviceToStart);
            data.writeString(getMethodName());
            data.write(1);
            data.writeString(pub.getAlgorithmName());
            data.writeBinaryString(pub.getEncoded());

            // Generate the signature
            baw.writeBinaryString(key.generateSignature(data.toByteArray()));

            SshMsgUserAuthRequest msg =
                    new SshMsgUserAuthRequest(getUsername(), serviceToStart,
                    getMethodName(), baw.toByteArray());

            authentication.sendMessage(msg);

    }



    /**
     *  Displays a file chooser for the user to select a private key followed by
     *  a passphrase chooser if the selected private key is protected
     *
     *@param  parent  The parent component
     *@return         <tt>true</tt> if the authenication is ready to be
     *      performed otherwise <tt>false</tt>
     */
    public boolean showAuthenticationDialog(Component parent) {

        SshPrivateKeyFile pkf = null;

        if(privateKeyFile==null) {
          JFileChooser chooser = new JFileChooser();
          chooser.setFileHidingEnabled(false);
          chooser.setDialogTitle("Select Private Key File For Authentication");

          if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
              privateKeyFile = chooser.getSelectedFile().getAbsolutePath();
          }
          else
            return false;
        }
        FileInputStream in = null;

        try {
            pkf = SshPrivateKeyFile.parse(new File(privateKeyFile));
        } catch (InvalidSshKeyException iske) {
            JOptionPane.showMessageDialog(parent, iske.getMessage());
            return false;
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(parent, ioe.getMessage());
        }


        // Now see if its passphrase protected
        if (pkf.isPassphraseProtected()) {

            if(passphrase==null) {
              // Show the passphrase dialog
              Window w =
                      (Window) SwingUtilities.getAncestorOfClass(Window.class,
                      parent);
              PassphraseDialog dialog = null;

                if (w instanceof Frame) {
                    dialog = new PassphraseDialog((Frame) w);
                } else if (w instanceof Dialog) {
                    dialog = new PassphraseDialog((Dialog) w);
                } else {
                    dialog = new PassphraseDialog();
                }

                do {
                    dialog.setVisible(true);

                    if (dialog.isCancelled()) {
                        return false;
                    }

                    passphrase = new String(dialog.getPassphrase());

                    try {
                        key = pkf.toPrivateKey(passphrase);

                        break;
                    } catch (InvalidSshKeyException ihke) {
                        dialog.setMessage("Passphrase Invalid! Try again");
                        dialog.setMessageForeground(Color.red);
                    }
                } while (true);
              }
              else {
                    try {
                        key = pkf.toPrivateKey(passphrase);
                    } catch (InvalidSshKeyException ihke) {
                        return false;
                    }
            }
          } else {
              try {
                  key = pkf.toPrivateKey(null);
              } catch (InvalidSshKeyException ihke) {
                  JOptionPane.showMessageDialog(parent, ihke.getMessage());

                  return false;
              }
          }

          return true;
    }

     public Properties getPersistableProperties() {
      Properties properties = new Properties();
      if(getUsername()!=null)
        properties.setProperty("Username", getUsername());
      if(privateKeyFile!=null)
        properties.setProperty("PrivateKey", privateKeyFile);

      return properties;
    }

    public void setPersistableProperties(Properties properties) {
      setUsername(properties.getProperty("Username"));
      if(properties.getProperty("PrivateKey")!=null)
        privateKeyFile = properties.getProperty("PrivateKey");
      if(properties.getProperty("Passphrase")!=null)
        passphrase = properties.getProperty("Passphrase");

    }

    public boolean canAuthenticate() {
      return (getUsername()!=null && key!=null);
    }
}
