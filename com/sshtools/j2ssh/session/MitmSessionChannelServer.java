/*
 * Created on Mar 1, 2003
 *
 */
package com.sshtools.j2ssh.session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
import com.sshtools.j2ssh.configuration.AllowedSubsystem;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.PlatformConfiguration;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelOutputStream;
import com.sshtools.j2ssh.connection.ChannelState;
import com.sshtools.j2ssh.connection.ConnectionProtocol;
import com.sshtools.j2ssh.connection.InvalidChannelException;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.platform.NativeProcessProvider;
import com.sshtools.j2ssh.subsystem.SubsystemServer;
import com.sshtools.j2ssh.transport.ServiceOperationException;

/**
 * @author David G&uuml;mbel<br>
 * created 01 March 2003<br>
 * <code>SessionChannelServer</code> implementation for mitm, largely 
 * depending upon <code>SessionChannelServer</code>.<br>
 *
 */
public class MitmSessionChannelServer extends SessionChannelServer {
	private static Logger log = Logger.getLogger(MitmSessionChannelServer.class);
	private static Map allowedSubsystems = new HashMap();
	
	/**
	 *  the channel type
	 */
	private InputStream channelIn;
	private Map environment = new HashMap();
	private NativeProcessProvider processInstance;
	private OutputStream channelOut;
	private ChannelOutputStream stderr;
	private SubsystemServer subsystemInstance;
	private Thread thread;
	private MitmGlue mg;
	
	private ChannelState state = new ChannelState();
	/**
	 *  The channels connection
	 */
	protected ConnectionProtocol connection;

	/**
	 * 
	 */
	public MitmSessionChannelServer(MitmGlue mg) {
		super(); 
		this.mg = mg;
		mg.setSessionChannelServer(this);
		log.debug("mitm: MitmSessionChannelServer created");
	}

	/**
	*  Handles the channel open. 
 	*  Calls super.onChannelOpen() after logging.
 	*@throws  ServiceOperationException  if a critical service operation fails
 	*/
	protected void onChannelOpen()
		 throws InvalidChannelException {
			/*channelIn = mg.scc.getInputStream();
			channelOut = mg.scc.getOutputStream();*/ 
		log.debug("mitm: onChannelOpen() called");
		super.onChannelOpen();
	}
	
	protected void onChannelRequest(String requestType, boolean wantReply, byte requestData[]) 
			throws IOException {
				log.debug("onChannelRequest(\""+requestType+"\") called");
				
				
				if (channelIn == null )  {
					log.debug("mitm: channelIn is null, setting it");
					channelIn = mg.getInputStream();
					} 
				if (channelOut == null) {
					log.debug("mitm: channelOut is null, setting it");
					channelOut = mg.getOutputStream();	
					} 
				/*channelIn = mg.scc.getInputStream();
				channelOut = mg.scc.getOutputStream(); */ 
		super.onChannelRequest(requestType, wantReply, requestData);
	}

	/**
 	*  Handles the channel close.<br>
 	*  <br>
 	*  This method attempts to close down any process or subsystem that the
 	*  channel has started. 
 	*
 	*@throws  ServiceOperationException  if a critical service operation fails
 	*/ 
	protected  void onChannelClose()
		 	throws ServiceOperationException {
			log.debug("mitm: closing channel...");
			try {
				mg.getOutputStream().flush();
				log.debug("mitm: flush()ed mg.out");
				
			}
			catch (Exception e) {
				log.debug("mitm: failed to close channels in onChannelClose()",e);
			}
			if (processInstance != null) {
				processInstance.kill();
			}

			if (subsystemInstance != null) {
				subsystemInstance.stop();
			}
}
	

	/**
	 *  Handles the request for a pseudo terminal.
	 *
	 *@param  term    the terminal answerback mode
	 *@param  cols    the number of columns
	 *@param  rows    the number of rows
	 *@param  width   the width in pixels
	 *@param  height  the height in pixels
	 *@param  modes   encoded terminal modes
	 *@return         <tt>true</tt> if the terminal has been allocated otherwise
	 *      <tt>false</tt>
	 */
	protected boolean onRequestPseudoTerminal(String term, int cols, int rows,
			int width, int height,
			String modes) {
			
			MitmPseudoTerminal pterm = new MitmPseudoTerminal(term,width, height, cols,rows);				
			log.info("mitm: requested a pty, created new PseudoTerminal object");
			boolean result = false;
			try {
		
			result = mg.requestPseudoTerminal(pterm, getInputStream(), getOutputStream());
		
	
			/*channelIn = mg.getInputStream();
			hannelOut = mg.getOutputStream(); */ 
			}
			catch (Exception e)
				{
					log.error("mitm: pty request failed: "+e.getMessage());
					result = false;
				}
			
			if (result) log.debug("mitm: pty request at target host successfull"); 
			else log.debug("mitm: pty request at target host failed"); 
		return result;
	}
	
	/**
	 *  Executes a command using the configured <code>NativeProcessProvider</code>
	 *  .
	 *
	 *@param  command  the command to execute.
	 *@return          <tt>true</tt> if the command has been executed otherwise
	 *      <tt>false</tt>
	 */
	protected boolean onExecuteCommand(String command) {
		boolean result = false;
		
		log.debug("mitm: trying to execute remote command "+command);
		try {
			if (mg.executeCommand(command, getInputStream(), getOutputStream())) log.debug("mitm: success at executing remote command!");
			result = true;
		}
		catch (Exception e) {
			e.printStackTrace();
			result = false;
		}

		return result;
	}
	
	
	

	/**
	 *  Adds an environment variable to the list to be supplied on the execution
	 *  of a command or shell
	 * 
	 *@param  name   the environment variable name
	 *@param  value  the environment variable value
	 */
	protected void onSetEnvironmentVariable(String name, String value) {
		environment.put(name, value);
		mg.setEnvironmentVariable(name,value);
	}

	
	
	/**
	 *  Starts the users shell by executing the command specified in the
	 *  TerminalProvider element in the server configuration file
	 *
	 *@return                             <tt>true</tt> if the shell has been
	 *      started otherwise <tt>false</tt>
	 *@throws  ServiceOperationException  if the service operation critically
	 *      fails
	 */
	protected boolean onStartShell()
			 throws ServiceOperationException {
			 return mg.startShell(getInputStream(), getOutputStream());
	}
	
	/**
	  *  Starts a given subsystem.
	  *  Currently not supported, aways returns <code>false.
	  *@param  subsystem  the subsystem name
	  *@return            <tt>true</tt> if the subsystem has been started
	  *      otherwise <tt>false</tt>
	  */
	 protected boolean onStartSubsystem(String subsystem) {
		 boolean result = false;
		 return false;
	 }

}
