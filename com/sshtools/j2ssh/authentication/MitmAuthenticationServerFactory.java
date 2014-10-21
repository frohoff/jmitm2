/*
 * Created on Mar 2, 2003
 *
 */
package com.sshtools.j2ssh.authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
import com.sshtools.j2ssh.MitmServer;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.ExtensionAlgorithm;
import com.sshtools.j2ssh.configuration.SshAPIConfiguration;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;


/**
 * @author David G&uuml;mbel<br>
 * created 02 March 2003<br>
 *
 * <code>AuthenticationServerFactory</code> implementation for use in mitm attack. 
 * Largely based on <code>SshAuthenticationServerFactory</code>.
 */
public class MitmAuthenticationServerFactory
	extends SshAuthenticationServerFactory {
	private MitmGlue mg;
	
	private List availableAuths;
	
	private static Map auths;
			private static Logger log =
				Logger.getLogger(MitmAuthenticationServerFactory.class);

			/** The Password authentication method */
			public final static String AUTH_PASSWORD = "password";

			/** The Public Key authentication method */
			public final static String AUTH_PK = "publickey";


			public final static String AUTH_KBI = "keyboard-interactive";

			static {
				auths = new HashMap();

				log.info("mitm:Loading supported authentication methods");
				/* mitm change: */
				auths.put(AUTH_PASSWORD, MitmAuthenticationServer.class);
				log.info("mitm:Loaded one supported authentication method");
				/*auths.put(AUTH_PK, PublicKeyAuthenticationServer.class);
				auths.put(AUTH_KBI, KBIPasswordAuthenticationServer.class);*/
				
				// Load external methods from configuration file
				SshAPIConfiguration config = ConfigurationLoader.getAPIConfiguration();

				if (config!=null) {

				List addons = config.getAuthenticationExtensions();

					Iterator it = addons.iterator();

					// Add the methods to our supported list
					while (it.hasNext()) {
						ExtensionAlgorithm method = (ExtensionAlgorithm) it.next();

						String name = method.getAlgorithmName();

						if (auths.containsKey(name)) {
							log.debug("Standard authentication implementation for "
									  + name + " is being overidden by "
									  + method.getImplementationClass());
						} else {
							log.debug(name + " authentication is implemented by "
									  + method.getImplementationClass());
						}

						try {
							Class cls = ConfigurationLoader.getExtensionClass(method.getImplementationClass());
							Object obj = cls.newInstance();
							if(obj instanceof SshAuthenticationServer ) {
								log.debug("mitm: putting an authentication method to the acceptable list");
							  auths.put(name, cls);
							}

						} catch (Exception e) {
							log.warn("Failed to load extension authentication implementation "
									  + method.getImplementationClass(), e);
						}
					}
				}
			}
	
	
	/**
	 * Constructor, sets local reference of MitmGlue instance.
	 * @param mg
	 */
	public MitmAuthenticationServerFactory(MitmGlue mg) {
		super();
		this.mg = mg;
	}
	
		
		/**
		 * This implementation does nothing.
		 */
		public static void initialize() {}

		/**
		 * Gets the supported authentication methods
		 *
		 * @return A List of Strings containing the authentication methods
		 */
		public static List getSupportedMethods() {
			// Get the list of ciphers
			ArrayList list = new ArrayList(auths.keySet());
			log.debug("mitm: MitmAuthenticationServerFactory.getSupportedMethods() called...");
			log.debug("mitm: ... list length = " + list.size());
			// Return the list
			return list;
		}

		/**
		 * Creates a new instance of the MitmAuthentication object which implements
		 * methodName.
		 *
		 * @param methodName The method name to instansiate
		 * @param mg	the local reference to the MitmGlue instance
		 *
		 * @return The instance
		 *
		 * @exception AlgorithmNotSupportedException if the method is not supported
		 */
		public static MitmAuthenticationServer newInstance(String methodName, MitmGlue mg)
											 throws AlgorithmNotSupportedException {
			try {
				
				log.debug("mitm: creating new MitmAuthenticationServer");
				MitmAuthenticationServer mas = (MitmAuthenticationServer) ((Class) auths.get(methodName))
				.newInstance();
				mas.setMitmGlue(mg);
				return mas;
			} catch (Exception e) {
				throw new AlgorithmNotSupportedException(methodName
														 + " is not supported!");
			}
		}	
		
	/**
	 * Method overriding superclass' <code>newInstance()</code> method. 
	 * Solely for debugging purposes, <b> should never be called </b>.
	 * @param methodname The method name to instantiate
	 * @deprecated
	 */
	public static SshAuthenticationServer newInstance(String methodname) 
										throws AlgorithmNotSupportedException {
		log.debug("mitm: wrong constructor called");
		throw new AlgorithmNotSupportedException("wrong MitmAuthenticationServer.newInstance() called");
		//return null;
	}
		
	}
	


