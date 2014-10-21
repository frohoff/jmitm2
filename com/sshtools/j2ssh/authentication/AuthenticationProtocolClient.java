package com.sshtools.j2ssh.authentication;

import com.sshtools.j2ssh.transport.*;

import java.util.List;

import java.io.IOException;
import org.apache.log4j.Logger;

public class AuthenticationProtocolClient extends Service {

  private static Logger log = Logger.getLogger(AuthenticationProtocolClient.class);
  private int[] resultFilter = new int[2];
  private int[] singleIdFilter = new int[3];

  public AuthenticationProtocolClient() {
    super("ssh-userauth");
    resultFilter[0] = SshMsgUserAuthSuccess.SSH_MSG_USERAUTH_SUCCESS;
    resultFilter[1] = SshMsgUserAuthFailure.SSH_MSG_USERAUTH_FAILURE;

    singleIdFilter[0] = SshMsgUserAuthSuccess.SSH_MSG_USERAUTH_SUCCESS;
    singleIdFilter[1] = SshMsgUserAuthFailure.SSH_MSG_USERAUTH_FAILURE;
  }

  protected void onServiceAccept() throws java.io.IOException {

  }

  protected void onStart() { }

  protected void onServiceInit(int startMode) throws java.io.IOException {

      if(startMode==Service.ACCEPTING_SERVICE)
        throw new ServiceOperationException("The Authentication Protocol client cannot be accepted");

      messageStore.registerMessage(SshMsgUserAuthFailure.SSH_MSG_USERAUTH_FAILURE,
              SshMsgUserAuthFailure.class);

      messageStore.registerMessage(SshMsgUserAuthSuccess.SSH_MSG_USERAUTH_SUCCESS,
              SshMsgUserAuthSuccess.class);

      messageStore.registerMessage(SshMsgUserAuthBanner.SSH_MSG_USERAUTH_BANNER,
              SshMsgUserAuthBanner.class);

  }

  protected void onServiceRequest() throws java.io.IOException {
    throw new ServiceOperationException("This class implements the client protocol only!");
  }


    /**
     *  Gets the available authentication methods returned by the server.<br>
     *  <br>
     *  NOTE: The authentication protocol states that the server may return
     *  authentication methods that are not valid for the user.
     *
     *@param  username                     The username to request
     *      authentication methods for
     *@param  serviceName                  The service name to start
     *@return                              The List of Strings detailing the
     *      authentication methods
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     */
    public List getAvailableAuths(String username, String serviceName)
             throws IOException {
        log.info("Requesting authentication methods");

        SshMessage msg =
              new SshMsgUserAuthRequest(username, serviceName, "none", null);

        transport.sendMessage(msg, this);

        msg = messageStore.getMessage(resultFilter);


        if(msg instanceof SshMsgUserAuthFailure) {
            return ((SshMsgUserAuthFailure)msg).getAvailableAuthentications();
        } else {
          throw new ServiceOperationException("None request returned success! Insecure feature not supported");
        }

    }


        /**
     *  Perform's user authentication
     *
     *@param  auth                         The authentication method instance to
     *      try.
     *@param  serviceToStart               The service instance to start.
     *@return                              The result of the authentication;
     *      this is an AuthenticationProtocolState value.
     *@throws  TransportProtocolException  if an error occurs in the Transport
     *      Protocol
     *@throws  ServiceOperationException   if a critical error occurs in the
     *      service operation
     */
    public int authenticate(SshAuthenticationClient auth, Service serviceToStart)
             throws IOException {

           /**
            * The authentication method can register and use additional messages
            * which will be returned by a call to read message, if the authentication
            * is completed or failed the read message throws TerminatedStateException
            * so that the processing is returned automatically to this method.
            *
            * This requires that the authentication methods DO NOT catch the TerminatedStateExcpetion
            * when calling readMessage
            */
      try {

        auth.authenticate(this, serviceToStart.getServiceName());

        SshMessage msg = parseMessage(messageStore.getMessage(resultFilter));

        // We should not get this far
        throw new AuthenticationProtocolException(
          "Unexpected authentication message "
          + msg.getMessageName());

      } catch(TerminatedStateException tse ){
        if(tse.getState()==AuthenticationProtocolState.COMPLETE) {
          serviceToStart.init(Service.ACCEPTING_SERVICE, transport,
                  nativeSettings);
          serviceToStart.start();
        }

        return tse.getState();
      }

     }

     public void sendMessage(SshMessage msg) throws IOException {
       transport.sendMessage(msg, this);
     }

     public byte[] getSessionIdentifier() {
       return transport.getSessionIdentifier();
     }

     public void registerMessage(Class cls, int messageId) {
       messageStore.registerMessage(messageId, cls);
     }

     public SshMessage readMessage(int messageId)
         throws TerminatedStateException, AuthenticationProtocolException {
       singleIdFilter[2] = messageId;
       return internalReadMessage(singleIdFilter);
     }

     private SshMessage internalReadMessage(int[] messageIdFilter)
         throws TerminatedStateException, AuthenticationProtocolException {
       try {
          SshMessage msg = messageStore.getMessage(messageIdFilter);

          return parseMessage(msg);

        } catch(MessageStoreEOFException meof) {
          throw new AuthenticationProtocolException("Failed to read messages");
        }

     }

     public SshMessage readMessage(int[] messageId) throws TerminatedStateException,
                                        AuthenticationProtocolException {
      int[] messageIdFilter = new int[messageId.length + resultFilter.length];
      System.arraycopy(resultFilter, 0, messageIdFilter, 0, resultFilter.length);
      System.arraycopy(messageId, 0, messageIdFilter, resultFilter.length, messageId.length);
      return internalReadMessage(messageIdFilter);

     }

     private SshMessage parseMessage(SshMessage msg)
         throws TerminatedStateException {

         if (msg instanceof SshMsgUserAuthFailure) {
           if ( ( (SshMsgUserAuthFailure) msg).getPartialSuccess())
             throw new TerminatedStateException(AuthenticationProtocolState.
                                                PARTIAL);
           else
             throw new TerminatedStateException(AuthenticationProtocolState.FAILED);
         }
         else if (msg instanceof SshMsgUserAuthSuccess) {
           throw new TerminatedStateException(AuthenticationProtocolState.COMPLETE);
         }
         else
          return msg;



     }

    /**
     *  Gets the authentication banner message received from the server. This
     *  may be null.
     *
     *@return    A String containing the banner message
     */
    public String getBannerMessage(int timeout) {
      /**
       *
       */
      try {
        log.debug("getBannerMessage is attempting to read the authentication banner");
      SshMessage msg =
        messageStore.peekMessage(SshMsgUserAuthBanner.SSH_MSG_USERAUTH_BANNER, timeout);

      return ((SshMsgUserAuthBanner)msg).getBanner();

      } catch(MessageNotAvailableException e) {
         return "";
      } catch(MessageStoreEOFException eof) {
        log.error("Failed to retreive banner becasue the message store is EOF");
        return "";
      }
    }
}