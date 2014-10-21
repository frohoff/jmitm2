package com.sshtools.j2ssh.util;

/**
 * @author unascribed
 * @version $Id: StartStopState.java,v 1.1 2003/01/10 21:50:18 martianx Exp $
 */

public class StartStopState extends State {

  public static final int STARTED = 1;
  public static final int STOPPED = 2;

  public StartStopState(int initial) {
    super(initial);
  }

  public boolean isValidState(int state) {
    return (state==STARTED) || (state==STOPPED);
  }
}