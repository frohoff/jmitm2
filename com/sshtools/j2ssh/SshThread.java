package com.sshtools.j2ssh;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import java.util.HashMap;

public class SshThread extends Thread {

  private static HashMap names = new HashMap();
  protected byte[] sessionId;
  private HashMap settings = new HashMap();

  public SshThread(Runnable target, String name, boolean daemon) {
    super(target);
    Integer i;
    if(names.containsKey(name)) {
      i = new Integer(((Integer)names.get(name)).intValue()+1);
    } else {
      i = new Integer(1);
    }
    names.put(name, i);
    setName(name + " " + Integer.toHexString(i.intValue()&0xFF));
    setDaemon(daemon);
    if(ConfigurationLoader.isContextClassLoader())
      setContextClassLoader(ConfigurationLoader.getContextClassLoader());
  }

  public void setSessionId(byte[] sessionId) {
    if(sessionId!=null) {
      this.sessionId = new byte[sessionId.length];
      System.arraycopy(sessionId, 0, this.sessionId, 0, sessionId.length);
    }
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public Thread cloneThread(Runnable target, String name) {
    SshThread thread = new SshThread(target, name, isDaemon());
    thread.setSessionId(sessionId);
    thread.settings.putAll(settings);
    return thread;
  }

  public void setProperty(String name, String value) {
    settings.put(name, value);
  }

  public String getProperty(String name) {
    return (String)settings.get(name);
  }
}