package com.sshtools.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.sftp.*;
import java.io.*;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.transport.cipher.SshCipherFactory;
import com.sshtools.j2ssh.transport.hmac.SshHmacFactory;
import com.sshtools.j2ssh.transport.AbstractHostKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;
import com.sshtools.j2ssh.transport.publickey.SshtoolsPrivateKeyFormat;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.session.SessionChannelClient;
import org.apache.tools.ant.TaskContainer;
import java.util.Vector;
import java.util.Iterator;

/**
 * @author Lee David Painter
 * @version $Id: Ssh.java,v 1.3 2003/02/17 16:06:02 martianx Exp $
 */

public class Ssh extends Task {

  protected String host;
  protected int port = 22;
  protected String username;
  protected String password;
  protected String keyfile;
  protected String passphrase;
  protected String cipher;
  protected String mac;
  protected String fingerprint;
  protected String logfile = "ssh.log";
  protected boolean verifyhost = true;
  protected boolean always = false;
  protected SshClient ssh;
  protected Vector tasks = new Vector();


  public Ssh() {
    super();
  }

  public void addConfiguredSftp(Sftp sftp) {
    tasks.add(sftp);
  }

  public void addConfiguredExec(Exec cmd) {
    tasks.add(cmd);
  }

  public void execute() throws org.apache.tools.ant.BuildException {

    try {

      if(host==null)
        throw new BuildException("You must provide a host to connect to!");

      if(username==null)
        throw new BuildException("You must supply a username for authentication!");

      if(password==null && keyfile==null)
        throw new BuildException("You must supply either a password or keyfile/passphrase to authenticate!");

      if(verifyhost && fingerprint==null)
        throw new BuildException("Public key fingerprint required to verify the host");


      log("Initializing J2SSH");

      System.setProperty("sshtools.logfile", logfile);
      ConfigurationLoader.initialize();

      log("Creating connection to " + host + ":" + String.valueOf(port));

      if(ssh==null) {
        ssh = new SshClient();

        SshConnectionProperties properties = new SshConnectionProperties();
        properties.setHost(host);
        properties.setPort(port);
        properties.setUsername(username);

        if(cipher!=null) {
          if(SshCipherFactory.getSupportedCiphers().contains(cipher)) {
            properties.setPrefSCEncryption(cipher);
            properties.setPrefCSEncryption(cipher);
          } else {
            this.log(cipher + " is not a supported cipher, using default " + SshCipherFactory.getDefaultCipher());
          }
        }

        if(mac!=null) {
          if(SshHmacFactory.getSupportedMacs().contains(mac)) {
            properties.setPrefCSMac(mac);
            properties.setPrefSCMac(mac);
          } else {
            this.log(mac + " is not a supported mac, using default " + SshHmacFactory.getDefaultHmac());
          }
        }

        log("Connecting....");

        ssh.connect(properties, new AbstractHostKeyVerification() {
          public void onUnknownHost(String hostname, String fingerprint)
                                              throws InvalidHostFileException {
            if(Ssh.this.verifyhost) {
              if(fingerprint.equalsIgnoreCase(Ssh.this.fingerprint))
                allowHost(hostname, fingerprint, always);
            } else
              allowHost(hostname, fingerprint, always);

          }

          public void onHostKeyMismatch(String hostname, String allowed, String supplied)
                                            throws InvalidHostFileException {
              if(Ssh.this.verifyhost) {
                if(supplied.equalsIgnoreCase(Ssh.this.fingerprint))
                  allowHost(hostname, supplied, always);
              } else
                allowHost(hostname, supplied, always);
          }

          public void onDeniedHost(String host) {
            log("The server host key is denied!");
          }

        });

        int result;
        boolean authenticated = false;

        log("Authenticating " + username);
        if(keyfile!=null) {

            log("Performing public key authentication");
            PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();

            // Open up the private key file
            SshPrivateKeyFile file =
                  SshPrivateKeyFile.parse(new File(keyfile));
            // If the private key is passphrase protected then ask for the passphrase
            if (file.isPassphraseProtected() && passphrase==null) {
              throw new BuildException("Private key file is passphrase protected, please supply a valid passphrase!");
            }
            // Get the key
            SshPrivateKey key = file.toPrivateKey(passphrase);
            pk.setUsername(username);
            pk.setKey(key);

            // Try the authentication
            result = ssh.authenticate(pk);

            if(result==AuthenticationProtocolState.COMPLETE) {
              authenticated = true;
            }
            else if(result==AuthenticationProtocolState.PARTIAL)
              log("Public key authentication completed, attempting password authentication");
            else
              throw new BuildException("Public Key Authentication failed!");

        }

        if(password!=null && authenticated==false) {
          log("Performing password authentication");

          PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
          pwd.setUsername(username);
          pwd.setPassword(password);

          result = ssh.authenticate(pwd);
          if(result==AuthenticationProtocolState.COMPLETE) {
            log("Authentication complete");
         }
          else if(result==AuthenticationProtocolState.PARTIAL)
            throw new BuildException("Password Authentication succeeded but further authentication required!");
          else
            throw new BuildException("Password Authentication failed!");
        }

      }

      SessionChannelClient session = ssh.openSessionChannel();
      SessionChannelClient session2 = null;
      SftpSubsystemClient sftp = null;
      /**
       *
       */
      Iterator it = tasks.iterator();
      while(it.hasNext()){


        Object obj= it.next();
        if(obj instanceof Exec) {

          session = ssh.openSessionChannel();
          executeCommand(session, (Exec)obj);
          if(!session.isClosed())
            session.close();

        }

        if(obj instanceof Sftp) {

          if(sftp==null) {
            session2 = ssh.openSessionChannel();
            sftp = new SftpSubsystemClient();
            if(!session2.startSubsystem(sftp))
              throw new BuildException("Failed to start the SFTP subsystem");
          }

          executeSFTP(sftp, (Sftp)obj);

        }
      }

      if(sftp!=null)
        sftp.stop();

      if(session2!=null)
        session2.close();

      log("Disconnecting from " + host);

      ssh.disconnect();


    } catch(IOException sshe) {
      throw new BuildException("SSH Connection failed: " + sshe.getMessage());
    }
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setKeyfile(String keyfile) {
    this.keyfile = keyfile;
  }

  public void setPassphrase(String passphrase){
    this.passphrase = passphrase;
  }

  public void setCipher(String cipher) {
    this.cipher = cipher;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public void setLogfile(String logfile) {
    this.logfile = logfile;
  }

  public void setVerifyhost(boolean verifyhost) {
    this.verifyhost = verifyhost;
  }

  public void setAlways(boolean always) {
    this.always = always;
  }



  /**
   *
   */
  public void executeSFTP(SftpSubsystemClient sftp, Sftp element) throws BuildException {


      element.validate();

      String rmdir = element.getRmdir();
      if(rmdir!=null) {

        log("Deleting directory " + rmdir);
        try {
          SftpFile file = sftp.openDirectory(rmdir);
          file.close();
          sftp.removeDirectory(rmdir);
          log("Deleted directory");
        } catch(IOException ioe) {
          log("Directory does not exist!");
        }

      }

      String mkdir = element.getMkdir();
      if(mkdir!=null) {

        log("Creating directory " + mkdir);
        try {
          sftp.openDirectory(mkdir);
          log("Directory already exists!");
        } catch(IOException ioe) {
          try {
           sftp.makeDirectory(mkdir);
           log("Directory created");
          } catch(IOException ioe2) {
            log("mkdir failed: " + ioe2.getMessage());
          }
        }



      }

      String delete = element.getDelete();
      if(delete!=null) {

        log("Deleting file " + delete);
        try {
          SftpFile file = sftp.openFile(delete, SftpSubsystemClient.OPEN_READ);
          file.close();
          sftp.removeFile(delete);
          log("File deleted");
        } catch(IOException ioe) {
          log("File does not exist!");
        }



      }

      String get = element.getGet();
      String dest = element.getDest();
      String put = element.getPut();

      if(get!=null && dest!=null) {

        log("Getting " + get + " into " + dest);
        try {
          SftpFile file = sftp.openFile(get, SftpSubsystemClient.OPEN_READ);
          byte[] buffer = new byte[65535];
          BufferedInputStream in = new BufferedInputStream(new SftpFileInputStream(file));
          BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
          int read;
          while((read = in.read(buffer))!=-1) {
            out.write(buffer,0,read);
          }
          in.close();
          out.close();
          log("Get complete");
        } catch(IOException ioe) {
          log("get failed: " + ioe.getMessage());
        }
      }

      if(put!=null && dest!=null) {

        log("Putting " + put + " into " + dest);

        try {
          SftpFile file = sftp.openFile(put, SftpSubsystemClient.OPEN_WRITE
                                                | SftpSubsystemClient.OPEN_CREATE);
          byte[] buffer = new byte[65535];
          BufferedInputStream in = new BufferedInputStream(new FileInputStream(dest));
          BufferedOutputStream out = new BufferedOutputStream(new SftpFileOutputStream(file));
          int read;
          while((read = in.read(buffer))!=-1) {
            out.write(buffer,0,read);
          }
          in.close();
          out.close();
          log("Put complete");
        } catch(IOException ioe) {
          log("put failed: " + ioe.getMessage());
        }
      }

  }

    public void executeCommand(SessionChannelClient session, Exec cmd) throws BuildException {

    try {

      cmd.validate();

      log("Executing " + cmd.getCmd());
      session.executeCommand(cmd.getCmd());

      BufferedReader stdout =
		new BufferedReader(new InputStreamReader(session.getInputStream()));

	    /*
	     * Read all output sent to stdout (line by line) and print it to our
	     * own stdout.
	     */
	    String line;
	    while((line = stdout.readLine()) != null && !session.isClosed()) {
		log(line);
	    }

            Integer exitcode = session.getExitCode();

            if(exitcode!=null) {
              if(exitcode.intValue()!=0)
                throw new BuildException("Exit code " + exitcode.toString());
            }


    } catch(IOException ioe) {
      throw new BuildException("The session failed: " + ioe.getMessage());
    } finally {

    }


  }
}