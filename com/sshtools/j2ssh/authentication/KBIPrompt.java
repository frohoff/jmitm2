package com.sshtools.j2ssh.authentication;

public class KBIPrompt {

    private String prompt;
    private String response;
    private boolean echo;

    protected KBIPrompt(String prompt, boolean echo) {
      this.prompt = prompt;
      this.echo = echo;
    }

    public String getPrompt() { return prompt; }
    public boolean echo() { return echo; }

    public void setResponse(String response) {
      this.response = response;
    }

    public String getResponse() {
      return response;
    }

  }
