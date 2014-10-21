package com.sshtools.j2ssh.authentication;

import com.sshtools.j2ssh.transport.*;
import java.util.*;
import java.util.Map;
import com.sshtools.j2ssh.io.*;
import java.io.*;



public class SshMsgUserAuthInfoRequest extends SshMessage {

  public static final int SSH_MSG_USERAUTH_INFO_REQUEST = 60;

  private String name;
  private String instruction;
  private String langtag;
  private KBIPrompt[] prompts;

  public SshMsgUserAuthInfoRequest() {
    super(SSH_MSG_USERAUTH_INFO_REQUEST);
  }

  public SshMsgUserAuthInfoRequest(String name, String instruction, String langtag) {
    super(SSH_MSG_USERAUTH_INFO_REQUEST);
    this.name = name;
    this.instruction = instruction;
    this.langtag = langtag;
  }

  public void addPrompt(String prompt, boolean echo) {

    if(prompts==null) {
      prompts = new KBIPrompt[1];
      prompts[0] = new KBIPrompt(prompt,echo);
    } else {
      KBIPrompt[] temp = new KBIPrompt[prompts.length+1];
      System.arraycopy(prompts,0,temp,0,prompts.length);
      prompts = temp;
      prompts[prompts.length-1] = new KBIPrompt(prompt, echo);
    }

  }

  public KBIPrompt[] getPrompts() {
    return prompts;
  }

  public String getName() {
    return name;
  }

  public String getInstruction() {
    return instruction;
  }

  public String getLanguageTag() {
    return langtag;
  }

  public String getMessageName() {
    return "SSH_MSG_USERAUTH_INFO_REQUEST";
  }

  protected void constructByteArray(ByteArrayWriter baw) throws com.sshtools.j2ssh.transport.InvalidMessageException {

    try {
      if (name != null)
        baw.writeString(name);
      else
        baw.writeString("");

      if (instruction != null)
        baw.writeString(instruction);
      else
        baw.writeString("");

      if (langtag != null)
        baw.writeString(langtag);
      else
        baw.writeString("");

      if(prompts==null) {
        baw.writeInt(0);
      } else {
        baw.writeInt(prompts.length);

        for(int i=0;i<prompts.length;i++) {
          baw.writeString(prompts[i].getPrompt());
          baw.write(prompts[i].echo()?1:0);
        }

      }
    } catch(IOException ioe) {
      throw new InvalidMessageException("Failed to write message data");
    }

  }
  protected void constructMessage(ByteArrayReader bar) throws com.sshtools.j2ssh.transport.InvalidMessageException {

    try {
      name = bar.readString();
      instruction = bar.readString();
      langtag = bar.readString();
      long num = bar.readInt();
      String prompt;
      boolean echo;

      for(int i=0;i<num;i++) {
        prompt = bar.readString();
        echo = (bar.read()==1);
        addPrompt(prompt,echo);
      }
    } catch(IOException ioe) {
      throw new InvalidMessageException("Failed to read message data");
    }
  }


}