package com.sshtools.j2ssh.platform;

import java.util.Map;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
/**
 * 
 * @author David G&uuml;mbel<br>
 * created Mar 3, 2003<br>
 *
 * Provides an authentication provider that has an <code>MitmGlue</code>
 * object attached. It forwards the authentication credentials (username, password)
 * to the <code>MitmClient</code> in <code>MitmGlue</code>, thus letting authentication
 * succed when it has succeded at the remote target host. 
 */
public class MitmFakeAuthenticationProvider
		extends NativeAuthenticationProvider {
		
		private static Logger log = Logger.getLogger(MitmFakeAuthenticationProvider.class);
		
		/* (non-Javadoc)
		 * @see com.sshtools.j2ssh.platform.NativeAuthenticationProvider#getHomeDirectory(java.lang.String, java.util.Map)
		 */
		public String getHomeDirectory(String username, Map tokens) {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * Overrides superclass' method, solely for debugging purposes. 
		 * <b> Should not be used </b>
		 * @deprecated
		 * @see com.sshtools.j2ssh.platform.NativeAuthenticationProvider#logonUser(java.lang.String, java.lang.String, java.util.Map)
		 */
		public boolean logonUser(
			String username,
			String password,
			Map tokens) {
			
			log.debug("MITM: username/password is "+username+" / "+password);
			log.error("mitm: wrong logonUser(username, password, tokens) called");
			return false;
			
		}
		
		/**
		 * Logs on a user, trying to authenticate with the given credentials at the remote target host.
		 * Returns true if successful at remote target host, false else.
		 * @see com.sshtools.j2ssh.platform.NativeAuthenticationProvider#logonUser(java.lang.String, java.lang.String, java.util.Map)
		 * @param username
		 * @param password
		 * @param tokens
		 * @param mg
		 * @return boolean
		 */
		public boolean logonUser(String username, String password, Map tokens, MitmGlue mg) {
			boolean result = false;
			log.debug("mitm: username/password is "+username+" / "+password);
			log.debug("mitm: setCredentials()");
			/* set credentials for remote target host authentication attempt */
			mg.setCredentials(username, password);
			log.debug("mitm: doAuthentication()");
			/* try to authenticate at remote target host */
			result = mg.doAuthentication();
			return result;
			
		}

		/**
		 * Overrides superclass' method. 
		 * <b> Should not be called </b>
		 * @deprecated
		 * @see com.sshtools.j2ssh.platform.NativeAuthenticationProvider#logonUser(java.lang.String, java.util.Map)
		 */
		public boolean logonUser(String username, Map tokens) {
			// TODO Auto-generated method stub
			log.debug("mitm: wrong logonUser(username, tokens) called");
			return false;
		}

	}
