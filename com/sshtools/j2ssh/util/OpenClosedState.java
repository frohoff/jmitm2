package com.sshtools.j2ssh.util;

/**
 * @author unascribed
 * @version $Id: OpenClosedState.java,v 1.1 2003/01/10 21:50:18 martianx Exp $
 */

public class OpenClosedState extends State {

  public static final int OPEN = 1;
  public static final int CLOSED = 2;

  public OpenClosedState(int initial) {
    super(initial);
  }

  public boolean isValidState(int state) {
    return (state==OPEN) || (state==CLOSED);
  }
}