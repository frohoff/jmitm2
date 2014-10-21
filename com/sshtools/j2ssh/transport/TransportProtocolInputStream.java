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

import java.io.BufferedInputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.net.Socket;

import com.sshtools.j2ssh.transport.cipher.SshCipher;
import com.sshtools.j2ssh.transport.compression.SshCompression;
import com.sshtools.j2ssh.transport.hmac.SshHmac;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.util.OpenClosedState;

/**
 *  Waits on the socket for data, decrypts and performs message authentication
 *  and then routes the message to the transport protocol for processing.
 *
 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
 *@created    20 December 2002
 *@version    $Id: TransportProtocolInputStream.java,v 1.11 2002/12/10 00:07:32
 *      martianx Exp $
 */
class TransportProtocolInputStream {
    private static Logger log =
            Logger.getLogger(TransportProtocolInputStream.class);

    private long bytesTransfered = 0;
    /**
     *  flag to indicate whether the protocol is disconnecting
     */
    private BufferedInputStream in;
    private Object sequenceLock = new Object();
    private Socket socket;
    private TransportProtocolAlgorithmSync algorithms;
    private long sequenceNo = 0;
    private long sequenceWrapLimit = BigInteger.valueOf(2).pow(32).longValue();

    private SshCipher cipher;
    private SshHmac hmac;
    private SshCompression compression;
    int msglen;
    int padlen;
    int read;
    int remaining;
    int cipherlen = 8;
    int maclen = 0;
    byte buffer[] = new byte[128 * cipherlen];
    ByteArrayWriter message = new ByteArrayWriter();
    byte initial[] = new byte[cipherlen];
    byte data[];


    /**
     *  Constructor for the TransportProtocolInputStream object
     *
     *@param  socket           The socket input stream
     *@param  listener         The transport layer for routing messages
     *@param  algorithms       The algorithms in use
     *@exception  IOException  Description of the Exception
     *@throws  IOException     if the InputStream fails to initialize
     */
    public TransportProtocolInputStream(Socket socket,
            TransportProtocolAlgorithmSync algorithms)
             throws IOException {
        this.socket = socket;
        this.in = new BufferedInputStream(socket.getInputStream());
        this.algorithms = algorithms;
    }


    /**
     *  Gets the sequence no of the last message sent
     *
     *@return    The sequenceNo value
     */
    public synchronized long getSequenceNo() {
        return sequenceNo;
    }

    protected long getNumBytesTransfered() {
      return bytesTransfered;
    }

    /**
     *  Main processing method for the SshInputStream object
     */
    public byte[] readMessage() throws IOException {

          // Reset the message for the next
          message.reset();

          // Read the first byte of this message (this is so we block
          // but we will determine the cipher length before reading all
          read = in.read(initial, 0, 1);

          // Make sure we have not closed or reached eof
          if (read < 0)
            throw new IOException("Socket is EOF");

          //algorithms.lock();
          cipher = algorithms.getCipher();
          hmac = algorithms.getHmac();
          compression = algorithms.getCompression();

          // If the cipher object has been set then make sure
          // we have the correct blocksize
          if (cipher != null) {
              cipherlen = cipher.getBlockSize();
          } else {
              cipherlen = 8;
          }

          // Verify we have enough buffer size for the inital block
          if(initial.length != cipherlen) {
          // Create a temporary array for the new block size and copy
              byte tmp[] = new byte[cipherlen];
              System.arraycopy(initial,0,tmp,0,initial.length);
              // Now change the initial buffer to our new array
              initial = tmp;
           }

          // Now read the rest of the first block of data
          int count = read;
          do {
            read = in.read(initial, count, initial.length - count);
            count+=read;
          } while(count < initial.length);

          // Make sure that our buffer size is a multiple of blocksize
          if ((buffer.length % cipherlen) != 0) {
              buffer = new byte[128 * cipherlen];
          }

          // Record the mac length
          if (hmac != null) {
              maclen = hmac.getMacLength();
          } else {
              maclen = 0;
          }

          // Decrypt the data if we have a valid cipher
          if (cipher != null) {
              initial = cipher.transform(initial);
          }

          //log.debug("Cipher is " + (cipher==null? "null": "not null"));
          // Save the initial data
          message.write(initial);

          // Preview the message length
          msglen = ByteArrayReader.readInt(initial, 0);

          //log.debug("Reading message with " + String.valueOf(msglen) + " bytes");

          padlen = initial[4];

          // Read, decrypt and save the remaining data
          remaining = (msglen - (cipherlen - 4));

          //log.debug(String.valueOf(remaining) + " bytes of data left to collect");
          // Loop collecting data untill we have the correct number of
          // bytes
          while (remaining > 0) {
              // Read up to buffer.length or remaining whichever is lower
              if (remaining > buffer.length) {
                  //log.debug("Reading " + String.valueOf(buffer.length) + " bytes from socket");
                  read = in.read(buffer);
              } else {
                  //log.debug("Reading " + String.valueOf(buffer.length) + " bytes from socket");
                  read = in.read(buffer, 0, remaining);
              }

              log.debug("Read " + String.valueOf(read) + " bytes from socket");

              // Check that nothing went wrong on the socket
              if (read > 0) {
                  // Record how many bytes weve received
                  remaining -= read;

                  // Decrypt the data and/or write it to the message
                  if (cipher != null) {
                      message.write(cipher.transform(buffer, 0, read));
                  } else {
                      message.write(buffer, 0, read);
                  }


              } else if(read == 0) {
                log.warn("Read returned zero bytes");
              } else {
                  throw new IOException("Socket InputStream is EOF");
              }
          }

           // End of while
          synchronized (sequenceLock) {
            //log.debug("Total num bytes encrypted so far: " + String.valueOf(bytesEncrypted));
            //log.debug("Message sequence no is " + String.valueOf(sequenceNo));

            if (hmac != null) {
                  count = 0;
                  while(count<maclen) {
                   // if(count!=0)
                   //   log.debug("Only " + String.valueOf(count) + " bytes of MAC data received, expecting " + String.valueOf(maclen-count) + " more");
                    read = in.read(buffer, 0, maclen-count);
                    if(read>0) {
                      message.write(buffer, 0, read);
                      count+=read;
                    } else {
                      throw new IOException("EOF whilst reading MAC data!");
                    }
                  }

                  // Verify the mac
                  if (!hmac.verify(sequenceNo, message.toByteArray())) {
                     throw new IOException("Corrupt Mac on input");
                  }
              }

                // Increment the sequence no
                if (sequenceNo < sequenceWrapLimit) {
                    sequenceNo++;
                } else {
                    sequenceNo = 0;
                }
            }

            bytesTransfered += message.size();

            // End of sequence no lock
            return message.toByteArray();
        }



}