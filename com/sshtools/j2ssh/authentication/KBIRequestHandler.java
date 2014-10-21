package com.sshtools.j2ssh.authentication;

public interface KBIRequestHandler {

  public void showPrompts(String name, String instruction, KBIPrompt[] prompts);

}
