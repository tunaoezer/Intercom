/* General AI - WAMP Server and Client
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net.wamp;

import ai.general.common.RandomString;
import ai.general.common.Strings;
import ai.general.directory.Directory;
import ai.general.directory.Handler;
import ai.general.directory.Request;
import ai.general.directory.Result;
import ai.general.net.Connection;
import ai.general.net.OutputSender;
import ai.general.net.RelayHandler;
import ai.general.net.RpcCallback;
import ai.general.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Represents a connection that uses the WebSocket Application Messaging Protocol (WAMP) as the
 * communication protocol.
 *
 * For details on the WAMP protocol see the <a href="http://wamp.ws">WAMP Specification.</a>.
 *
 * This class implements the entire WAMP specification both for WAMP servers and clients.
 *
 * WampConnection can function both as a server and client at the same time. This implementation
 * is compatible with standard WAMP but extends the standard protocol in the following
 * ways:
 * <p><ul>
 * <li>WampConnection allows symmetric communication between client and server. Both the server
 * and client can make RPC calls to each other and can subscribe to messages at either endpoint.
 * </li>
 * <li>The * wildcard can be used to subscribe to or unsubscribe from a set of topics.</li>
 * <li>WampConnection treats publish and event calls in the same way. Whether a publish or event
 * message is sent, depends on whether the connection is on the server or client side, regardless
 * of the method that is called.</li>
 * <li>WampConnection allows that multiple methods execute an RPC call and combines the
 * results of the methods into a single return.</li>
 * <li>WampConnection supports the specification of request parameters as URI query parameters.
 * </li>
 * <li>WampConnection supports user information specified in URI's.</li>
 * <li>WampConnection can map from one user's URI structure to another users's URI structure
 * allowing communication between assymetric clients.</li>
 * <li>WampConnection can forward requests to another server allowing a network of servers that
 * route message.</li>
 * </ul></p>
 *
 * WampConnection must be closed by calling the {@link #close()} method in order to properly
 * remove all Node handlers.
 *
 * WampConnection is thread-safe. The {@link #process(String)} method can be executed by a
 * thread pool.
 */
public class WampConnection extends Connection {

  // The URI port.
  private static final int kPort = -1;

  // The URI protocol.
  private static final String kProtocol = "wamp";

  // Length of session ID strings.
  private static final int kSessionIdLength = 16;

  // Server ID string sent in welcome messages.
  private static final String kServerId = "general.ai-Intercom/2014.01.15";

  // Required by protocol.
  private static final int kWampVersion = 1;

  // WAMP type ID's.
  private static final int kWelcome = 0;
  private static final int kPrefix = 1;
  private static final int kCall = 2;
  private static final int kCallResult = 3;
  private static final int kCallError = 4;
  private static final int kSubscribe = 5;
  private static final int kUnsubscribe = 6;
  private static final int kPublish = 7;
  private static final int kEvent = 8;

  /**
   * By default the WampConnection starts in client mode. To switch the connection to server
   * mode, the {@link #welcome()} method needs to be called.
   *
   * All URI's are interpreted with respect to the home path of the user account associated
   * with this connection. WampConnection does not allow access to resources that are not
   * reachable via the home path of the user account.
   *
   * If access to external resources is needed, the {@link Directory} class can be used to
   * create links from a node under the home path of the user to a resource outside the home path
   * to grant temporary access to the user. This allows a more scalable and secure authentication
   * architecture.
   *
   * Requests are always relativized with respect to the home path of a user account.
   * Requests from one user account to another user account (e.g., with a publish request) are
   * mapped from the source URI structure to the destination URI structure. This allows
   * communication between users without the need to use the same URI structures.
   *
   * @param uri The URI of the server endpoint associated with this connection.
   * @param user_account Account name associated with this connection or null if none.
   * @param home_path Home directory path for user account. Must be absolute.
   * @param sender OutputSender to be used to send output to the remote endpoint.
   */
  public WampConnection(Uri uri,
                        String user_account,
                        String home_path,
                        OutputSender sender) {
    super(uri, user_account, home_path);
    this.sender_ = sender;
    is_server_ = false;
    setSessionId("0");
    json_mapper_ = new ObjectMapper();
    object_array_type_ = json_mapper_.getTypeFactory().constructArrayType(Object.class);
    string_array_list_type_ =
      json_mapper_.getTypeFactory().constructCollectionType(ArrayList.class, String.class);
    client_subscribed_uris_ = new ArrayList<Uri>();
    server_subscribed_paths_ = new ArrayList<String>();
    pending_rpc_calls_ = new HashMap<String, RpcCallback>();
    rpc_call_counter_ = 0;
    prefix_ = new HashMap<String, String>();
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
  @Override
  public boolean call(String method_path, RpcCallback callback, Object ... arguments) {
    if (method_path.length() == 0) {
      return false;
    }
    if (method_path.charAt(0) != '/') {
      method_path = "/" + method_path;
    }
    try {
      return call(createUriFromPath(method_path), callback, arguments);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

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
  @Override
  public boolean call(Uri method_uri, RpcCallback callback, Object ... arguments) {
    String call_id;
    synchronized (pending_rpc_calls_) {
      rpc_call_counter_++;
      call_id = getSessionId() + ":" + rpc_call_counter_ + ":" + System.currentTimeMillis();
      pending_rpc_calls_.put(call_id, callback);
    }
    ArrayNode request = json_mapper_.createArrayNode();
    request.add(kCall);
    request.add(call_id);
    request.add(method_uri.toString());
    for (Object argument : arguments) {
      request.addPOJO(argument);
    }
    try {
      return sender_.sendText(json_mapper_.writeValueAsString(request));
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  /**
   * Resets the session ID. This may be necessary to reconnect to a server using the same
   * WampConnection instance.
   */
  public void clearSessionId() {
    setSessionId("0");
  }

  /**
   * This method must be called in order to properly clean up when the WampConnection is
   * closed.
   * Removes all handlers added by this instance.
   */
  @Override
  public void close() {
    super.close();
    unsubscribeAll();
  }

  /**
   * Sends an event message to the remote endpoint for the specified topic path with the provided
   * data.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side.
   *
   * This method generates the appropriate URI for the request based on information provided
   * during construction of this object.
   * The path must be absolute.
   *
   * The data object must be a POJO, e.g. a Java Bean object.
   *
   * @param topic_path The topic path to which to send the event message.
   * @param data The data associated with the event message. Must be a POJO.
   * @return True if the request was sent.
   */
  public boolean event(String topic_path, Object data) {
    return publish(topic_path, data, false, null, null);
  }

  /**
   * Sends an event message to the remote endpoint for the specified topic URI with the provided
   * data in JSON format.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side.
   *
   * @param topic_uri The topic URI to which to send the event message.
   * @param data The data associated with the event message.
   * @return True if the request was sent.
   */
  public boolean event(Uri topic_uri, Object data) {
    return publish(topic_uri, data, false, null, null);
  }

  /**
   * Whether this connection acts as a WAMP server or WAMP client.
   *
   * @return True if this connection uses the WAMP server protocol.
   */
  public boolean isServer() {
    return is_server_;
  }

  /**
   * Sends a prefix request with the specified prefix and URI.
   *
   * @param prefix The desired prefix for the URI.
   * @param uri The URI to be replaced by the prefix.
   * @return True if the message was sent.
   */
  public boolean prefix(String prefix, Uri uri) {
    ArrayNode request = json_mapper_.createArrayNode();
    request.add(kPrefix);
    request.add(prefix);
    request.add(uri.toString());
    try {
      return sender_.sendText(json_mapper_.writeValueAsString(request));
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  /**
   * Processes an incoming WAMP message. This method processes both server and client messages.
   * Any response is sent to the caller via the output sender.
   *
   * This method returns true if the input conforms to the protocol and could successfully be
   * interpreted. The method does return true even if the processing of the request has resulted
   * in a logic error as long as the input conforms to the protocol.
   *
   * @param input Message received from remote endpoint.
   * @return True if the input was successfully interpreted.
   */
  @Override
  public boolean process(String input) {
    if (input == null) {
      return false;
    }
    try {
      Object[] request = json_mapper_.readValue(input, object_array_type_);
      if (request.length < 1) {
        log.trace("invalid request");
        return false;
      }
      int type_id = (Integer) request[0];
      switch (type_id) {
        case kWelcome: return processWelcome(request);
        case kPrefix: return processPrefix(request);
        case kCall: return processCall(request);
        case kCallResult: return processCallResult(request);
        case kCallError: return processCallError(request);
        case kSubscribe: return processSubscribe(request);
        case kUnsubscribe: return processUnsubscribe(request);
        case kPublish: return processPublish(request);
        case kEvent: return processEvent(request);
        default: return false;
      }
    } catch (Exception e) {
      log.catching(Level.TRACE, e);
      return false;
    }
  }

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data. This method creates the appropriate URI.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @return True if the request was sent.
   */
  @Override
  public boolean publish(String topic_path, Object data) {
    return publish(topic_path, data, false, null, null);
  }

  /**
   * Sends a publish request to the remote endpoint for the specified topic URI with the provided
   * data.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side.
   *
   * Any additional publish parameters, such as exclude or eligible lists, must be specified
   * as URI query parameters. Note that URI parameters are not a standard WAMP feature.
   *
   * @param topic_uri The topic URI to publish to.
   * @param data The data to publish.
   * @return True if the request was sent.
   */
  @Override
  public boolean publish(Uri topic_uri, Object data) {
    return publish(topic_uri, data, false, null, null);
  }

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data.
   *
   * This method allows explicit specification of the exclude_me argument.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side. Note that
   * the exclude_me parameter is not supported on the server in the standard WAMP protocol.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @param exclude_me Value of exclude_me argument.
   * @return True if the request was sent.
   */
  @Override
  public boolean publish(String topic_path, Object data, boolean exclude_me) {
    return publish(topic_path, data, exclude_me, null, null);
  }

  /**
   * Sends a publish request to the remote endpoint for the specified topic path with the provided
   * data.
   *
   * This method allows explicit specification or exclude or eligible lists.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. This method
   * produces a WAMP event on the server side and a WAMP publish on the client side. Note that
   * the exclude and eligible parameters are not supported on the server in the standard WAMP
   * protocol.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @param exclude List of target session ID's to exclude from the publish request.
   * @param eligible Explicit list of target session ID's to include in the publish request.
   * @return True if the request was sent.
   */
  @Override
  public boolean publish(String topic_path, Object data, String[] exclude, String[] eligible) {
    return publish(topic_path, data, false, exclude, eligible);
  }

  /**
   * Sends a subscribe request to the remote endpoint for the specified topic path.
   * This method generates the appropriate URI for the request based on information provided
   * during construction of this object.
   * The path must be absolute.
   *
   * @param topic_path The topic path to subscribe to.
   * @return True if the request was sent.
   */
  @Override
  public boolean subscribe(String topic_path) {
    if (topic_path.length() == 0) {
      return false;
    }
    if (topic_path.charAt(0) != '/') {
      topic_path = "/" + topic_path;
    }
    try {
      return subscribe(createUriFromPath(topic_path));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Sends a subscribe request to the remote endpoint for the specified topic URI.
   *
   * @param topic_uri The topic URI to subscribe to.
   * @return True if the request was sent.
   */
  @Override
  public boolean subscribe(Uri topic_uri) {
    ArrayNode request = json_mapper_.createArrayNode();
    request.add(kSubscribe);
    request.add(topic_uri.toString());
    try {
      if (sender_.sendText(json_mapper_.writeValueAsString(request))) {
        synchronized (client_subscribed_uris_) {
          client_subscribed_uris_.add(topic_uri);
        }
        return true;
      } else {
        return false;
      }
    } catch (JsonProcessingException e) {
      // Exception will not be thrown due to construction.
      return false;
    }
  }

  /**
   * Sends an unsubscribe request to the remote endpoint for the specified topic path.
   * This method generates the appropriate URI for the request based on information provided
   * during construction of this object.
   * This method is the counterpart of {@link #subscribe(String)}.
   *
   * @param topic_path The topic path to unsubscribe from.
   * @return True if the request was sent.
   */
  @Override
  public boolean unsubscribe(String topic_path) {
    if (topic_path.length() == 0) {
      return false;
    }
    if (topic_path.charAt(0) != '/') {
      topic_path = "/" + topic_path;
    }
    try {
      return unsubscribe(createUriFromPath(topic_path));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Sends an unsubscribe request to the remote endpoint for the specified topic URI.
   *
   * @param topic_uri The topic URI to unsubscribe from.
   * @return True if the request was sent.
   */
  @Override
  public boolean unsubscribe(Uri topic_uri) {
    ArrayNode request = json_mapper_.createArrayNode();
    request.add(kUnsubscribe);
    request.add(topic_uri.toString());
    try {
      if (sender_.sendText(json_mapper_.writeValueAsString(request))) {
        synchronized (client_subscribed_uris_) {
          client_subscribed_uris_.remove(topic_uri);
        }
        return true;
      } else {
        return false;
      }
    } catch (JsonProcessingException e) {
      // Exception will not be thrown due to construction.
      return false;
    }
  }

  /**
   * Starts a new session with a WAMP client and sends a welcome message to the client using
   * a randomly generated session ID.
   *
   * This message must be sent by a WAMP server in order to initiate communication with a client.
   *
   * After this method is called the WampConnection will use the WAMP server protocol in future
   * messages.
   *
   * @return True if the message was sent.
   */
  public boolean welcome() {
    return welcome(RandomString.nextString(kSessionIdLength));
  }

  /**
   * Starts a new session with a WAMP client and sends a welcome message to the client using the
   * specified session ID.
   *
   * This message must be sent by a WAMP server in order to initiate communication with a client.
   *
   * After this method is called the WampConnection will use the WAMP server protocol in future
   * messages.
   *
   * @param session_id Unique Session ID.
   * @return True if the message was sent.
   */
  public boolean welcome(String session_id) {
    setSessionId(session_id);
    is_server_ = true;
    ArrayNode response = json_mapper_.createArrayNode();
    response.add(kWelcome);
    response.add(session_id);
    response.add(kWampVersion);
    response.add(kServerId);
    try {
      if (sender_.sendText(json_mapper_.writeValueAsString(response))) {
        setIsReady(true);
        log.trace("connected as server with session ID '{}'", getSessionId());
        return true;
      }
    } catch (JsonProcessingException e) { /* Exception will not be thrown due to construction. */ }
    return false;
  }

  /**
   * Creates a Uri object from the given URI string. Verifies that the URI string conforms to the
   * expected format and normalizes the URI string as necessary.
   *
   * This method also expands any WAMP CURIE to a full URI if an applicable CURIE prefix has
   * been defined via a previous prefix request.
   *
   * @param uri_string The unprocessed URI as a string.
   * @return The URI if the uri_string is valid or null if it is invalid.
   */
  private Uri createUri(String uri_string) {
    if (!prefix_.isEmpty()) {
      int index = uri_string.indexOf(':');
      if (index > 0) {
        String prefix_replacement = prefix_.get(uri_string.substring(0, index));
        if (prefix_replacement != null) {
          uri_string = prefix_replacement + uri_string.substring(index + 1);
        }
      }
    }
    try {
      return new Uri(new URI(uri_string).normalize());
    } catch (URISyntaxException e) {
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Creates a Uri object from a path. This method uses the protocol, user, hostname, and port
   * information from this connection object.
   *
   * @param path The path for which to create the URI object.
   * @return A full URI for the specified path using parameters from this connection.
   * @throws IllegalArgumentException If the URI cannot be created for the specified path.
   */
  private Uri createUriFromPath(String path) throws IllegalArgumentException {
    Uri uri = new Uri(kProtocol, getHostname(), path);
    uri.setPort(kPort);
    uri.setUser(getUserAccount());
    return uri;
  }

  /**
   * Creates a call error message that can be sent to the caller of an RPC method.
   *
   * If any error_details are provided, they are included in the message.
   * The call URI is modified by setting the error code as its fragment.
   *
   * @param uri The URI of the original RPC method call.
   * @param call_id The call ID supplied by the original caller.
   * @param error_code An error code that is added as a fragment to the error URI.
   * @param error_description The description of the error (mandatory).
   * @param error_detals Additional details about the error (optional). May be null.
   * @return The call error message to be returned to the caller.
   */
  private String makeCallError(Uri uri,
                               String call_id,
                               String error_code,
                               String error_description,
                               Object error_details) {
    try {
      uri.setFragment(error_code);
      ArrayNode response = json_mapper_.createArrayNode();
      response.add(kCallError);
      response.add(call_id);
      response.add(uri.toUri().toString());
      response.add(error_description);
      if (error_details != null) {
        response.addPOJO(error_details);
      }
      return json_mapper_.writeValueAsString(response);
    } catch (JsonProcessingException e) {  // handled below
      // Revert to manual message generation if there is a secondary exception during exception
      // handling.
      return "[" + kCallError + ", " + call_id +
        ", \"wamp://" + getHostname() + "/error#runtime_error\", \"runtime error\"]";
    }
  }

  /**
   * Creates a call result message that can be sent to the caller of an RPC method.
   *
   * If the result array has only one item, that item will be returned as the RPC result.
   * If the result array has multiple items, the entire array is returned as a JSON array.
   * If the result array has no items, null is returned.
   * If there is an error while generating the call result, a call error will be returned.
   *
   * @param uri The URI of the original RPC method call.
   * @param call_id The call ID supplied by the original caller.
   * @param result The RPC method results.
   * @return The call result message to be returned to the caller.
   */
  private String makeCallResult(Uri uri, String call_id, Collection<Object> result) {
    try {
      ArrayNode response = json_mapper_.createArrayNode();
      response.add(kCallResult);
      response.add(call_id);
      switch (result.size()) {
        case 0: response.addNull(); break;
        case 1: response.addPOJO(result.iterator().next()); break;
        default: response.addPOJO(result); break;
      }
      return json_mapper_.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      return makeCallError(uri, call_id, "runtime_error", "runtime error", null);
    }
  }

  /**
   * Processes an incoming call request.
   *
   * Once the call has completed, sends the call result or call error message via the output
   * sender.
   *
   * The method handler is run synchronously on the calling thread. This method does not return
   * until the method handler returns.
   *
   * wamp_request[1] = call ID
   * wamp_request[2] = method URI
   * wamp_request[3..] = arguments
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully processed.
   * @throws ClassCastException if the request arguments are invalid.
   */
  private boolean processCall(Object[] wamp_request) {
    final int kIndexCallId = 1;
    final int kIndexMethodUri = 2;

    if (wamp_request.length < 3) {
      // Protocol violation, do not respond.
      log.trace("invalid call request");
      return false;
    }
    String call_id = (String) wamp_request[kIndexCallId];
    Uri uri = createUri((String) wamp_request[kIndexMethodUri]);
    if (uri == null) {
      log.trace("invalid method uri: {}", wamp_request[kIndexMethodUri]);
      return sender_.sendText(makeCallError(createUriFromPath("/error"),
                                            call_id,
                                            "rpc_error",
                                            "undefined method",
                                            null));
    }
    Request request = new Request(uri, Request.RequestType.Call);
    for (int i = 3; i < wamp_request.length; i++) {
      request.addArgument(wamp_request[i]);
    }
    if (Directory.Instance.handle(getHomePath(), request) > 0) {
      Result result = request.getResult();
      if (!result.hasErrors()) {
        sender_.sendText(makeCallResult(uri, call_id, result.getValues()));
        log.trace("processed RPC call with success: '{}'", wamp_request[kIndexMethodUri]);
      } else {
        // WAMP supports returning only one error. Thus, only the first error is returned to the
        // caller.
        Result.Error error = result.getError(0);
        sender_.sendText(makeCallError(uri,
                                       call_id,
                                       "logic_error",
                                       error.getDescription(),
                                       error.getDetails()));
        log.trace("processed RPC call with error: '{}'", wamp_request[kIndexMethodUri]);
      }
    } else {
      sender_.sendText(makeCallError(uri, call_id, "rpc_error", "undefined method", null));
      log.trace("call to undefined method: '{}'", wamp_request[kIndexMethodUri]);
    }
    return true;
  }

  /**
   * Processes an incoming call error.
   *
   * The call error callback is called on the calling thread.
   *
   * wamp_request[1] = call ID
   * wamp_request[2] = error URI
   * wamp_request[3] = error description
   * wamp_request[4] = error details (optional)
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully processed.
   * @throws ClassCastException if the request arguments are invalid.
   */
  private boolean processCallError(Object[] wamp_request) {
    final int kIndexCallId = 1;
    final int kIndexErrorUri = 2;
    final int kIndexErrorDescription = 3;
    final int kIndexErrorDetails = 4;

    if (wamp_request.length < 4) {
      log.trace("invalid call error request");
      return false;
    }
    String call_id = (String) wamp_request[kIndexCallId];
    Object error_details = null;
    if (wamp_request.length > 4) {
      error_details = wamp_request[kIndexErrorDetails];
    }
    RpcCallback callback = pending_rpc_calls_.get(call_id);
    if (callback == null) {
      log.trace("call error with no callback");
      return true;
    }
    synchronized (pending_rpc_calls_) {
      pending_rpc_calls_.remove(call_id);
    }
    try {
      callback.onError(new Uri((String) wamp_request[kIndexErrorUri]),
                       (String) wamp_request[kIndexErrorDescription],
                       error_details);
    } catch (IllegalArgumentException e) {
      // On URI error make the callback without the URI.
      callback.onError(null, (String) wamp_request[kIndexErrorDescription], error_details);
    }
    log.trace("processed call error");
    return true;
  }

  /**
   * Processes an incoming call result.
   *
   * The call result is processed on the calling thread.
   *
   * wamp_request[1] = call ID
   * wamp_request[2] = call result
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully processed.
   * @throws ClassCastException if the request arguments are invalid.
   */
  private boolean processCallResult(Object[] wamp_request) {
    final int kIndexCallId = 1;
    final int kIndexCallResult = 2;

    if (wamp_request.length < 3) {
      log.trace("invalid call result request");
      return false;
    }
    String call_id = (String) wamp_request[kIndexCallId];
    RpcCallback callback = pending_rpc_calls_.get(call_id);
    if (callback == null) {
      log.trace("call result with no callback");
      return true;
    }
    synchronized (pending_rpc_calls_) {
      pending_rpc_calls_.remove(call_id);
    }
    callback.onSuccess(wamp_request[kIndexCallResult]);
    log.trace("processed call result");
    return true;
  }

  /**
   * Processes an incoming event request.
   *
   * This method allows events for unsubscribed URI's. This is necessary since a subscription
   * requests may contain wildcards.
   *
   * wamp_request[1] = topic URI
   * wamp_request[2] = event data
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully processed.
   * @throws ClassCastException if the request arguments are invalid.
   */
  private boolean processEvent(Object[] wamp_request) {
    final int kIndexTopicUri = 1;
    final int kIndexEventData = 2;

    if (wamp_request.length < 3) {
      log.trace("invalid event request");
      return false;
    }
    Uri uri = createUri((String) wamp_request[kIndexTopicUri]);
    if (uri == null) {
      log.trace("invalid topic uri: {}", wamp_request[kIndexTopicUri]);
      return false;
    }
    Request request = new Request(uri, Request.RequestType.Publish, wamp_request[kIndexEventData]);
    Directory.Instance.handle(getHomePath(), request);
    log.trace("processed event '{}'", wamp_request[kIndexTopicUri]);
    return true;
  }

  /**
   * Processes an incoming prefix request.
   *
   * wamp_request[1] = prefix
   * wamp_request[2] = URI to be prefixed
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully procesed.
   * @throws ClassCastException if request arguments are invalid.
   */
  private boolean processPrefix(Object[] wamp_request) {
    final int kIndexPrefix = 1;
    final int kIndexUri = 2;

    if (wamp_request.length < 3) {
      log.trace("invalid prefix request");
      return false;
    }
    synchronized (prefix_) {
      prefix_.put((String) wamp_request[kIndexPrefix], (String) wamp_request[kIndexUri]);
    }
    log.trace("processed prefix '{}' -> '{}'",
              wamp_request[kIndexPrefix], wamp_request[kIndexUri]);
    return true;
  }

  /**
   * Processes an incoming publish request.
   *
   * wamp_request[1] = topic URI
   * wamp_request[2] = event data
   * wamp_request[3] = exclude_me or exclude list (optional)
   * wamp_request[4] = eligible list (optional)
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully procesed.
   * @throws ClassCastException if request arguments are invalid.
   */
  private boolean processPublish(Object[] wamp_request) {
    final int kIndexTopicUri = 1;
    final int kIndexEventData = 2;
    final int kIndexExclude = 3;
    final int kIndexElligible = 4;

    if (wamp_request.length < 3) {
      log.trace("invalid publish request");
      return false;
    }
    Uri uri = createUri((String) wamp_request[kIndexTopicUri]);
    if (uri == null) {
      log.trace("invalid topic uri: {}", wamp_request[kIndexTopicUri]);
      return false;
    }
    Request request = new Request(uri, Request.RequestType.Publish, wamp_request[kIndexEventData]);
    if (wamp_request.length > kIndexExclude) {
      if (wamp_request[kIndexExclude] instanceof Boolean) {
        uri.setParameter("exclude", getSessionId());
      } else if (wamp_request[kIndexExclude] instanceof ArrayList) {
        ArrayList<String> exclude =
          json_mapper_.convertValue(wamp_request[kIndexExclude], string_array_list_type_);
        if (exclude.size() > 0) {
          uri.setParameter("exclude", Strings.join(exclude, ","));
        }
      }
      if (wamp_request.length > kIndexElligible) {
        if (wamp_request[kIndexElligible] instanceof ArrayList) {
          ArrayList<String> eligible =
            json_mapper_.convertValue(wamp_request[kIndexElligible], string_array_list_type_);
          if (eligible.size() > 0) {
            uri.setParameter("eligible", Strings.join(eligible, ","));
          }
        }
      }
    }
    Directory.Instance.handle(getHomePath(), request);
    log.trace("processed publish '{}'", wamp_request[kIndexTopicUri]);
    return true;
  }

  /**
   * Processes an incoming subscribe request.
   *
   * wamp_request[1] = topic URI
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully procesed.
   * @throws ClassCastException if request arguments are invalid.
   */
  private boolean processSubscribe(Object[] wamp_request) {
    final int kIndexTopicUri = 1;

    if (wamp_request.length < 2) {
      log.trace("invalid subscribe request");
      return false;
    }
    Uri uri = createUri((String) wamp_request[kIndexTopicUri]);
    if (uri == null) {
      log.trace("invalid topic uri: {}", wamp_request[kIndexTopicUri]);
      return false;
    }
    String path = getHomePath() + uri.getPath();

    synchronized (server_subscribed_paths_) {
      if (!server_subscribed_paths_.contains(path)) {
        Handler handler = new RelayHandler(relayHandlerName(path), this, uri);
        if (Directory.Instance.addHandler(path, handler)) {
          server_subscribed_paths_.add(path);
        }
      }
    }
    log.trace("Processed subscribe '{}'", path);
    return true;
  }

  /**
   * Processes an incoming unsubscribe request.
   *
   * wamp_request[1] = topic URI
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully procesed.
   * @throws ClassCastException if request arguments are invalid.
   */
  private boolean processUnsubscribe(Object[] wamp_request) {
    final int kIndexTopicUri = 1;

    if (wamp_request.length < 2) {
      log.trace("invalid unsubscribe request");
      return false;
    }
    Uri uri = createUri((String) wamp_request[kIndexTopicUri]);
    if (uri == null) {
      log.trace("invalid topic uri: {}", wamp_request[kIndexTopicUri]);
      return false;
    }
    String path = getHomePath() + uri.getPath();
    Directory.Instance.removeHandler(path, relayHandlerName(path));
    synchronized (server_subscribed_paths_) {
      server_subscribed_paths_.remove(path);
    }
    log.trace("Processed unsubscribe '{}'", path);
    return true;
  }

  /**
   * Processes an incoming welcome request.
   *
   * wamp_request[1] = session ID
   * wamp_request[2] = protocol version
   * wamp_request[3] = server ID
   *
   * @param wamp_request The WAMP request arguments.
   * @return True if the request has been successfully procesed.
   * @throws ClassCastException if request arguments are invalid.
   */
  private boolean processWelcome(Object[] wamp_request) {
    final int kIndexSessionId = 1;
    //  final int kProtocolVersion = 2;  // ignored
    final int kIndexServerId = 3;

    if (wamp_request.length < 4) {
      log.trace("invalid welcome request");
      return false;
    }
    setSessionId((String) wamp_request[kIndexSessionId]);
    setServerId((String) wamp_request[kIndexServerId]);
    setIsReady(true);
    log.trace("received server welcome from {} for session {}", getServerId(), getSessionId());
    return true;
  }

  /**
   * Internal unified publish method that accepts a string topic path.
   *
   * See {@link #publish(int, URI, Object, boolean, String[], String[])} for detailed comments.
   *
   * @param topic_path The topic URI path to publish to.
   * @param data The data to publish.
   * @param exclude_me Optional value of exclude_me argument (only included if true).
   * @param exclude List of session ID's to exclude from the publish request or null.
   * @param eligible Explicit list of session ID's to include in the publish request or null.
   * @return True if the request was sent.
   */
  private boolean publish(String topic_path,
                          Object data,
                          boolean exclude_me,
                          String[] exclude,
                          String[] eligible) {
    if (topic_path.length() == 0) {
      return false;
    }
    if (topic_path.charAt(0) != '/') {
      topic_path = "/" + topic_path;
    }
    try {
      return publish(createUriFromPath(topic_path), data, exclude_me, exclude, eligible);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Internal unified publish method that accepts a URI topic path.
   *
   * The WAMP protocol distinguishes between a publish from a client and a server. Server messages
   * are published as WAMP events and client messages as WAMP publish messages. This method
   * combines the two mechanism by using the right WAMP type ID depending on connection context.
   *
   * This method adds the optional exclude_me, exclude and eligible parameters to the message.
   * The exclude_me parameter is only added if it is true. The exclude parameter is only added
   * if it is not null and exclude_me is false. The eligible parameter is only added if it is
   * not not. If the eligible parameter is specified, but both exclude_me is false and exclude
   * is null, this method adds an empty exclude array to the message to conform to the protocol.
   *
   * While this method does not strictly enforce it, the WAMP protocol does not support the
   * exclude_me, exclude and eligible parameters on the server side. So, on the server side
   * these values should be always set to false and null.
   *
   * @param topic_uri The topic URI to publish to.
   * @param data The data to publish.
   * @param exclude_me Optional value of exclude_me argument (only included if true).
   * @param exclude List of session ID's to exclude from the publish request or null.
   * @param eligible Explicit list of session ID's to include in the publish request or null.
   * @return True if the request was sent.
   */
  private boolean publish(Uri topic_uri,
                          Object data,
                          boolean exclude_me,
                          String[] exclude,
                          String[] eligible) {
    ArrayNode request = json_mapper_.createArrayNode();
    request.add(is_server_ ? kEvent : kPublish);
    request.add(topic_uri.toString());
    request.addPOJO(data);
    if (exclude_me) {
      request.add(exclude_me);
    } else if (exclude != null) {
      request.addPOJO(exclude);
    }
    if (eligible != null) {
      if (request.size() < 4) {
        request.addPOJO(new String[] {});
      }
      request.addPOJO(eligible);
    }
    try {
      return sender_.sendText(json_mapper_.writeValueAsString(request));
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  /**
   * Creates and returns a unique name for the relay handler for the specified path.
   *
   * @return A unique name for the relay handler for the specified path.
   */
  private String relayHandlerName(String path) {
    if (getUserAccount() == null) {
      return path + "->@" + getSessionId();
    } else {
      return path + "->" + getUserAccount() + "@" + getSessionId();
    }
  }

  /**
   * Executes both client and server unsubscription.
   * As client, unsubscribes from all subscribed topics.
   * As server, unsubscribes all topics subscribed to by clients.
   */
  private void unsubscribeAll() {
    // client unsubscription
    synchronized (client_subscribed_uris_) {
      Uri[] topic_uris = client_subscribed_uris_.toArray(new Uri[] {});
      for (Uri uri : topic_uris) {
        unsubscribe(uri);
      }
      client_subscribed_uris_.clear();
    }
    // server unsubscription
    synchronized (server_subscribed_paths_) {
      for (String path : server_subscribed_paths_) {
        Directory.Instance.removeHandler(path, relayHandlerName(path));
      }
      server_subscribed_paths_.clear();
    }
  }

  private static Logger log = LogManager.getLogger();

  private ArrayList<Uri> client_subscribed_uris_;  // All URI's subscribed to as client.
  private boolean is_server_;  // If true, use server protocol.
  private ObjectMapper json_mapper_;  // JSON parser and generator.
  private JavaType object_array_type_;  // Object[] type used in JSON parsing.
  private HashMap<String, RpcCallback> pending_rpc_calls_;  // RPC calls in progress.
  private HashMap<String, String> prefix_;  // WAMP prefix directory.
  private long rpc_call_counter_;  // Counter used to keep track of RPC calls.
  private OutputSender sender_;  // Used to send messages to the remote endpoint.
  private ArrayList<String> server_subscribed_paths_;  // All paths subscribed to by clients.
  private JavaType string_array_list_type_;  // ArrayList<String> type used in JSON parsing.
}
