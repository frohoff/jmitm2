/*
 * Created on Mar 3, 2003
 *
 */
package com.sshtools.j2ssh;

/**
 * @author David G&uuml;mbel<br>
 * created Mar 3, 2003 <br>
 * This class encapsulates state descriptions of a Mitm attack session.
 * 
 * 
 */
public class MitmState {
	
	/**
	 * freshly initialized
	 */
	public static final int MITM_STATE_UNUSED = 1;
	
	/**
	 * initialized and credentials (username, password) set. 
	 * ready to connect to target host.
	 */
	public static final int MITM_STATE_SETUP = 2;
	
	/**
	 * ssh connection to target host was successful.
	 */ 
	public static final int MITM_STATE_TRANSPORT_CONNECTED = 3;
	
	/**
	 * successfully authenticated at remote host
	 */
	public static final int MITM_STATE_AUTHENTICATED = 4;
	
	/**
	 * <code>InputStream</code> and <code>OutputStream</code> of <code>SessionChannelClient</code> retrieved.
	 */
	public static final int MITM_STATE_STREAMS_SETUP = 5;
	
	/**
	 * connected the streams of client and server, i.e. mitm is successfully
	 * and transparently running.
	 */
	public static final int MITM_STATE_STREAMS_CONNECTED = 6;
	
	/**
	 * disconnected the streams of client and server.
	 */
	public static final int MITM_STATE_STREAMS_DISCONNECTED = -6;
	
	/**
	 * stopped connection to remote host.
	 */
	public static final int MITM_STATE_TRANSPORT_DISCONNECTED = -3;

}
