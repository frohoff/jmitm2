package com.sshtools.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.session.SessionChannelClient;

/**
 * @author Lee David Painter  lee@sshtools.com
 * @version $Id: Exec.java,v 1.1 2003/02/06 18:33:56 martianx Exp $
 */

public class Exec {

  private String cmd;

  public Exec() {
  }

  public void setCmd(String cmd) {
    this.cmd = cmd;
  }

  public String getCmd() {
    return cmd;
  }


  public void validate() throws BuildException {

    if(cmd==null)
      throw new BuildException("You must set a command to execute on the remote server!");
  }


}