package com.sshtools.j2ssh.sftp;

import com.sshtools.j2ssh.subsystem.SubsystemMessage;

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.io.UnsignedInteger32;


/**
 * @author unascribed
 * @version 1.0
 */

public class SshFxpRealPath extends SubsystemMessage implements MessageRequestId {

  public static final int SSH_FXP_REALPATH = 16;

  private UnsignedInteger32 id;
  private String path;

  public SshFxpRealPath() {
    super(SSH_FXP_REALPATH);
  }

  public SshFxpRealPath(UnsignedInteger32 id, String path) {
    super(SSH_FXP_REALPATH);
    this.id = id;
    this.path = path;
  }

  public UnsignedInteger32 getId() {
    return id;
  }

  public String getPath() {
    return path;
  }

  public void constructMessage(ByteArrayReader bar) throws java.io.IOException, com.sshtools.j2ssh.transport.InvalidMessageException {
    id = bar.readUINT32();
    path = bar.readString();
  }
  public String getMessageName() {
    return "SSH_FXP_READPATH";
  }

  public void constructByteArray(ByteArrayWriter baw) throws java.io.IOException, com.sshtools.j2ssh.transport.InvalidMessageException {
    baw.writeUINT32(id);
    baw.writeString(path);
  }
}