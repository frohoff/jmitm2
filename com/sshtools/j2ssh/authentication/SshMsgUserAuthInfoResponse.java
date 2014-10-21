package com.sshtools.j2ssh.authentication;

import com.sshtools.j2ssh.transport.*;
import com.sshtools.j2ssh.io.*;
import java.io.IOException;


public class SshMsgUserAuthInfoResponse extends SshMessage {

  public static final int SSH_MSG_USERAUTH_INFO_RESPONSE = 61;
  private KBIPrompt[] prompts;
  String[] responses;

  public SshMsgUserAuthInfoResponse() {
    super(SSH_MSG_USERAUTH_INFO_RESPONSE);
  }

  public SshMsgUserAuthInfoResponse(KBIPrompt[] prompts) {
    super(SSH_MSG_USERAUTH_INFO_RESPONSE);
    if(prompts!=null) {
      responses = new String[prompts.length];
      for (int i = 0; i < responses.length; i++)
        responses[i] = prompts[i].getResponse();
    }
  }

  public String getMessageName() {
    return "SSH_MSG_USERAUTH_INFO_RESPONSE";
  }

  public String[] getResponses() {
    return responses;
  }

  protected void constructByteArray(ByteArrayWriter baw) throws com.sshtools.j2ssh.transport.InvalidMessageException {
    try {
      if(responses==null)
        baw.writeInt(0);
      else {
        baw.writeInt(responses.length);
        for(int i=0;i<responses.length;i++) {
          baw.writeString(responses[i]);
        }
      }
    } catch(IOException ioe) {
      throw new InvalidMessageException("Failed to write message data");
    }
  }
  protected void constructMessage(ByteArrayReader bar) throws com.sshtools.j2ssh.transport.InvalidMessageException {
    try {
      int num = (int)bar.readInt();
      if(num >0) {
        responses = new String[num];
        for(int i=0;i<responses.length;i++)
          responses[i] = bar.readString();
      }
    } catch(IOException ioe) {
      throw new InvalidMessageException("Failed to read message data");
    }
  }
}