/*
 * Created on Mar 1, 2003
 *
 */
package com.sshtools.j2ssh.session;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.MitmGlue;
import com.sshtools.j2ssh.MitmServer;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelFactory;
import com.sshtools.j2ssh.connection.InvalidChannelException;


/**
 * @author David G&uuml;mbel<br>
 * created 01 March 2003<br>
 *
 * SessionChannelFactory for mitm attack program. Largely based on
 * <code>SessionChannelFactory></code>.
 */
public class MitmSessionChannelFactory implements ChannelFactory {
	private static Logger log = Logger.getLogger(MitmSessionChannelFactory.class);
	private MitmGlue mg;
	
	/**
	 * Creates a new <code>MitmSessionChannelFactory</code>
	 * @param mg 
	 */
	public MitmSessionChannelFactory(MitmGlue mg) {
		//super();
		log.debug("mitm: MitmSessionChannelFactory created");
		this.mg = mg;
	}
		
	/**
	 * Gets the list of channel types this factory can create. This method
	 * returns one type 'session'
	 *
	 * @return a list of channel type Strings
	 */
	public List getChannelType() {
		List list = new ArrayList();
		list.add(MitmSessionChannelServer.SESSION_CHANNEL_TYPE);

		return list;
	}

	/**
	 * Creates an uninitialized <code>MitmSessionChannelServer</code> channel
	 * instance.
	 *
	 * @param channelType the channel type to create
	 * @param requestData the channel request data
	 *
	 * @return an uninitialized channel ready for opening
	 *
	 * @throws InvalidChannelException if the channel cannot be created
	 */
	public Channel createChannel(String channelType, byte requestData[])
						  throws InvalidChannelException {
		MitmGlue mg2 =null;
		MitmSessionChannelServer m = null;
		log.debug("mitm: MitmSessionChannelFactory.createChannel("+channelType+") called");
		if (channelType.equals("session")) {
			/* the MitmGlue object needs to know about the SessionChannelServer object 
			 * for later to be capable of closing it after disconnection fro mthe remote 
			 * target host.
			 */
			try {
				mg2 = (MitmGlue)mg.clone();
				this.mg = mg2;
				m = new MitmSessionChannelServer(mg);
				//mg.setSessionChannelServer(m);
			}
			catch (CloneNotSupportedException e) {
				log.error("mitm: failed to clone() MitmGlue object!");
				throw new InvalidChannelException("mitm: failed to clone() MitmGlue object");
			}
			log.debug("mitm: successfully cloned MitmGlue object");
			
			
			return m;
		} else {
			throw new InvalidChannelException("Only session channels can be opened by this factory");
		}
	}

}
