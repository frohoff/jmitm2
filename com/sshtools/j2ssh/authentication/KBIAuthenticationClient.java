package com.sshtools.j2ssh.authentication;

import java.util.*;
import java.awt.*;
import com.sshtools.j2ssh.io.*;
import com.sshtools.j2ssh.transport.SshMessage;
import javax.swing.JOptionPane;

public class KBIAuthenticationClient extends SshAuthenticationClient {

  KBIRequestHandler handler;
  Component parent = null;

  public Properties getPersistableProperties() {
    return new Properties();
  }

  public void setKBIRequestHandler(KBIRequestHandler handler) {
    this.handler = handler;
  }

  public void authenticate(AuthenticationProtocolClient authentication, String serviceToStart) throws com.sshtools.j2ssh.authentication.TerminatedStateException, java.io.IOException {

    if(handler==null)
      throw new AuthenticationProtocolException("A request handler must be set!");

    authentication.registerMessage(SshMsgUserAuthInfoRequest.class
                                   , SshMsgUserAuthInfoRequest.SSH_MSG_USERAUTH_INFO_REQUEST);
    // Send the authentication request
    ByteArrayWriter baw = new ByteArrayWriter();
    baw.writeString("");
    baw.writeString("");
    SshMessage msg = new SshMsgUserAuthRequest(getUsername(),
        serviceToStart,
        getMethodName(),
        baw.toByteArray());

    authentication.sendMessage(msg);

    // Read a message
    while(true) {
      msg = authentication.readMessage(SshMsgUserAuthInfoRequest.SSH_MSG_USERAUTH_INFO_REQUEST);

      if (msg instanceof SshMsgUserAuthInfoRequest) {
        SshMsgUserAuthInfoRequest request = (SshMsgUserAuthInfoRequest)msg;
        KBIPrompt[] prompts = request.getPrompts();
        handler.showPrompts(request.getName(),
                                  request.getInstruction(),
                                  prompts);

        // Now send the response message
        msg = new SshMsgUserAuthInfoResponse(prompts);
        authentication.sendMessage(msg);

      }
      else
        throw new AuthenticationProtocolException(
            "Unexpected authentication message "
            + msg.getMessageName());
    }



  }

  public boolean canAuthenticate() {
    return true;
  }

  public String getMethodName() {
  return "keyboard-interactive";
  }

  public void setPersistableProperties(Properties properties) {

  }

  public boolean showAuthenticationDialog(Component parent) throws java.io.IOException {
    final Component myparent = parent;
    this.handler = new KBIRequestHandler() {
      public void showPrompts(String name, String instructions, KBIPrompt[] prompts) {

        /**
         * TODO: A custom dialog that can show the prompts, including masking
         * of the data when prompt[i].echo() == false
         */
        if(prompts!=null) {
          for (int i = 0; i < prompts.length; i++) {
              // We can echo the response back to the client
              prompts[i].setResponse( (JOptionPane.showInputDialog(myparent,
                  prompts[i].getPrompt(),
                  name,
                  JOptionPane.QUESTION_MESSAGE)));
          }
        }
      }
    };
    return true;
  }


}