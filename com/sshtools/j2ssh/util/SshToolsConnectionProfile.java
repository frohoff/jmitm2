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
package com.sshtools.j2ssh.util;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import java.math.BigInteger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import java.awt.Color;

import com.sshtools.j2ssh.transport.cipher.SshCipherFactory;
import com.sshtools.j2ssh.transport.compression.SshCompressionFactory;
import com.sshtools.j2ssh.transport.hmac.SshHmacFactory;
import com.sshtools.j2ssh.transport.kex.SshKeyExchangeFactory;
import com.sshtools.j2ssh.transport.publickey.SshKeyPairFactory;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;

import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClientFactory;

import com.sshtools.j2ssh.forwarding.ForwardingConfiguration;

import com.sshtools.j2ssh.io.IOUtil;

import com.sshtools.j2ssh.ui.UIUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.log4j.Logger;

/**
 * @author unascribed
 * @version 1.0
 */

public class SshToolsConnectionProfile extends SshConnectionProperties {

 private static Logger log = Logger.getLogger(SshToolsConnectionProfile.class);

    private Map applicationProperties = new HashMap();
    private Map authMethods = new HashMap();
    private Map localForwardings = new HashMap();
    private Map remoteForwardings = new HashMap();
    private boolean forwardingOnly = false;

    // SAX Processing variables
    private String currentElement = null;
    private String currentAuthentication = null;
    private Properties currentProperties = null;

    /**
     * Constructor for the SshToolsConnectionProfile object
     */
    public SshToolsConnectionProfile()  {

    }


    /**
     * Gets the list of authentication methods for this connection
     *
     * @return
     */
    public Map getAuthenticationMethods() {
        return authMethods;
    }


    public String getApplicationProperty(String name, String defaultValue) {
      String value = (String)applicationProperties.get(name);
      if(value==null)
        return defaultValue;
      else
        return value;
    }


    public int getApplicationPropertyInt(String name, int defaultValue) {
        try {
            return Integer.parseInt(getApplicationProperty(
                name, String.valueOf(defaultValue)));
        }
        catch(NumberFormatException nfe) {
            return defaultValue;
        }
    }


    public boolean getApplicationPropertyBoolean(String name, boolean defaultValue) {
        try {
            return new Boolean(getApplicationProperty(
                name, String.valueOf(defaultValue))).booleanValue();
        }
        catch(NumberFormatException nfe) {
            return defaultValue;
        }
    }


    public Color getApplicationPropertyColor(String name, Color defaultColor) {
        return PropertyUtil.stringToColor(getApplicationProperty(
                name, PropertyUtil.colorToString(defaultColor)));
    }

    public void setApplicationProperty(String name, String value) {
      applicationProperties.put(name, value);
    }

    public void setApplicationProperty(String name, int value) {
      applicationProperties.put(name, String.valueOf(value));
    }

    public void setApplicationProperty(String name, boolean value) {
      applicationProperties.put(name, String.valueOf(value));
    }

    public void setApplicationProperty(String name, Color value) {
      applicationProperties.put(name, PropertyUtil.colorToString(value));
    }


    /**
     * Adds and authentication method to the properties. When a connection is
     * saved, these methods are attempted without the need for the user to
     * specify them
     *
     * @param method The method name to add
     */
    public void addAuthenticationMethod(SshAuthenticationClient method) {
      if(method!=null) {
        if (!authMethods.containsKey(method.getMethodName())) {
            authMethods.put(method.getMethodName(), method);
        }
      }
    }

    public void addLocalForwarding(ForwardingConfiguration config) {
      if(config!=null)
        localForwardings.put(config.getName(), config);
    }

    public void addRemoteForwarding(ForwardingConfiguration config) {
      if(config!=null)
       remoteForwardings.put(config.getName(), config);
    }

    public void removeLocalForwarding(String name) {
      localForwardings.remove(name);
    }

    public void removeRemoteForwarding(String name) {
      remoteForwardings.remove(name);
    }

    public Map getLocalForwardings() {
      return localForwardings;
    }

    public Map getRemoteForwardings() {
      return remoteForwardings;
    }

    /**
     * Opens a connection file and loads the properties ready for connection
     *
     * @param file The full path to the file
     *
     * @throws InvalidProfileFileException Thrown if the file is not a valid
     *         connection file
     */
    public void open(String file)
              throws InvalidProfileFileException {
        open(new File(file));
    }

    /**
     * Opens a connection file and loads the properties ready for connection
     *
     * @param file the file
     *
     * @throws InvalidProfileFileException Thrown if the file is not a valid
     *         connection file
     */
    public void open(File file)
              throws InvalidProfileFileException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            open(in);
        }
        catch (FileNotFoundException fnfe) {
            throw new InvalidProfileFileException(file + " was not found!");
        }
        finally {
            IOUtil.closeStream(in);
        }
    }

    /**
     * Loads from the stream
     *
     * @param stream the stream
     *
     * @throws InvalidProfileFileException Thrown if the stream does not represent
     *          a valid connection profile
     */
    public void open(InputStream in)
              throws InvalidProfileFileException {
        try {
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = saxFactory.newSAXParser();
            XMLHandler handler = new XMLHandler();
            saxParser.parse(in, handler);
            handler = null;

//            in.close();
        } catch (IOException ioe) {
            throw new InvalidProfileFileException("IO error. " +
                ioe.getMessage());
        } catch(SAXException sax) {
          throw new InvalidProfileFileException("SAX Error: " + sax.getMessage());
        } catch(ParserConfigurationException pce) {
          throw new InvalidProfileFileException("SAX Parser Error: " + pce.getMessage());
        } finally {
        }
    }

    /**
     * Removes an authentication method
     *
     * @param method
     */
    public void removeAuthenticaitonMethod(String method) {
        authMethods.remove(method);
    }

    /**
     * Call this method to save the connection properties to file
     *
     * @param file The file to save to
     *
     * @throws InvalidProfileFileException
     */
    public void save(String file)
              throws InvalidProfileFileException {
        try {
            File f = new File(file);

            FileOutputStream out = new FileOutputStream(f);

            /**
             * TODO: save profile xml
             */
            out.write(toString().getBytes());

            out.close();
        } catch (FileNotFoundException fnfe) {
            throw new InvalidProfileFileException(file + " was not found!");
        } catch (IOException ioe) {
            throw new InvalidProfileFileException("io error on " + file);
        } finally {
        }
    }



    public String toString() {
     String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
     xml+= "<SshToolsConnectionProfile Hostname=\"" + host
              + "\" Port=\"" + String.valueOf(port) + "\" Username=\"" + username + "\">";
     xml+="   <PreferedCipher Client2Server=\"" + prefEncryption
                      + "\" Server2Client=\"" + prefDecryption + "\"/>\n";
     xml+="   <PreferedMac Client2Server=\"" + prefRecvMac
                      + "\" Server2Client=\"" + prefSendMac + "\"/>\n";
     xml+="   <PreferedCompression Client2Server=\"" + prefRecvComp
                      + "\" Server2Client=\"" + prefSendComp + "\"/>\n";
     xml+="   <PreferedPublicKey Name=\"" + prefPK + "\"/>\n";
     xml+="   <PreferedKeyExchange Name=\"" + prefKex + "\"/>\n";

     Iterator it = authMethods.entrySet().iterator();
     Map.Entry entry;
     Properties properties;
     while(it.hasNext()) {
      entry = (Map.Entry)it.next();
      xml+="   <AuthenticationMethod Name=\"" + entry.getKey() + "\">\n";
      SshAuthenticationClient auth = (SshAuthenticationClient)entry.getValue();
      properties = auth.getPersistableProperties();
      Iterator it2 = properties.entrySet().iterator();
      while(it2.hasNext()) {
        entry = (Map.Entry)it2.next();
        xml+= "      <AuthenticationProperty Name=\"" + entry.getKey()
                  + "\" Value=\"" + entry.getValue() + "\"/>\n";
      }
      xml+="   </AuthenticationMethod>\n";
     }

     it = applicationProperties.entrySet().iterator();
     while(it.hasNext()) {
      entry = (Map.Entry)it.next();
      xml+="   <ApplicationProperty Name=\"" + entry.getKey() +
                  "\" Value=\"" + entry.getValue() + "\"/>\n";
     }

     it = localForwardings.values().iterator();
     while(it.hasNext()) {
      ForwardingConfiguration config = (ForwardingConfiguration) it.next();
      xml+="   <LocalPortForwarding Name=\"" + config.getName()
                      + "\" AddressToBind=\"" + config.getAddressToBind()
                      + "\" PortToBind=\"" + String.valueOf(config.getPortToBind())
                      + "\" AddressToConnect=\"" + config.getHostToConnect()
                      + "\" PortToConnect=\"" + String.valueOf(config.getPortToConnect())
                      + "\"/>\n";
     }

     it = remoteForwardings.values().iterator();
     while(it.hasNext()) {
      ForwardingConfiguration config = (ForwardingConfiguration) it.next();
      xml+="   <RemotePortForwarding Name=\"" + config.getName()
                      + "\" AddressToBind=\"" + config.getAddressToBind()
                      + "\" PortToBind=\"" + String.valueOf(config.getPortToBind())
                      + "\" AddressToConnect=\"" + config.getHostToConnect()
                      + "\" PortToConnect=\"" + String.valueOf(config.getPortToConnect())
                      + "\"/>\n";
     }

     xml+= "</SshToolsConnectionProfile>";
     return xml;
    }

    private class XMLHandler extends DefaultHandler {

      public void startElement(String uri, String localName, String qname,
                             Attributes attrs)
                      throws SAXException {
      if (currentElement==null) {
            if (!qname.equals("SshToolsConnectionProfile")) {
                throw new SAXException("Unexpected root element " + qname);
            }
            host = attrs.getValue("Hostname");
            username = attrs.getValue("Username");
            String p = attrs.getValue("Port");
            if(p==null)
              port = 22;
            else
              port = Integer.parseInt(p);
            if(host==null || username==null)
              throw new SAXException("Required attribute for element <SshToolsConnectionProfile> missing");
      } else {
        String c2s;
        String s2c;
        if(currentElement.equals("SshToolsConnectionProfile")) {
          if(qname.equals("PreferedCipher")) {
            c2s = attrs.getValue("Client2Server");
            s2c = attrs.getValue("Server2Client");
            if(c2s==null||s2c==null)
              throw new SAXException("Required attribute missing for <PreferedCipher> element");
            prefEncryption = c2s;
            prefDecryption = s2c;
          } else if(qname.equals("PreferedCompression")) {
            c2s = attrs.getValue("Client2Server");
            s2c = attrs.getValue("Server2Client");
            if(c2s==null||s2c==null)
              throw new SAXException("Required attribute missing for <PreferedCompression> element");
            prefRecvComp = c2s;
            prefSendComp = s2c;
          } else if(qname.equals("PreferedMac")) {
            c2s = attrs.getValue("Client2Server");
            s2c = attrs.getValue("Server2Client");
            if(c2s==null||s2c==null)
              throw new SAXException("Required attribute missing for <PreferedMac> element");
            prefRecvMac = c2s;
            prefSendMac = s2c;
          } else if(qname.equals("PreferedPublicKey")) {
            String name = attrs.getValue("Name");
            if(name==null)
              throw new SAXException("Required attribute missing for <PreferedPublickey> element");
            prefPK = name;
          } else if(qname.equals("PreferedKeyExchange")) {
            String name = attrs.getValue("Name");
            if(name==null)
              throw new SAXException("Required attribute missing for <PreferedKeyExchange> element");
            prefPK = name;
          } else if(qname.equals("ApplicationProperty")) {
            String name = attrs.getValue("Name");
            String value = attrs.getValue("Value");
            if(name==null || value==null)
              throw new SAXException("Required attributes missing for <ApplicationProperty> element");
            applicationProperties.put(name, value);
          } else if(qname.equals("AuthenticationMethod")) {
            currentAuthentication = attrs.getValue("Name");
            currentProperties = new Properties();
            if(currentAuthentication==null)
              throw new SAXException("Required attribute missing for <AuthenticationMethod> element");

          } else if(qname.equals("LocalPortForwarding")
                  || qname.equals("RemotePortForwarding")) {
            String name = attrs.getValue("Name");
            String addressToBind = attrs.getValue("AddressToBind");
            String portToBind = attrs.getValue("PortToBind");
            String addressToConnect = attrs.getValue("AddressToConnect");
            String portToConnect = attrs.getValue("PortToConnect");

            if(name==null || addressToBind==null || portToBind==null
                || addressToConnect==null || portToConnect==null)
                  throw new SAXException("Required attribute missing for <" + qname + "> element");

            ForwardingConfiguration config =
                          new ForwardingConfiguration(name,
                                                      addressToBind,
                                                      Integer.parseInt(portToBind),
                                                      addressToConnect,
                                                      Integer.parseInt(portToConnect));

            if(qname.equals("LocalPortForwarding"))
              localForwardings.put(name, config);
            else
              remoteForwardings.put(name, config);


          } else
            throw new SAXException("Unexpected element <" + qname + "> after SshToolsConnectionProfile");
        } else if(currentElement.equals("AuthenticationMethod")) {
            if(qname.equals("AuthenticationProperty")) {
              String name = attrs.getValue("Name");
              String value = attrs.getValue("Value");
              if(name==null||value==null)
                throw new SAXException("Required attribute missing for <AuthenticationProperty> element");
              currentProperties.setProperty(name, value);
            } else
              throw new SAXException("Unexpected element <" + qname + "> found after AuthenticationMethod");

        }

      }

      currentElement = qname;
    }

    public void endElement(String uri, String localName, String qname)
                    throws SAXException {
         if (currentElement!=null) {
            if (!currentElement.equals(qname)) {
                throw new SAXException("Unexpected end element found " + qname);
            } else if (qname.equals("SshToolsConnectionProfile")) {
                currentElement = null;
            } else if(qname.startsWith("Prefered")) {
              currentElement = "SshToolsConnectionProfile";
            } else if(qname.equals("ApplicationProperty")) {
              currentElement = "SshToolsConnectionProfile";
            } else if(qname.equals("AuthenticationProperty")) {
              currentElement = "AuthenticationMethod";
            } else if(qname.equals("LocalPortForwarding")
                || qname.equals("RemotePortForwarding")) {
                currentElement = "SshToolsConnectionProfile";
            } else if(qname.equals("AuthenticationMethod")) {
              currentElement = "SshToolsConnectionProfile";
              try {
                SshAuthenticationClient auth = SshAuthenticationClientFactory.newInstance(currentAuthentication);
                auth.setPersistableProperties(currentProperties);
                authMethods.put(currentAuthentication, auth);
              } catch(AlgorithmNotSupportedException anse) {
                log.warn("AuthenticationMethod element ignored because '" + currentAuthentication
                      + "' authentication is not supported");
              } finally {
                currentAuthentication = null;
              }
            } else
              throw new SAXException("Unexpected end element <" + qname + "> found");
        }
    }


    }

}