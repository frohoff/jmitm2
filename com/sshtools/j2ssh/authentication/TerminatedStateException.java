package com.sshtools.j2ssh.authentication;

public class TerminatedStateException extends Exception {
  private int state;
  public TerminatedStateException(int state) {
    this.state = state;
  }

  public int getState() { return state; }

}