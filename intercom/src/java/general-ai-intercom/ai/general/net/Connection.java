/* General AI - Networking
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

/**
 * Base class for connections to remote endpoints.
 *
 * Subclasses of Connection implement a specific connection protocol.
 *
 * The Connection class treats both endpoints of the connection symmetrically. The connection
 * can be either at the client side or the server side.
 * Typically, the remote endpoint will have a corresponding Connection instance that processes
 * requests sent by this Connection.
 *
 * The Connection class supports two types of requests: publish and RPC.
 *
 * A Connection can publish some data on a topic path. A Connection can also subscribe to receive
 * data on a topic path.
 *
 * In addition, a connection may sent a reuqest for a remote procedure call. Once the remote
 * procedure has completed execution an RPC callback with the result or any errors is called.
 *
 * The main difference between publish and RPC is that publish requests are fire-and-forget.
 * Unless the client is subscribed to the topic it publishes, no response is received from the
 * remote endpoint. On the other hand a response is always sent to an RPC request even if the
 * RPC does not produce a return value.
 * In principle, both publish and RPC requests can be broadcast to more than one receiver. However,
 * more typically RPC requests are processed by one receiver.
 *
 * Publish requests can be forwarded to another connection by the remote endpoint. In other words,
 * a publish request can be sent to a third party that is either directly or indirectly connected
 * to the remote endpoint. In general, RPC request cannot be easily forwarded since they are not
 * fire-and-forget. Thus, requests that require forwarding should be made as publish requests.
 *
 * All requests path, whether topic paths or method paths, refer to nodes in the
 * {@link ai.general.directory.Directory}. Requests path are interpreted with respect to the home
 * directory path, which may be associated with a user account. Thus, the same path may target two
 * different Directory nodes depending on the user account associated with the request.
 *
 * Both endpoints are expected to have a node directory. The two endpoints may use different
 * home paths to map request paths into the appropriate context. For example, on the client side
 * the home path may be specified as the root node '/'. On the server side, the home path may
 * be the user account home path such as '/home/user1/'. A request with path '/x/y/z' will map to
 * node '/x/y/z' on the client side and to node '/home/user1/x/y/z' on the server side.
 *
 * In order to grant a user access to resources not under the user home path, the
 * {@link ai.general.directory.Directory} class can be used to create links from a node under the
 * home path of the user to a resource outside the home path. This allows a more scalable and
 * secure authentication architecture.
 *
 * Subclasses of this class must implement the protocol specific methods to make publish and RPC
 * requests.
 * RPC methods:
 * <p><ul>
 * <li>{@link #call(String, RpcCallback, Object...)}</li>
 * <li>{@link #call(Uri, RpcCallback, Object...)}</li>
 * </ul></p>
 *
 * Publish and subscribe/unsubscribe methods:
 * <p><ul>
 * <li>{@link #publish(String, Object)}</li>
 * <li>{@link #publish(Uri, Object)}</li>
 * <li>{@link #publish(String, Object, boolean)}</li>
 * <li>{@link #publish(String, Object, String[], String[])}</li>
 * <li>{@link #subscribe(String)}</li>
 * <li>{@link #subscribe(Uri)}</li>
 * <li>{@link #unsubscribe(String)}</li>
 * <li>{@link #unsubscribe(Uri)}</li>
 * </ul></p>
 *
 * In addition to making requests, the Connection class also processes incoming requests. This
 * is done via the {@link #process(String)} method. The input to the process method is the
 * protocol specific output produced by the remote endpoint.
 *
 * Subclasses must call the {@link #setIsReady(boolean)} method to indicate when the connection is
 * ready to receive and send messages. Depending on the connection protocol, a connection may
 * become ready asynchronously. Thus, a connection may not be immediately ready after it has
 * been constructed. Calling setIsReady(boolean) ensures that all services are notified when the
 * connection can be used to send and receive messages. Services may use this event to subscribe
 * to events or start making RPC calls.
 *
 * The {@link #close()} method notifies all services that the connection is closing. Services may
 * use this event to unsubscribe from events. If the close() method is overriden by a subclass, the
 * superclass version must be called to ensure that all services are disconnected. This should be
 * done before the connection is actually closed.
 *
 * Subclasses should set the session ID and server ID when these values are known. These values
 * are not set in the constructor since they are typically not known until the connection handshake
 * has completed. These values should be set via the {@link #setSessionId(String)} and
 * {@link #setServerId(String)} methods as soon as they are known by the subclass.
 */
public abstract class Connection {

  /**
   * Creates a connection instance and initializes it with the specified parameters.
   * The server ID and session ID are usually not known at instantiation time and need to be
   * set when they have been determined or received from the remote endpoint.
   *
   * Every connection is associated with a connection URI. On the server side, the connection URI
   * specifies the URI of the server endpoint at which the request was received. On the client
   * side, the connection URI specifies the URI of the server endpoint to which the client is
   * connected.
   *
   * A connection can be associated with a user account that is transmitted in requests.
   * The connection home path refers to the home path node in the
   * {@link ai.general.directory.Directory}. If a user account is specified, the home path should
   * be the user account home path or a node reachable by the user account home path. If no user
   * account is specified, the home path can be the root node of the directory ('/') or any other
   * node.
   * Any handlers associated with the connection will be added to a node that is under the
   * connection home path.
   * Requests paths are interpreted with respect to the connection home path. Thus, a request for
   * the resource '/x/y/z' is interpreted as a request for '/home_path/x/y/z'. Requests may not
   * refer to resources that are not reachable from the home path node.
   *
   * @param uri The URI of the server endpoint associated with this connection.
   * @param user_account User account associated with this connection or null if none.
   * @param home_path The home directory for the user account or the root directory.
   */
  protected Connection(Uri uri, String user_account, String home_path) {
    this.uri_ = uri;
    this.user_account_ = user_account;
    this.home_path_ = home_path;
    if (!home_path_.startsWith("/")) {
      home_path_ = "/" + home_path_;
    }
    if (home_path_.endsWith("/")) {
      home_path_ = home_path_.substring(0, home_path_.length() - 1);
    }
    is_ready_ = false;
    server_id_ = null;
    session_id_ = null;
  }

  /**
   * Makes an RPC call to the remote endpoint at the specified method path.
   * This method creates an appropriate URI for the call.
   *
   * The RPC is executed asynchronously. When completed the provided RpcCallback will be called.
   *
   * @param method_path Method URI path of the RPC method to call.
   * @param callback The callback to invoke when the RPC returns.
   * @param arguments RPC method arguments.
   * @return True if the call was sent.
   */
  public abstract boolean call(String method_path, RpcCallback callback, Object ... arguments);

  /**
   * Makes an RPC call to the remote endpoint at the specified method URI.
   *
   * The RPC is executed asynchronously. When completed the provided RpcCallback will be called.
   *
   * @param method_uri Method URI of the RPC method to call.
   * @param callback The callback to invoke when the RPC returns.
   * @param arguments RPC method arguments.
   * @return True if the call was sent.
   */
  public abstract boolean call(Uri method_uri, RpcCallback callback, Object ... arguments);

  /**
   * Closes the connection.
   *
   * By default, this method calls {@link #setIsReady(boolean)} with a value of false, which
   * disconnects all services from the connection.
   *
   * If this method is overriden by a subclass, this implementation should be called first.
   */
  public void close() {
    setIsReady(false);
  }

  /**
   * Local home directory path.
   *
   * @return The local home directory path.
   */
  public String getHomePath() {
    return home_path_;
  }

  /**
   * Hostname associated with connection.
   *
   * @return The connection hostname.
   */
  public String getHostname() {
    return uri_.getServer();
  }

  /**
   * The server ID received by the server. This is only valid if this connection is a client
   * connection and is set after the connection has been established.
   *
   * The server ID provides information about the server, such as server version.
   *
   * @return The server ID or null.
   */
  public String getServerId() {
    return server_id_;
  }

  /**
   * The session ID. This is only valid if this connection is a client connection and it set after
   * the connection has been established.
   *
   * @return The session ID or null.
   */
  public String getSessionId() {
    return session_id_;
  }

  /**
   * Returns the URI associated with this connection.
   *
   * On the server side, the connection URI specifies the URI of the server endpoint at which the
   * request was received.
   * On the client side, the connection URI specifies the URI of the server endpoint to which the
   * client is connected.
   *
   * @return The connection URI.
   */
  public Uri getUri() {
    return uri_;
  }

  /**
   * User account associated with this connection.
   *
   * @return User account associated with this connection or null if none.
   */
  public String getUserAccount() {
    return user_account_;
  }

  /**
   * True if this connection can be used to send or receive messages.
   *
   * A connection starts in the not ready state and becomes ready when the connection is open and
   * the connection handshake has successfully completed.
   *
   * A connection becomes not ready again when the connection is closed.
   *
   * @return True if messages can be sent or received using the connection.
   */
  public boolean isReady() {
    return is_ready_;
  }

  /**
   * Processes an incoming message. This method processes messages both on the server and client
   * side.
   *
   * This method returns true if the input conforms to the protocol and could successfully be
   * interpreted. The method does return true even if the processing of the request has resulted
   * in a logic error as long as the input conforms to the protocol.
   *
   * @param input Message received from remote endpoint.
   * @return True if the input was successfully interpreted.
   */
  public abstract boolean process(String input);

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data. This method creates the appropriate URI.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @return True if the request was sent.
   */
  public abstract boolean publish(String topic_path, Object data);

  /**
   * Sends a publish request to the remote endpoint for the specified topic URI with the provided
   * data.
   *
   * Any additional publish parameters may be provided via URI query parameters.
   *
   * @param topic_uri The topic URI to publish to.
   * @param data The data to publish.
   * @return True if the request was sent.
   */
  public abstract boolean publish(Uri topic_uri, Object data);

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data.
   *
   * This method allows explicit specification of the exclude_me argument.
   * If exclude_me is true and this connection is subscribed to the topic, the publish message
   * will not be sent back to this connection. Otherwise, this connection will receive a copy of
   * the published message.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @param exclude_me Value of exclude_me argument.
   * @return True if the request was sent.
   */
  public abstract boolean publish(String topic_path, Object data, boolean exclude_me);

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data.
   *
   * This method allows explicit specification or exclude or eligible lists.
   * The exclude and eligible lists are lists of session ID's.
   * If an exclude list list is provided, the publish message is sent to all subscribers of
   * the topic except those in the exclude list.
   * If an eligible list is provided, the publish message is sent only to subscribers of the topic
   * which are also explicitly listed in the eligible list.
   * The subscribers are identified via their session ID's.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @param exclude List of target session ID's to exclude from the publish request.
   * @param eligible Explicit list of target session ID's to include in the publish request.
   * @return True if the request was sent.
   */
  public abstract boolean publish(String topic_path,
                                  Object data,
                                  String[] exclude, 
                                  String[] eligible);

  /**
   * Sends a subscribe request to the remote endpoint for the specified topic path.
   *
   * This method generates the appropriate URI for the request based on information provided
   * during construction of this object.
   * The path must be absolute.
   *
   * @param topic_path The topic path to subscribe to.
   * @return True if the request was sent.
   */
  public abstract boolean subscribe(String topic_path);

  /**
   * Sends a subscribe request to the remote endpoint for the specified topic URI.
   *
   * @param topic_uri The topic URI to subscribe to.
   * @return True if the request was sent.
   */
  public abstract boolean subscribe(Uri topic_uri);

  /**
   * Sends an unsubscribe request to the remote endpoint for the specified topic path.
   *
   * This method generates the appropriate URI for the request based on information provided
   * during construction of this object.
   * This method is the counterpart of {@link #subscribe(String)}.
   *
   * @param topic_path The topic path to unsubscribe from.
   * @return True if the request was sent.
   */
  public abstract boolean unsubscribe(String topic_path);

  /**
   * Sends an unsubscribe request to the remote endpoint for the specified topic URI.
   * This method is the counterpart of {@link #unsubscribe(Uri)}.
   *
   * @param topic_uri The topic URI to unsubscribe from.
   * @return True if the request was sent.
   */
  public abstract boolean unsubscribe(Uri topic_uri);

  /**
   * Sets the local home directory path.
   *
   * @param home_path The local home directory path.
   */
  protected void setHomePath(String home_path) {
    this.home_path_ = home_path;
  }

  /**
   * Sets whether this connection is ready to send and receive messages.
   *
   * The connection starts in the not ready state and becomes ready when connection has been opened
   * and the connection handshake has successfully completed. When the connection becomes ready,
   * services are notified that the connection is ready and may start using the connection to
   * exchange messages.
   * The connection becomes not ready again when it is closed.
   *
   * A Connection subclass must call this method with a value of true when the connection is ready.
   * This method must be also called with a value of false when the connection is closed. The
   * default {@link #close()} method automatically calls this method with a value of false.
   *
   * When the connection is ready, the connection instance is added to the
   * {@link ConnectionManager}. When the connection is disconnected it is removed from the
   * ConnectionManager. Calling this method ensures that all services are notified when the
   * connection becomes ready or is closed.
   *
   * @param is_ready True if messages can be sent or received using the connection.
   */
  protected void setIsReady(boolean is_ready) {
    if (this.is_ready_ != is_ready) {
      this.is_ready_ = is_ready;
      if (is_ready) {
        ConnectionManager.Instance.add(this);
      } else {
        ConnectionManager.Instance.remove(this);
      }
    }
  }

  /**
   * Sets the server ID received by the server.
   *
   * @param server_id The server ID or null.
   */
  protected void setServerId(String server_id) {
    this.server_id_ = server_id;
  }

  /**
   * Sets the session ID.
   *
   * @param session_id The session ID or null.
   */
  protected void setSessionId(String session_id) {
    this.session_id_ = session_id;
  }

  /**
   * Sets the user account associated with this connection.
   *
   * @param user_account User account associated with this connection or null if none.
   */
  protected void setUserAccount(String user_account) {
    this.user_account_ = user_account;
  }

  private String home_path_;  // The home directory of this user account.
  private boolean is_ready_;  // True if the connection handshake successfully completed.
  private String server_id_;  // Server identification received during welcome handshake.
  private String session_id_;  // Session ID of WAMP session.
  private Uri uri_;  // The connection URI.
  private String user_account_;  // Account name or null if none.
}
