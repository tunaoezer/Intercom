/* General AI - Networking
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import ai.general.directory.Handler;
import ai.general.directory.Request;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Relays requests to a remote endpoint.
 * RelayHandler only relays publish requests. Call requests are not relayed.
 *
 * Request URI's are modified to use the relay path rather than the original request path.
 * This allows mapping from the caller URI structure to the receiver URI structure allowing
 * communication between assymetric clients.
 *
 * RelayHandler includes the path remainder in relayed catchall requests providing the receiver
 * with the full context of the request.
 */
public class RelayHandler extends Handler {

  /**
   * If the relay path ends with a '/*', this handler will be configured as a catch all handler.
   * Relayed URI's do not include the wildcard, but include the full path of the target node
   * with respect to the relay_uri.
   *
   * @param name A unique name for this handler.
   * @param connection Connection through which to relay requests.
   * @param relay_uri The URI that will be used in relayed messaged.
   */
  public RelayHandler(String name, Connection connection, Uri relay_uri) {
    super(name, relay_uri.getPath().endsWith("/*"));
    this.connection_ = connection;
    if (isCatchAll()) {
      // remove the final '*';
      this.relay_uri_ =
        changePath(relay_uri, relay_uri.getPath().substring(0, relay_uri.getPath().length() - 1));
    } else {
      this.relay_uri_ = relay_uri;
    }
  }

  /**
   * Relays the request to the remote endpoint. This method uses the relay URI specified in
   * the constructor.
   *
   * @param request The request to be relayed.
   */
  @Override
  public void handle(Request request) {
    handle(relay_uri_, request);
  }

  /**
   * In case of a catch-all request, includes the path_remainder in the relay URI.
   *
   * @param path_remainder The relative path from the calling node to the target of the request.
   * @param request The request to handle.
   */
  public void handleCatchAll(String path_remainder, Request request) {
    handle(changePath(relay_uri_, relay_uri_.getPath() + path_remainder), request);
  }

  /**
   * Internal handle implementation. Allows specification of the relay URI.
   * The relayed path may be expanded if the handler is called in a catch-all request.
   *
   * @param relay_uri The URI that will be used in relayed messaged.
   * @param request The request to be relayed.
   */
  private void handle(Uri relay_uri, Request request) {
    if (request.getRequestType() != Request.RequestType.Publish ||
        request.getArguments().size() != 1) {
      return;
    }
    Uri request_uri = request.getUri();
    if (request_uri.hasParameter("eligible")) {
      String eligible = request_uri.getParameter("eligible");
      if (!(eligible.equals(connection_.getSessionId()) ||
            eligible.startsWith(connection_.getSessionId() + ",") ||
            eligible.indexOf("," + connection_.getSessionId()) > 0)) {
        log.trace("not eligible: {}", relay_uri.toString());
        return;
      }
    }
    if (request_uri.hasParameter("exclude")) {
      String exclude = request_uri.getParameter("exclude");
      if (exclude.equals(connection_.getSessionId()) ||
          exclude.startsWith(connection_.getSessionId() + ",") ||
          exclude.indexOf("," + connection_.getSessionId()) > 0) {
        log.trace("exclude relay: {}", relay_uri.toString());
        return;
      }
    }
    log.trace("relay: {}", relay_uri.toString());
    connection_.publish(relay_uri, request.getArgument(0));
  }

  /**
   * Changes the path of the URI and returns a new URI with the updated path.
   * Returns the input URI if the new path is malformed.
   *
   * @param uri URI to change.
   * @param new_path The updated path.
   * @return The updated URI.
   */
  private Uri changePath(Uri uri, String new_path) {
    Uri new_uri = new Uri(uri);
    try {
      new_uri.setPath(new_path);
      return new_uri;
    } catch (IllegalArgumentException e) {
      return uri;
    }
  }

  private static Logger log = LogManager.getLogger();

  private Connection connection_;  // The connection to use to relay messages.
  private Uri relay_uri_;  // The outgoing URI to use in relayed messages.
}
