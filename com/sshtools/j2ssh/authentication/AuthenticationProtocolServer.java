package com.sshtools.j2ssh.authentication;

import com.sshtools.j2ssh.transport.*;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.sshtools.j2ssh.configuration.ServerConfiguration;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;

import java.io.InputStream;
import java.io.IOException;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class AuthenticationProtocolServer extends AsyncService {

    private static Logger log = Logger.getLogger(AuthenticationProtocolServer.class);
    private List completedAuthentications = new ArrayList();
    private Map acceptServices = new HashMap();
    public List availableAuths;
    private String serviceToStart;
    private int[] messageFilter = new int[1];
    private SshMessageStore methodMessages = new SshMessageStore();

  public AuthenticationProtocolServer() {
    super("ssh-userauth");
    messageFilter[0] = SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST;

  }
  protected void onServiceAccept() throws java.io.IOException {
    /**@todo: implement this com.sshtools.j2ssh.transport.Service abstract method*/
  }

  protected void onServiceInit(int startMode) throws java.io.IOException {
    // Register the required messages
    messageStore.registerMessage(SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST,
            SshMsgUserAuthRequest.class);
    transport.addMessageStore(methodMessages);
  }

  public byte[] getSessionIdentifier() {
    return transport.getSessionIdentifier();
  }

  public void sendMessage(SshMessage msg) throws IOException {
    transport.sendMessage(msg, this);
  }

  public SshMessage readMessage() throws IOException {
    return methodMessages.nextMessage();
  }

  public void registerMessage(int messageId, Class cls) {
    methodMessages.registerMessage(messageId, cls);
  }

  protected void onServiceRequest() throws java.io.IOException {

    availableAuths = SshAuthenticationServerFactory.getSupportedMethods();

    // Accept the service request
    sendServiceAccept();

    // Send a user auth banner if configured
    ServerConfiguration server =
            ConfigurationLoader.getServerConfiguration();

    if (server == null) {
        throw new AuthenticationProtocolException("Server configuration unavailable");
    }

    String bannerFile = server.getAuthenticationBanner();

    if (bannerFile != null) {
        if (bannerFile.length() > 0) {
            InputStream in = ConfigurationLoader.loadFile(bannerFile);

            if (in != null) {
                    byte data[] = new byte[in.available()];
                    in.read(data);
                    in.close();

                    SshMsgUserAuthBanner bannerMsg =
                            new SshMsgUserAuthBanner(new String(data));
                    transport.sendMessage(bannerMsg, this);

            } else {
                log.info("The banner file '" + bannerFile
                        + "' was not found");
            }
        }
    }
  }

  protected void onMessageReceived(SshMessage msg) throws java.io.IOException {
     switch (msg.getMessageId()) {
          case SshMsgUserAuthRequest.SSH_MSG_USERAUTH_REQUEST:
          {
              onMsgUserAuthRequest((SshMsgUserAuthRequest) msg);

              break;
          }

          default:
              throw new AuthenticationProtocolException("Unregistered message received!");
    }
  }


  protected int[] getAsyncMessageFilter() {
     return messageFilter;
  }

    /**
     *  Configures the Authentication Protocol to allow authentication attempts
     *  to start a Transport Protocol service.
     *
     *@param  service  The service instance to start
     */
    public void acceptService(Service service) {
        acceptServices.put(service.getServiceName(), service);
    }


    /**
     *  Sends the SSH_USERAUTH_FAILURE message
     *
     *@param  success                      True if a partial success
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     *@throws  ServiceOperationException   if a critical error occurs in the
     *      service operation
     */
    private void sendUserAuthFailure(boolean success)
             throws IOException,
            ServiceOperationException {
        Iterator it = availableAuths.iterator();
        String auths = null;

        while (it.hasNext()) {
            auths = ((auths == null) ? "" : auths + ",") + (String) it.next();
        }

        SshMsgUserAuthFailure reply = new SshMsgUserAuthFailure(auths, success);
        transport.sendMessage(reply, this);
    }


    /**
     *  Sends the SSH_USERAUTH_SUCCESS message
     *
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     *@throws  ServiceOperationException   if a critical error occurs in the
     *      service operation
     */
    private void sendUserAuthSuccess()
             throws IOException {
        SshMsgUserAuthSuccess msg = new SshMsgUserAuthSuccess();
        Service service = (Service) acceptServices.get(serviceToStart);
        service.init(Service.ACCEPTING_SERVICE, transport, nativeSettings);
        service.start();
        transport.sendMessage(msg, this);
        stop();
    }


    /**
     *  Handles the SSH_USERAUTH_REQUEST message
     *
     *@param  msg
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     *@throws  ServiceOperationException   if a critical error occures in the
     *      service operation
     */
    private void onMsgUserAuthRequest(SshMsgUserAuthRequest msg)
             throws IOException,
            ServiceOperationException {
        if (msg.getMethodName().equals("none")) {
            sendUserAuthFailure(false);
        } else {
            // If the service is supported then perfrom the authentication
            if (acceptServices.containsKey(msg.getServiceName())) {
                String method = msg.getMethodName();

                if (availableAuths.contains(method)) {
                    SshAuthenticationServer auth =
                            SshAuthenticationServerFactory.newInstance(method);
                    serviceToStart = msg.getServiceName();
                    auth.setUsername(msg.getUsername());
                    int result = auth.authenticate(this, msg, nativeSettings);
                    if(result == AuthenticationProtocolState.FAILED) {
                      sendUserAuthFailure(false);
                    }
                    else if(result == AuthenticationProtocolState.COMPLETE) {
                      completedAuthentications.add(auth.getMethodName());
                      ServerConfiguration sc =
                            ConfigurationLoader.getServerConfiguration();
                      Iterator it = sc.getRequiredAuthentications().iterator();
                      while(it.hasNext()) {
                        if(!completedAuthentications.contains(it.next())) {
                          sendUserAuthFailure(true);
                          return;
                        }
                      }
                      nativeSettings.put("Username", msg.getUsername());
                      sendUserAuthSuccess();
                    } else {
                      // Authentication probably returned READY as no complettion
                      // evaluation was needed
                    }

                } else {
                    sendUserAuthFailure(false);
                }
            } else {
                sendUserAuthFailure(false);
            }
        }
    }
}