package com.sshtools.j2ssh.transport.publickey;


import java.util.*;
import com.sshtools.j2ssh.configuration.*;
import org.apache.log4j.Logger;

public class SshPublicKeyFormatFactory {

  private static String defaultFormat;
  private static HashMap formatTypes = new HashMap();
  private static Logger log = Logger.getLogger(SshPublicKeyFormatFactory.class);
  private static Vector types = new Vector();
  static {


    SshAPIConfiguration config = ConfigurationLoader.getAPIConfiguration();
    defaultFormat = config.getDefaultPublicKeyFormat();
    log.debug("Default public key format will be " + defaultFormat);
    List formats = config.getPublicKeyFormats();

    Iterator it = formats.iterator();
    String classname;
    while(it.hasNext()) {
      classname = (String)it.next();
      try {
        Class cls = ConfigurationLoader.getExtensionClass(classname);
        SshPublicKeyFormat f = (SshPublicKeyFormat)cls.newInstance();
        log.debug("Installing " + f.getFormatType() + " public key format");
        formatTypes.put(f.getFormatType(), cls);
        types.add(f.getFormatType());
      } catch(Exception iae) {
        log.warn("Public key format implemented by " + classname + " will not be available",iae);
      }
    }

    SshPublicKeyFormat f = new SECSHPublicKeyFormat();
    formatTypes.put(f.getFormatType(), SECSHPublicKeyFormat.class);
    f = new OpenSSHPublicKeyFormat();
    formatTypes.put(f.getFormatType(), OpenSSHPublicKeyFormat.class);



  }

  public static List getSupportedFormats() {
    return types;
  }

  public static SshPublicKeyFormat newInstance(String type) throws InvalidSshKeyException {
    try {
      if (formatTypes.containsKey(type))
        return (SshPublicKeyFormat) ( (Class) formatTypes.get(type)).
            newInstance();
      else
        throw new InvalidSshKeyException("The format type " + type +
                                         " is not supported");
    } catch(IllegalAccessException iae) {
      throw new InvalidSshKeyException("Illegal access to class implementation of " + type);
    } catch(InstantiationException ie) {
      throw new InvalidSshKeyException("Failed to create instance of format type " + type);
    }
  }

  public static String getDefaultFormatType() {
    return defaultFormat;
  }
}