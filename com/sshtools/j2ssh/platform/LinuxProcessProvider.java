/*
 * Created on Mar 4, 2003
 *
 */
package com.sshtools.j2ssh.platform;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author David G&uuml;mbel<br>
 * created Mar 4, 2003<br>
 *
 * 
 * 
 */
class LinuxProcessProvider extends NativeProcessProvider {
	private Runtime r = Runtime.getRuntime();
	private Process p = null;

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.platform.NativeProcessProvider#kill()
	 */
	public void kill() {
		p.destroy();
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.platform.NativeProcessProvider#start(java.lang.String, java.util.Map, java.util.Map)
	 */
	public boolean start(String command, Map environment, Map nativeSettings) {
		boolean result = false;
		String[] envp =convertMap(environment);
		try {
			p = r.exec(command, envp);
		}
		catch (Exception e) {
		}
		
		return result;
	}
	
	/**
	 * Converts a <code>Map</code> to a <code>String[]</code> of format "[key]=[value]".
	 * @param m the Map to convert, usually a environment.
	 * @return String[]
	 */
	private String[] convertMap(Map m) {
		String[] envp = null;
		int i = 0;
		Collection coll = m.values();
		Set st = m.keySet();
		Iterator ci = coll.iterator();
		Iterator si = st.iterator();
		while (ci.hasNext() && si.hasNext()) {
			envp[i] = (String)si.next()  + "=" + (String)ci.next(); 
		}
		return envp;
	}

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.SessionDataProvider#getInputStream()
	 */
	public InputStream getInputStream() {
		return p.getInputStream();
		}
	

	/* (non-Javadoc)
	 * @see com.sshtools.j2ssh.session.SessionDataProvider#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		return p.getOutputStream();
	}

}
