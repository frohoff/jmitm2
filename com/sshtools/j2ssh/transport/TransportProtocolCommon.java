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

import java.io.IOException;

import java.math.BigInteger;

import java.net.Socket;

import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.transport.kex.KeyExchangeException;
import com.sshtools.j2ssh.transport.kex.SshKeyExchange;
import com.sshtools.j2ssh.transport.kex.SshKeyExchangeFactory;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.util.Hash;
import com.sshtools.j2ssh.util.InvalidStateException;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.SshThread;
import com.sshtools.j2ssh.configuration.PlatformConfiguration;

/**
 *  <p>
 *
 *  The main transport protocol implementation. This abstract class provides the
 *  common functionality of both client and server implementations. </p>
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: TransportProtocolCommon.java,v 1.24 2002/12/12 20:03:33
 *      martianx Exp $
 */
public abstract class TransportProtocolCommon
         implements TransportProtocol, Runnable {
    // Flag to keep on running
    //private boolean keepRunning = true;

    /**
     *  The log4j log object
     */
    protected static Logger log =
            Logger.getLogger(TransportProtocolCommon.class);

    /**
     * So we can trackthe threads
     */
    private static int nextThreadNo = 1;
    private int threadNo = nextThreadNo++;
    public int getConnectionId() { return threadNo; }

    /**
     *  End of line setting for CR+LF
     */
    public final static int EOL_CRLF = 1;

    /**
     *  End of Line setting for LF
     */
    public final static int EOL_LF = 2;

    /**
     *  The protocol version supported
     */
    public static final String PROTOCOL_VERSION = "2.0";

    /**
     *  The software version comments that are sent during protocol negotiation
     */
    public static final String SOFTWARE_VERSION_COMMENTS =
            "http://www.sshtools.com "
            + ConfigurationLoader.getVersionString("J2SSH", "j2ssh.properties");

    /**
     *  The secret value k produced during key exchange
     */
    protected BigInteger k = null;

    /**
     *  Indicates when either the remote or local side has completed key
     *  exchange
     */
    protected Boolean completeOnNewKeys = new Boolean(false);

    /**
     *  The host verification instance for verifying host's and host keys
     */
    protected HostKeyVerification hosts;

    /**
     *  The key exchange engine
     */
    protected Map kexs = new HashMap();

    /**
     *  Map of transport message id's to implementation class
     */
    //protected Map transportMessages = new HashMap();

    /**
     *  The connection properties for the current connection
     */
    protected SshConnectionProperties properties;

    /**
     *  The transport layer's message store
     */
    protected SshMessageStore messageStore = new SshMessageStore();

    /**
     *  The key exchange init message sent by the client
     */
    protected SshMsgKexInit clientKexInit = null;

    /**
     *  The key exchange init message sent by the server
     */
    protected SshMsgKexInit serverKexInit = null;

    /**
     *  The identification string sent by the client
     */
    protected String clientIdent = null;

    /**
     *  The identification string sent by the server
     */
    protected String serverIdent = null;

    /**
     *  Sync object containing the cipher, mac and compression objects for the
     *  transport protocol input stream
     */
    protected TransportProtocolAlgorithmSync algorithmsIn;

    /**
     *  Snyc object containing the cipher, mac and compression objects for the
     *  transport protocol output stream
     */
    protected TransportProtocolAlgorithmSync algorithmsOut;

    /**
     *  The transport protocols state instance
     */
    protected TransportProtocolState state = new TransportProtocolState();

    /**
     *  The exchange hash output from key exchange
     */
    private byte exchangeHash[] = null;

    /**
     *  The session identifier (the first exchange hash produced)
     */

    protected byte sessionIdentifier[] = null;

    /**
     *  The servers host key data
     */
    protected byte hostKey[] = null;

    /**
     *  The servers signature supplied to verify the host key
     */
    protected byte signature[] = null;

    // Storage of messages whilst in key exchange
    private List messageStack = new ArrayList();

    // Message notification registry
    private Map messageNotifications = new HashMap();

    // Key exchange lock for accessing the kex init messages
    private Object kexLock = new Object();

    // Object to synchronize key changing
    private Object keyLock = new Object();

    // The connected socket
    private Socket socket;

    // The thread object
    private SshThread thread;

    private long kexTimeout = 3600000L;
    private long kexTransferLimit = 1073741824L;
    private long startTime = System.currentTimeMillis();

    // The input stream for recieving data
    /**
     *  Description of the Field
     */
    protected TransportProtocolInputStream sshIn;

    // The output stream for sending data
    /**
     *  Description of the Field
     */
    protected TransportProtocolOutputStream sshOut;
    private int remoteEOL = EOL_CRLF;
    //private Map registeredMessages = new HashMap();
    private Vector messageStores = new Vector();

    /**
     *  Constructor for the SshTransportProtocol object
     */
    public TransportProtocolCommon() { }


    /**
     *  Gets the guessed EOL setting for the remote host
     *
     *@return    either EOL_CRLF or EOL_LF
     */
    public int getRemoteEOL() {
        return remoteEOL;
    }


    /**
     *  Gets the state attribute of the TransportProtocolCommon object
     *
     *@return    The transport protocols state
     */
    public TransportProtocolState getState() {
        return state;
    }

    protected abstract void onDisconnect();

    /**
     *  Disconnects the connection by sending a disconnect message with the
     *  BY_APPLICAITON reason.
     *
     *@param  description  The description of the reason
     */
    public void disconnect(String description) {
        log.debug("Disconnect: " + description);

        onDisconnect();
        try {
            // Send the disconnect message automatically
            sendDisconnect(SshMsgDisconnect.BY_APPLICATION, description);
        } catch (Exception e) {
            log.warn("Failed to send disconnect", e);
        }
    }

    public void setKexTimeout(long seconds)
        throws TransportProtocolException {
      if(seconds < 60)
        throw new TransportProtocolException("Keys can only be re-exchanged every minute or more");

      kexTimeout = seconds * 1000;
    }


    public void setKexTransferLimit(long kilobytes)
        throws TransportProtocolException {
      if(kilobytes < 5)
        throw new TransportProtocolException("Keys can only be re-exchanged after every 10k of data, or more");

      kexTransferLimit = kilobytes * 1024;

    }

    /**
     *  Called to request the registration of transport protocol messages
     *
     *@throws  MessageAlreadyRegisteredException  if the message is already
     *      registered
     */
    public abstract void registerTransportMessages()
             throws MessageAlreadyRegisteredException;


    /*public byte[] getExchangeHash() {
      return (byte[])exchangeHash.clone();
    }*/


  public byte[] getSessionIdentifier() {
    return (byte[])sessionIdentifier.clone();
  }

    /**
     *  Main processing method for the TransportProtocolCommon object
     */
    public void run() {
        try {
            state.setValue(TransportProtocolState.NEGOTIATING_PROTOCOL);

            log.info("Registering transport protocol messages with inputstream");

            algorithmsOut = new TransportProtocolAlgorithmSync();
            algorithmsIn = new TransportProtocolAlgorithmSync();

            // Create the input/output streams
            sshIn =
                    new TransportProtocolInputStream(socket, algorithmsIn);
            sshOut =
                    new TransportProtocolOutputStream(socket, this, algorithmsOut);

            // Register the transport layer messages that this class will handle
            messageStore.registerMessage(SshMsgDisconnect.SSH_MSG_DISCONNECT,
                    SshMsgDisconnect.class);

            messageStore.registerMessage(SshMsgIgnore.SSH_MSG_IGNORE,
                    SshMsgIgnore.class);

            messageStore.registerMessage(SshMsgUnimplemented.SSH_MSG_UNIMPLEMENTED,
                    SshMsgUnimplemented.class);

            messageStore.registerMessage(SshMsgDebug.SSH_MSG_DEBUG,
                    SshMsgDebug.class);

            messageStore.registerMessage(SshMsgKexInit.SSH_MSG_KEX_INIT,
                    SshMsgKexInit.class);

            messageStore.registerMessage(SshMsgNewKeys.SSH_MSG_NEWKEYS,
                    SshMsgNewKeys.class);

            registerTransportMessages();

            List list = SshKeyExchangeFactory.getSupportedKeyExchanges();
            Iterator it = list.iterator();

            while (it.hasNext()) {
                String keyExchange = (String) it.next();
                SshKeyExchange kex =
                        SshKeyExchangeFactory.newInstance(keyExchange);
                kex.init(this);
                kexs.put(keyExchange, kex);
            }

            // call abstract to initialise the local ident string
            setLocalIdent();

            // negotiate the protocol version
            negotiateVersion();

            startBinaryPacketProtocol();

          } catch (Exception e) {
            if(state.getValue()!=TransportProtocolState.DISCONNECTED) {
              log.error("The Transport Protocol thread failed", e);
              stop();
          }
        } finally {
          thread = null;
        }

        log.debug("The Transport Protocol has been stopped");
    }


    /**
     *  Send an SSH message, if the state doen't allow it because of key
     *  exchange then the message is stored and sent as soon as the state
     *  changes
     *
     *@param  msg                             The SshMessage to send
     *@param  sender                          the object whom is sending the
     *      message
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    public synchronized void sendMessage(SshMessage msg, Object sender)
             throws IOException {
        // Send a message, if were in key exchange then add it to
        // the list unless of course it is a transport protocol or key
        // exchange message
        log.debug("Sending " + msg.getMessageName());

        int currentState = state.getValue();

        if (sender instanceof SshKeyExchange
                || sender instanceof TransportProtocolCommon
                || (currentState == TransportProtocolState.CONNECTED)) {
            sshOut.sendMessage(msg);

            return;
        }

        if (currentState == TransportProtocolState.PERFORMING_KEYEXCHANGE) {
            log.debug("Adding to message queue whilst in key exchange");

            synchronized (messageStack) {
                // Add this message to the end of the list
                messageStack.add(msg);
            }
        } else {
            throw new TransportProtocolException("The transport protocol is disconnected");
        }
    }


    /**
     *  Starts the transport protocol
     *
     *@param  socket                       the underlying socket for
     *      communication
     *@param  properties                   the properties of the connection
     *@throws  TransportProtocolException  if a protocol error occurs
     */
    public void startTransportProtocol(Socket socket,
            SshConnectionProperties properties)
             throws IOException {
        // Save the connected socket for later use
        this.socket = socket;
        this.properties = properties;

        // Start the transport layer message loop
        log.info("Starting transport protocol");
        thread = new SshThread(this, "Transport protocol", true);
        thread.start();

        PlatformConfiguration platform =
                            ConfigurationLoader.getPlatformConfiguration();

        if (platform != null) {
            log.info("Loading native settings from platform configuration");

            Iterator it = platform.getNativeSettings().entrySet().iterator();
            Map.Entry entry;
            while(it.hasNext()) {

              entry = (Map.Entry)it.next();

              thread.setProperty(entry.getKey().toString(),
                                 entry.getValue().toString());

              log.debug("Native Setting: Name=" + entry.getKey().toString()
                                + " Value=" + entry.getValue().toString());

            }
        }

        /**
         * Wait for either a connected or disconnected state
         */
        while(state.getValue()!=TransportProtocolState.CONNECTED &&
              state.getValue()!=TransportProtocolState.DISCONNECTED) {
          state.waitForStateUpdate();
        }

        if(state.getValue()==TransportProtocolState.DISCONNECTED)
          throw new TransportProtocolException("The connection did not complete");



    }


    /**
     *  Implements the TransportProtocol method to allow external SSH
     *  implementations to unregister a message.
     *
     *@param  messageId                          The message id of the message
     *@param  store                              The message store receiving the
     *      notifications.
     *@exception  MessageNotRegisteredException  if the message is not
     *      registered.
     */
    public void unregisterMessage(Integer messageId, SshMessageStore store)
             throws MessageNotRegisteredException {
        log.debug("Unregistering message Id " + messageId.toString());

        if (!messageNotifications.containsKey(messageId)) {
            throw new MessageNotRegisteredException(messageId);
        }

        SshMessageStore actual =
                (SshMessageStore) messageNotifications.get(messageId);

        if (!store.equals(actual)) {
            throw new MessageNotRegisteredException(messageId, store);
        }

        messageNotifications.remove(messageId);
    }


    /**
     *  Abstract method to determine the correct decryption algorithm to use
     *  This is found by iterating through the clients supported algorithm and
     *  selecting the first supported decryption method that the server also
     *  supports. Client and server implementations should define this method
     *  using the determineAlgorithm method to pass either the CS or SC methods
     *  of the SshMsgKexInit object.
     *
     *@return                                  The decryption algorithm to use
     *      i.e. "3des-cbc"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getDecryptionAlgorithm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abstract method to determine the correct encryption algorithm to use
     *  This is found by iterating through the clients supported algorithm and
     *  selecting the first supported encryption method that the server also
     *  supports. Client and server implementations should define this method
     *  using the determineAlgorithm method to pass either the CS or SC methods
     *  of the SshMsgKexInit object object
     *
     *@return                                  The encryption algorithm to use
     *      i.e. "3des-cbc"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getEncryptionAlgorithm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abtract method for the client/server implmentations to determine the
     *  compression algorithm for the input stream. Client and server
     *  implementations should define this method using the determineAlgorithm
     *  method to pass either the CS or SC methods of the SshMsgKexInit object.
     *
     *@return                                  The compression algorithm to use
     *      i.e. "zlib"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getInputStreamCompAlgortihm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abtract method for the client/server implmentations to determine the
     *  message authentication algorithm for the input stream. Client and server
     *  implementations should define this method using the determineAlgorithm
     *  method to pass either the CS or SC methods of the SshMsgKexInit object.
     *
     *@return                                  The mac algorithm to use i.e.
     *      "hmac-sha1"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getInputStreamMacAlgorithm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abstract method that requires a derived class to set value of the local
     *  identification string. If the class implementing this method is a client
     *  then it should set the clientIdent protected member variable, if the
     *  class is implementing a server it should set the protected member
     *  serverIdent.
     */
    protected abstract void setLocalIdent();


    /**
     *  Abstract method to return the local identification string which is used
     *  in protocol negotiation and in computing the exchange hash.
     *  Implementations should return either the protected member variable
     *  clientIdent or serverIdent
     *
     *@return    The local computers idnetification string, used in protocol
     *      negotiation
     */
    protected abstract String getLocalIdent();


    /**
     *  Abstract method to set the local kex init msg which is used in computing
     *  the exchange hash. Implementations should set the appropriate client or
     *  server member variable
     *
     *@param  msg  The local computers kex init message
     */
    protected abstract void setLocalKexInit(SshMsgKexInit msg);


    /**
     *  Abstract method to get the local kex init msg which is used in computing
     *  the exchange hash. Implementations should return the appropriate client
     *  or server member variable
     *
     *@return    The local computers kex init message
     */
    protected abstract SshMsgKexInit getLocalKexInit();


    /**
     *  Abtract method for the client/server implmentations to determine the
     *  compression algorithm for the output stream. Client and server
     *  implementations should define this method using the determineAlgorithm
     *  method to pass either the CS or SC methods of the SshMsgKexInit object.
     *
     *@return                                  The compression algorithm to use
     *      i.e. "zlib"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getOutputStreamCompAlgorithm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abtract method for the client/server implmentations to determine the
     *  message authentication algorithm for the output stream. Client and
     *  server implementations should define this method using the
     *  determineAlgorithm method to pass either the CS or SC methods of the
     *  SshMsgKexInit object.
     *
     *@return                                  The mac algorithm to use i.e.
     *      "hmac-sha1"
     *@exception  AlgorithmNotAgreedException  if an algorithm cannot be agreed
     */
    protected abstract String getOutputStreamMacAlgorithm()
             throws AlgorithmNotAgreedException;


    /**
     *  Abstract method that requires a derived class to set value of the remote
     *  identification string. If the class implementing this method is a client
     *  then it should set the serverIdent protected member variable, if the
     *  class is implementing a server it should set the protected member
     *  clientIdent.
     *
     *@param  ident  The identifiaction string received from the remote host
     */
    protected abstract void setRemoteIdent(String ident);


    /**
     *  Abstract method to return the remote identification string which is used
     *  in protocol negotiation and in computing the exchange hash.
     *  Implementations should return either the protected member variable
     *  clientIdent or serverIdent
     *
     *@return    The local computers idnetification string, used in protocol
     *      negotiation
     */
    protected abstract String getRemoteIdent();


    /**
     *  Abstract method to set the remote kex init msg which is used in
     *  computing the exchange hash. Implementations should set the appropriate
     *  client or server member variable
     *
     *@param  msg  The remote computers kex init message
     */
    protected abstract void setRemoteKexInit(SshMsgKexInit msg);


    /**
     *  Abstract method to get the remote kex init msg which is used in
     *  computing the exchange hash. Implementations should return the
     *  appropriate client or server member variable
     *
     *@return    The local computers kex init message
     */
    protected abstract SshMsgKexInit getRemoteKexInit();


    /**
     *  Abstract method called when key exchange has begun
     *
     *@param  kex                          the key exchange in progress
     *@throws  TransportProtocolException  if a protocol error occurs
     *@throws  KeyExchangeException        if key exchange fails
     */
    protected abstract void performKeyExchange(SshKeyExchange kex)
             throws IOException,
            KeyExchangeException;


    /**
     *  Determines the correct key exchange algorithm to use
     *
     *@return                               A string containing the algorithm
     *      name i.e. "diffie-hellman-group1.sha1"
     *@throws  AlgorithmNotAgreedException  if no algorithm is agreed between
     *      the two parties
     */
    protected String getKexAlgorithm()
             throws AlgorithmNotAgreedException {
        return determineAlgorithm(clientKexInit.getSupportedKex(),
                serverKexInit.getSupportedKex());
    }


    /**
     *  Sets the transport layer up for performing the key exchange, this is
     *  called when either a SSH_MSG_KEXINIT message is received or sent by
     *  either party
     *
     *@throws  TransportProtocolException  if a protocol error occurs
     *@throws  KeyExchangeException        if key exchange fails
     */
    protected void beginKeyExchange()
             throws IOException,
            KeyExchangeException {
        log.info("Starting key exchange");

        //state.setValue(TransportProtocolState.PERFORMING_KEYEXCHANGE);

        String kexAlgorithm = "";

        // We now have both kex inits, this is where client/server
        // implemtations take over so call abstract methods
        try {
            // Determine the key exchange algorithm
            kexAlgorithm = getKexAlgorithm();

            log.debug("Key exchange algorithm: " + kexAlgorithm);

            // Get an instance of the key exchange algortihm
            SshKeyExchange kex = (SshKeyExchange) kexs.get(kexAlgorithm);

            // Do the key exchange
            performKeyExchange(kex);

            // Record the output

            exchangeHash = kex.getExchangeHash();

            if(sessionIdentifier==null) {
              sessionIdentifier = new byte[exchangeHash.length];
              System.arraycopy(exchangeHash, 0, sessionIdentifier, 0,
                               sessionIdentifier.length);
              thread.setSessionId(sessionIdentifier);
            }

            hostKey = kex.getHostKey();
            signature = kex.getSignature();
            k = kex.getSecret();

            // Send new keys
            sendNewKeys();

            kex.reset();
        } catch (AlgorithmNotAgreedException e) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "No suitable key exchange algorithm was agreed");

            throw new KeyExchangeException("No suitable key exchange algorithm could be agreed.");
        }
    }


    /**
     *  Creates the local key exchange init message
     *
     *@return                              the local kex init message
     *@throws  TransportProtocolException  if a protocol error occurs
     */
    protected SshMsgKexInit createLocalKexInit()
             throws IOException {
        return new SshMsgKexInit(properties);
    }


    /**
     *  This is called when a corrupt Mac has been received on the input stream.
     *  In this instance we will send a disconnect message.
     */
    protected void onCorruptMac() {
        log.fatal("Corrupt Mac on Input");

        // Send a disconnect message
        sendDisconnect(SshMsgDisconnect.MAC_ERROR, "Corrupt Mac on input");
    }



    /**
     *  Called by the framework when a new message is received.
     *
     *@param  msg                             The message recevied
     *@exception  TransportProtocolException  Description of the Exception
     *@exception  ServiceOperationException   Description of the Exception
     */
    protected abstract void onMessageReceived(SshMessage msg)
             throws IOException,
            ServiceOperationException;


    /**
     *  Sends a disconnect message
     *
     *@param  reason       The reason code.
     *@param  description  The readable reason description.
     */
    protected void sendDisconnect(int reason, String description) {
        SshMsgDisconnect msg = new SshMsgDisconnect(reason, description, "");

        try {
            sendMessage(msg, this);
            stop();
        } catch (Exception e) {
            log.warn("Failed to send disconnect", e);
        }
    }


    /**
     *  Sends the key exchange init message
     *
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    protected void sendKeyExchangeInit()
             throws IOException {
        setLocalKexInit(createLocalKexInit());
        sendMessage(getLocalKexInit(), this);
        state.setValue(TransportProtocolState.PERFORMING_KEYEXCHANGE);
    }


    /**
     *  Sends the SSH_MSG_NEWKEYS message to indicate that new keys are now in
     *  operation
     *
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    protected void sendNewKeys()
             throws IOException {
        // Send new keys
        SshMsgNewKeys msg = new SshMsgNewKeys();
        sendMessage(msg, this);

        // Lock the outgoing algorithms so nothing else is sent untill
        // weve updated them with the new keys
        algorithmsOut.lock();

        /**
         * Wait for new keys
         */
         int filter[] = new int[1];
         filter[0] = SshMsgNewKeys.SSH_MSG_NEWKEYS;

         msg = (SshMsgNewKeys)readMessage(filter);

         log.debug("Received " + msg.getMessageName());

         completeKeyExchange();

    }


    /**
     *  Sets up the new keys for the IOStreams
     *
     *@param  encryptCSKey                       the client->server encryption
     *      key
     *@param  encryptCSIV                        the client->server encrytioon
     *      IV
     *@param  encryptSCKey                       the server->client encryption
     *      key
     *@param  encryptSCIV                        the server->client encryption
     *      IV
     *@param  macCSKey                           the client->server message
     *      authentication key
     *@param  macSCKey                           the server->client message
     *      authentication key
     *@throws  AlgorithmNotAgreedException       if an algorithm cannot be
     *      agreed
     *@throws  AlgorithmOperationException       if an algorithm fails
     *@throws  AlgorithmNotSupportedException    if an algorithm agreed is not
     *      supported
     *@throws  AlgorithmInitializationException  if an algorithm fails to
     *      initialize
     */
    protected abstract void setupNewKeys(byte encryptCSKey[],
            byte encryptCSIV[],
            byte encryptSCKey[],
            byte encryptSCIV[], byte macCSKey[],
            byte macSCKey[])
             throws AlgorithmNotAgreedException,
            AlgorithmOperationException,
            AlgorithmNotSupportedException,
            AlgorithmInitializationException;


    /**
     *  Completes key exchange by creating keys from the exchange hash and puts
     *  them into use.
     *
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    protected void completeKeyExchange()
             throws IOException {
        log.info("Completing key exchange");

        try {
            // Reset the state variables
            //completeOnNewKeys = new Boolean(false);

            log.debug("Making keys from key exchange output");

            // Make the keys
            byte encryptionKey[] = makeSshKey('C');
            byte encryptionIV[] = makeSshKey('A');
            byte decryptionKey[] = makeSshKey('D');
            byte decryptionIV[] = makeSshKey('B');
            byte sendMac[] = makeSshKey('E');
            byte receiveMac[] = makeSshKey('F');

            log.debug("Creating algorithm objects");

            setupNewKeys(encryptionKey, encryptionIV, decryptionKey,
                    decryptionIV, sendMac, receiveMac);

            // Reset the key exchange
            clientKexInit = null;
            serverKexInit = null;

            //algorithmsIn.release();
            algorithmsOut.release();

            /*
             *  Update our state, we can send all packets
             *
             */
            state.setValue(TransportProtocolState.CONNECTED);

            // Send any outstanding messages
            synchronized (messageStack) {
                Iterator it = messageStack.iterator();

                log.debug("Sending queued messages");

                while (it.hasNext()) {
                    SshMessage msg = (SshMessage) it.next();

                    sendMessage(msg, this);
                }

                messageStack.clear();
            }
        } catch (AlgorithmNotAgreedException anae) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Algorithm not agreed");
            throw new TransportProtocolException("The connection was disconnected because an algorithm could not be agreed");
        } catch (AlgorithmNotSupportedException anse) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Application error");
            throw new TransportProtocolException("The connection was disconnected because an algorithm class could not be loaded");
        } catch (AlgorithmOperationException aoe) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Algorithm operation error");
            throw new TransportProtocolException("The connection was disconnected because"
                    + " of an algorithm operation error");
        } catch (AlgorithmInitializationException aie) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Algorithm initialization error");
            throw new TransportProtocolException("The connection was disconnected because"
                    + " of an algorithm initialization error");
        }
    }


    /**
     *  Helper method to determine the first algorithm that appears in the
     *  client list that is also supported by the server
     *
     *@param  clientAlgorithms                 The list of client algorithms
     *@param  serverAlgorithms                 The list of server algorithms
     *@return                                  The determined algorithms
     *@exception  AlgorithmNotAgreedException  if the algorithm cannot be agreed
     */
    protected String determineAlgorithm(List clientAlgorithms,
            List serverAlgorithms)
             throws AlgorithmNotAgreedException {
        log.debug("Determine Algorithm");
        log.debug("Client Algorithms: " + clientAlgorithms.toString());
        log.debug("Server Algorithms: " + serverAlgorithms.toString());

        String algorithmClient;
        String algorithmServer;

        Iterator itClient = clientAlgorithms.iterator();

        while (itClient.hasNext()) {
            algorithmClient = (String) itClient.next();

            Iterator itServer = serverAlgorithms.iterator();

            while (itServer.hasNext()) {
                algorithmServer = (String) itServer.next();

                if (algorithmClient.equals(algorithmServer)) {
                    log.debug("Returning " + algorithmClient);

                    return algorithmClient;
                }
            }
        }

        throw new AlgorithmNotAgreedException("Could not agree algorithm");
    }


    /**
     *  Starts the transport protocols binary messaging
     *
     *@throws  TransportProtocolException  if a protocol error occurs
     *@throws  ServiceOperationException   if an operation fails
     */
    protected void startBinaryPacketProtocol()
             throws IOException {

        // Send our Kex Init
        sendKeyExchangeInit();

        SshMessage msg;

        // Perform a transport protocol message loop
        while(state.getValue() != TransportProtocolState.DISCONNECTED) {

          // Process incoming messages returning any transport protocol
          // messages to be handled here
          msg =  processMessages();

          log.debug("Transport Protocol is processing "
                   + msg.getMessageName());

          switch (msg.getMessageId()) {
              case SshMsgKexInit.SSH_MSG_KEX_INIT:
              {
                  onMsgKexInit((SshMsgKexInit) msg);
                  break;
              }

              case SshMsgDisconnect.SSH_MSG_DISCONNECT:
              {
                  onMsgDisconnect((SshMsgDisconnect) msg);
                  break;
              }

              case SshMsgIgnore.SSH_MSG_IGNORE:
              {
                  onMsgIgnore((SshMsgIgnore) msg);
                  break;
              }

              case SshMsgUnimplemented.SSH_MSG_UNIMPLEMENTED:
              {
                  onMsgUnimplemented((SshMsgUnimplemented) msg);
                  break;
              }

              case SshMsgDebug.SSH_MSG_DEBUG:
              {
                  onMsgDebug((SshMsgDebug) msg);
                  break;
              }

              default:
                  onMessageReceived(msg);
            }
        }
    }


    /**
     *  Stops the transport layer
     */
    protected final void stop() {
        state.setValue(TransportProtocolState.DISCONNECTED);

        // Close the input/output streams
        //sshIn.close();
        messageStore.close();
        try {
            socket.close();
        } catch (IOException ioe) {
        }
    }


    /**
     *  Creates an Ssh key from the exchange hash and a literal character
     *
     *@param  chr                             The character used to create the
     *      key
     *@return                                 40 bytes of key data
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    private byte[] makeSshKey(char chr)
             throws IOException {
        try {
            // Create the first 20 bytes of key data
            ByteArrayWriter keydata = new ByteArrayWriter();
            byte data[] = new byte[20];

            Hash hash = new Hash("SHA");

            // Put the dh k value
            hash.putBigInteger(k);

            // Put in the exchange hash
            hash.putBytes(exchangeHash);

            // Put in the character
            hash.putByte((byte) chr);

            // Put the exchange hash in again
            hash.putBytes(sessionIdentifier);

            // Create the fist 20 bytes
            data = hash.doFinal();
            keydata.write(data);

            // Now do the next 20
            hash.reset();

            // Put the dh k value in again
            hash.putBigInteger(k);

            // And the exchange hash
            hash.putBytes(exchangeHash);

            // Finally the first 20 bytes of data we created
            hash.putBytes(data);

            data = hash.doFinal();

            // Put it all together
            keydata.write(data);

            // Return it
            return keydata.toByteArray();
        } catch (NoSuchAlgorithmException nsae) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Application error");
            throw new TransportProtocolException("SHA algorithm not supported");
        } catch (IOException ioe) {
            sendDisconnect(SshMsgDisconnect.KEY_EXCHANGE_FAILED,
                    "Application error");
            throw new TransportProtocolException("Error writing key data");
        }
    }


    /**
     *  When the protocol starts, both sides must send an identification string
     *  that identifies the protocol version supported as well as and additional
     *  software version comments field. The identification strings are saved
     *  for later use in computing the exchange hash
     *
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    private void negotiateVersion()
             throws IOException {
        byte buf[];
        int len;
        String remoteVer = "";

            log.info("Negotiating protocol version");
            log.debug("Local identification: " + getLocalIdent());

            // Get the local ident string by calling the abstract method, this
            // way the implementations set the correct variables for computing the
            // exchange hash
            String data = getLocalIdent() + "\r\n";

            // Send our version string
            socket.getOutputStream().write(data.getBytes());

            // Now wait for a reply and evaluate the ident string
            //buf = new byte[255];
            StringBuffer buffer = new StringBuffer();

            char ch;

            // Look for a string starting with "SSH-"
            while (!remoteVer.startsWith("SSH-")) {
                // Get the next string
                while ((ch = (char) socket.getInputStream().read()) != '\n') {
                    buffer.append(ch);
                }

                // Set trimming off any EOL characters
                remoteVer = buffer.toString();

                // Guess the remote sides EOL by looking at the end of the ident string
                if (remoteVer.endsWith("\r")) {
                    remoteEOL = EOL_CRLF;
                } else {
                    remoteEOL = EOL_LF;
                }

                log.debug("EOL is guessed at "
                        + ((remoteEOL == EOL_CRLF) ? "CR+LF" : "LF"));

                // Remove any \r
                remoteVer = remoteVer.trim();
            }

            // Get the index of the seperators
            int l = remoteVer.indexOf("-");
            int r = remoteVer.indexOf("-", l + 1);

            // Call abstract method so the implementations can set the
            // correct member variable
            setRemoteIdent(remoteVer.trim());

            log.debug("Remote identification: " + getRemoteIdent());

            // Get the version
            String remoteVersion = remoteVer.substring(l + 1, r);

            // Evaluate the version, we only support 2.0
            if (!(remoteVersion.equals("2.0") || (remoteVersion.equals("1.99")))) {
                log.fatal("The remote computer does not support protocol version 2.0");
                throw new TransportProtocolException("The protocol version of the remote computer is not supported!");
            }

            log.info("Protocol negotiation complete");
    }


    /**
     *  Handles a debug message
     *
     *@param  msg  the debug message
     */
    private void onMsgDebug(SshMsgDebug msg) {
        log.debug(msg.getMessage());
    }


    /**
     *  Handles a disconnect message
     *
     *@param  msg                             the disconnect message
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    private void onMsgDisconnect(SshMsgDisconnect msg)
             throws IOException {
        log.info("The remote computer disconnected");
        log.debug(msg.getDescription());
        onDisconnect();
        stop();
    }


    /**
     *  Handles the ignore message
     *
     *@param  msg  the ignore message
     */
    private void onMsgIgnore(SshMsgIgnore msg) {
        log.debug("SSH_MSG_IGNORE with "
                + String.valueOf(msg.getData().length()) + " bytes of data");
    }


    /**
     *  Handles the kex init message
     *
     *@param  msg                             the kex init message
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    private void onMsgKexInit(SshMsgKexInit msg)
             throws IOException {
        log.debug("Received remote key exchange init message");
        log.debug(msg.toString());

        synchronized (kexLock) {
            setRemoteKexInit(msg);

            // As either party can initiate a key exchange then we
            // must check to see if we have sent our own
            if(state.getValue()!=TransportProtocolState.PERFORMING_KEYEXCHANGE)
            //if (getLocalKexInit() == null) {
                sendKeyExchangeInit();
            //}

            beginKeyExchange();
        }
    }


    /**
     *  Handles the new keys message
     *
     *@param  msg                             The message received
     *@exception  TransportProtocolException  if a protocol error occurs
     */
    private void onMsgNewKeys(SshMsgNewKeys msg)
             throws IOException {
        // Determine whether we have completed our own
        log.debug("Received New Keys");
        algorithmsIn.lock();

        synchronized (completeOnNewKeys) {
            if (completeOnNewKeys.booleanValue()) {
                completeKeyExchange();
            } else {
                completeOnNewKeys = new Boolean(true);
            }
        }
    }


    /**
     *  Handles an unimplemented message
     *
     *@param  msg  the unimplemented message
     */
    private void onMsgUnimplemented(SshMsgUnimplemented msg) {
        log.debug("The message with sequence no " + msg.getSequenceNo()
                + " was reported as unimplemented by the remote end.");
    }


    public SshMessage readMessage(int filter[]) throws IOException {

        byte msgdata[];
        SshMessage msg;

        while(state.getValue() != TransportProtocolState.DISCONNECTED) {

          msgdata = sshIn.readMessage();
          Integer messageId = SshMessage.getMessageId(msgdata);

          // First check the filter
          for(int i=0;i<filter.length;i++) {
            if(filter[i]==messageId.intValue()) {
              /**
               * We have a match so create and return
               */
              if(messageStore.isRegisteredMessage(messageId)) {
                return messageStore.createMessage(msgdata);
              } else {
                SshMessageStore ms = getMessageStore(messageId);
                msg = ms.createMessage(msgdata);
                log.debug("Processing " + msg.getMessageName());
                return msg;
              }
            }
          }

          /**
           * If we are here then there is no match in the filter
           * so process this as a basic transport layer message
           * if its not then we have an exception
           */
          if(messageStore.isRegisteredMessage(messageId)) {
            /**
             * Transport layer message
             */
            msg = messageStore.createMessage(msgdata);
            switch(messageId.intValue()) {
              case SshMsgDisconnect.SSH_MSG_DISCONNECT:
                {
                    onMsgDisconnect((SshMsgDisconnect) msg);
                    break;
                }

                case SshMsgIgnore.SSH_MSG_IGNORE:
                {
                    onMsgIgnore((SshMsgIgnore) msg);
                    break;
                }

                case SshMsgUnimplemented.SSH_MSG_UNIMPLEMENTED:
                {
                    onMsgUnimplemented((SshMsgUnimplemented) msg);
                    break;
                }

                case SshMsgDebug.SSH_MSG_DEBUG:
                {
                    onMsgDebug((SshMsgDebug) msg);
                    break;
                }
                default:
                {
                  // Exception not allowed
                  throw new IOException("Unexpected transport protocol message");

                }
              }
            } else
              throw new IOException("Unexpected message received");

          }

        throw new IOException("The transport protocol disconnected");


    }
    /**
     *  Implements the message loop
     *
     *@throws  TransportProtocolException  if a protocol error occurs
     *@throws  ServiceOperationException   if a service operation fails
     */
    protected SshMessage processMessages()
             throws IOException {

        byte msgdata[];
        SshMessage msg;
        SshMessageStore ms;

        while (state.getValue() != TransportProtocolState.DISCONNECTED) {

          /**
           * Check the data transfered and connection time to determine whether
           * to restart key-exchange
           */
           long currentTime = System.currentTimeMillis();

           if((currentTime-startTime) > kexTimeout
              || (sshIn.getNumBytesTransfered()
                  +sshOut.getNumBytesTransfered()) > kexTransferLimit) {
             startTime = currentTime;
             sendKeyExchangeInit();
           }

            msgdata = sshIn.readMessage();

            Integer messageId = SshMessage.getMessageId(msgdata);

            if(!messageStore.isRegisteredMessage(messageId)) {
              try {
                ms = getMessageStore(messageId);
                msg = ms.createMessage(msgdata);
                log.debug("Processing " + msg.getMessageName());
                ms.addMessage(msg);
              } catch(MessageNotRegisteredException mnre) {
                  log.info("Unimplemented message received "
                            + String.valueOf(messageId.intValue()));
                  msg = new SshMsgUnimplemented(sshIn.getSequenceNo());
                  sendMessage(msg, this);
              }
            }
            else
               return messageStore.createMessage(msgdata);

        }

        throw new ServiceOperationException("The transport protocol has disconnected");
    }


    /**
     *  Implements the TransportProtocol interface method to allow external SSH
     *  implementations to receive message notificaitons.
     *
     *@param  messageId                              The messageId of the
     *      registered message
     *@param  implementor                            The class that implements
     *      the message
     *@param  store                                  The message store to
     *      receive notificaiton
     *@exception  MessageAlreadyRegisteredException  if the message cannot be
     *      registered.
     */
    public void addMessageStore(SshMessageStore store)
             throws MessageAlreadyRegisteredException {


        messageStores.add(store);

        /*Object[] id = store.getRegisteredMessageIds();

        // Check transport protocol messages
        for(int i=0;i<id.length;i++) {
          if(messageStore.isRegisteredMessage((Integer)id[i]))
            throw new MessageAlreadyRegisteredException((Integer)id[i]);
        }

        // Check the additional message stores
        for(int i=0;i<id.length;i++) {
          if(registeredMessages.containsKey(id[i]))
            throw new MessageAlreadyRegisteredException((Integer)id[i]);
          else
            registeredMessages.put(id[i], store);
        }*/

    }

    private SshMessageStore getMessageStore(Integer messageId)
        throws MessageNotRegisteredException {
      SshMessageStore ms;
      for(Iterator it = messageStores.iterator();it!=null && it.hasNext();) {
        ms = (SshMessageStore)it.next();
        if(ms.isRegisteredMessage(messageId))
          return ms;
      }

      throw new MessageNotRegisteredException(messageId);
    }


    public void removeMessageStore(SshMessageStore ms) {
      messageStores.remove(ms);
    }

}
