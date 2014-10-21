package com.sshtools.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.sftp.*;
import java.io.*;

public class Sftp {

  private String dest;
  private String get;
  private String put;
  private String mkdir;
  private String rmdir;
  private String delete;

  public Sftp() {

  }

  public void setDest(String dest) {
    this.dest = dest;
  }

  public String getDest() {
    return dest;
  }

  public void setGet(String get) {
    this.get = get;
  }

  public String getGet() {
    return get;
  }

  public void setPut(String put) {
    this.put = put;
  }

  public String getPut() {
    return put;
  }

  public void setMkdir(String mkdir) {
    this.mkdir = mkdir;
  }

  public String getMkdir() {
    return mkdir;
  }

  public void setRmdir(String rmdir) {
    this.rmdir = rmdir;
  }

  public String getRmdir() {
    return rmdir;
  }

  public void setDelete(String delete) {
    this.delete = delete;
  }

  public String getDelete() {
    return delete;
  }

  protected void validate() throws org.apache.tools.ant.BuildException {

    if(get!=null && dest==null)
      throw new BuildException("You must supply a destination for the get operation");

    if(put!=null && dest==null)
      throw new BuildException("You must supply a destination for the put operation");

    if(get!=null && put!=null)
      throw new BuildException("You cannot specify a get and put together, use seperate tasks");

  }
}