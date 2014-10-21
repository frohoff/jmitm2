package com.sshtools.j2ssh.transport.publickey;

import java.util.*;
import com.sshtools.j2ssh.configuration.*;
import org.apache.log4j.Logger;

public class SshPrivateKeyFormatFactory {

  private static String defaultFormat;
  private static HashMap formatTypes = new HashMap();
  private static Logger log = Logger.getLogger(SshPrivateKeyFormatFactory.class);
  private static Vector types = new Vector();
  static {

    SshAPIConfiguration config = ConfigurationLoader.getAPIConfiguration();
    defaultFormat = config.getDefaultPrivateKeyFormat();
    log.debug("Default private key format will be " + defaultFormat);
    List formats = config.getPrivateKeyFormats();

    Iterator it = formats.iterator();
    String classname;
    while (it.hasNext()) {
      classname = (String) it.next();
      try {
        Class cls = ConfigurationLoader.getExtensionClass(classname);
        SshPrivateKeyFormat f = (SshPrivateKeyFormat) cls.newInstance();
        log.debug("Installing " + f.getFormatType() + " private key format");
        formatTypes.put(f.getFormatType(), cls);
        types.add(f.getFormatType());
      }
      catch (Exception iae) {
        log.warn("Private key format implemented by " + classname +
                 " will not be available", iae);
      }
    }

    SshPrivateKeyFormat f = new SshtoolsPrivateKeyFormat();
    formatTypes.put(f.getFormatType(), SshtoolsPrivateKeyFormat.class);

  }

  public static List getSupportedFormats() {
    return types;
  }

  public static SshPrivateKeyFormat newInstance(String type) throws
      InvalidSshKeyException {
    try {
      if (formatTypes.containsKey(type))
        return (SshPrivateKeyFormat) ( (Class) formatTypes.get(type)).
            newInstance();
      else
        throw new InvalidSshKeyException("The format type " + type +
                                         " is not supported");
    }
    catch (IllegalAccessException iae) {
      throw new InvalidSshKeyException(
          "Illegal access to class implementation of " + type);
    }
    catch (InstantiationException ie) {
      throw new InvalidSshKeyException(
          "Failed to create instance of format type " + type);
    }
  }

  public static String getDefaultFormatType() {
    return defaultFormat;
  }
}