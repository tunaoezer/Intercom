/* General AI - Networking
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a URI.
 *
 * Unlike the built-in Java URI class, the Uri class allows modification of URI components.
 *
 * A Uri consists of at least a path.
 * It is typically also associated with a protocol and a server.
 * The Uri can be fruther extended with a set of query parameters, a network port, a fragment and
 * user information.
 */
public class Uri {

  /**
   * Constructs a Uri using the specified URI string.
   *
   * The URI string must include at least a path.
   * If a protocol and server domain name or IP address are specified, the path must be absolute.
   * The URI string may also include a port, a user, query parameters, and a fragment.
   *
   * The general syntax of a URI is:
   * protocol://user@server:port/path?query#fragment
   *
   * If the URI is invalid, an IllegalArgumentException is thrown.
   *
   * @param uri_string The URI string.
   * @throws IllegalArgumentException If the URI string is invalid.
   */
  public Uri(String uri_string) throws IllegalArgumentException {
    this(parseUriString(uri_string));
  }

  /**
   * Copy constructor.
   *
   * @param uri The Uri to copy.
   */
  public Uri(Uri uri) {
    this.protocol_ = uri.protocol_;
    this.server_ = uri.server_;
    this.port_ = uri.port_;
    this.path_ = uri.path_;
    this.user_ = uri.user_;
    this.parameters_ = new HashMap<String, String>(uri.parameters_);
    this.fragment_ = uri.fragment_;
  }

  /**
   * Construct a Uri from a Java URI.
   *
   * The URI must include at least a path.
   * If a protocol and server domain name or IP address are specified, the path must be absolute.
   * The URI may also include a port, a user, query parameters, and a fragment.
   *
   * @param uri The Java URI to convert to a Uri.
   */
  public Uri(URI uri) {
    protocol_ = uri.getScheme();
    if (protocol_ == null) {
      protocol_ = "";
    }
    server_ = uri.getHost();
    if (server_ == null) {
      server_ = "";
    }
    path_ = uri.getPath();
    if (path_ == null) {
      path_ = "";
    }
    port_ = uri.getPort();
    user_ = uri.getUserInfo();
    if (user_ == null) {
      user_ = "";
    }
    parameters_ = new HashMap<String, String>();
    if (uri.getQuery() != null && uri.getQuery().length() > 0) {
      String[] query = uri.getQuery().split("&");
      for (String param : query) {
        String[] name_value = param.split("=");
        if (name_value.length > 0 && name_value[0].length() > 0) {
          if (name_value.length > 1) {
            parameters_.put(name_value[0], name_value[1]);
          } else {
            parameters_.put(name_value[0], "");
          }
        }
      }
    }
    fragment_ = uri.getFragment();
    if (fragment_ == null) {
      fragment_ = "";
    }
  }

  /**
   * Creates a Uri with the specified protocol, server and resource path.
   * All parametes are required.
   *
   * The resource path must be an absolute path starting with a '/'.
   *
   * Query parameters, a non-default network port, user information and a fragement can be
   * optionally added after URI creation.
   *
   * @param protocol The network protocol name.
   * @param server The server hostname or IP address.
   * @param path Path to the resource.
   * @throws IllegalArgumentException if one of the arguments is invalid.
   */
  public Uri(String protocol, String server, String path)
    throws IllegalArgumentException {
    if (protocol == null || server == null || path == null) {
      new IllegalArgumentException("All arguments are required.");
    }
    if ((protocol.length() > 0 && server.length() == 0) ||
        (protocol.length() == 0 && server.length() > 0)) {
      new IllegalArgumentException("Protocol and server must be both empty or non-empty.");
    }
    this.protocol_ = protocol;
    int index = server.indexOf(':');
    port_ = -1;
    if (index < 0) {
      this.server_ = server;
    } else {
      this.server_ = server.substring(0, index);
      try {
        port_ = Integer.valueOf(server.substring(index + 1));
      } catch (NumberFormatException e) {}
    }
    setPath(path);
    user_ = "";
    parameters_ = new HashMap<String, String>();
    fragment_ = "";
  }

  /**
   * Returns the fragment or empty string if there is no fragment.
   *
   * @return Fragment or empty string.
   */
  public String getFragment() {
    return fragment_;
  }

  /**
   * Returns the value of a URI parameter if it exists. If the parameter does not exist,
   * returns null.
   *
   * @param name The name of the parameter.
   * @return The value of the parameter or null.
   */
  public String getParameter(String name) {
    return parameters_.get(name);
  }

  /**
   * Returns the path to the resource.
   *
   * @return The resource path.
   */
  public String getPath() {
    return path_;
  }

  /**
   * Returns the network port.
   * Returns -1 if the default port for the protocol is used.
   *
   * @return The port.
   */
  public int getPort() {
    return port_;
  }

  /**
   * Returns the network protocol. Returns the empty string if there is no protocol specified.
   *
   * @return The network protocol or empty string.
   */
  public String getProtocol() {
    return protocol_;
  }

  /**
   * Returns the server hostname or IP address. Returns the empty string if there is no
   * server specified.
   *
   * @return The server hostname or IP address or empty string.
   */
  public String getServer() {
    return server_;
  }

  /**
   * Returns the user information or empty string if there is no user information.
   *
   * @return User information or empty string.
   */
  public String getUser() {
    return user_;
  }

  /**
   * Returns true if this URI has a query parameter with the specified name.
   *
   * @param name The name of the parameter.
   * @return True if this URI has a query parameter with the specified name.
   */
  public boolean hasParameter(String name) {
    return parameters_.containsKey(name);
  }

  /**
   * Removes a URI parameter if it exists.
   *
   * @param name The name of the parameter to remove.
   */
  public void removeParameter(String name) {
    parameters_.remove(name);
  }

  /**
   * Sets the fragment.
   *
   * @param fragment The fragment.
   */
  public void setFragment(String fragment) {
    this.fragment_ = fragment != null ? fragment : "";
  }

  /**
   * Sets a URI parameter.
   * If the parameter does not already exist, it is added.
   * If the parameter already exists, its value is overriden.
   *
   * @param name The name of the parameter.
   * @param value The value of the parameter (may be empty).
   */
  public void setParameter(String name, String value) {
    if (value == null) {
      value = "";
    }
    parameters_.put(name, value);
  }

  /**
   * Sets the path to the resource.
   * If the URI has a protocol and server, the path must be absolute.
   *
   * @param path The resource path.
   */
  public void setPath(String path) {
    if (path == null) {
      path = "";
    }
    if (server_.length() > 0 && (path.length() == 0 || path.charAt(0) != '/')) {
      path = "/" + path;
    }
    this.path_ = path;
  }

  /**
   * Sets the network port.
   * If port is set to -1, the default port for the protocol will be used.
   *
   * @param port The port.
   */
  public void setPort(int port) {
    this.port_ = port;
  }

  /**
   * Sets the user.
   *
   * @param user The user information.
   */
  public void setUser(String user) {
    this.user_ = user != null ? user : "";
  }

  /**
   * Returns a string representation of the URI.
   *
   * @return A string representation of the URI.
   */
  @Override
  public String toString() {
    URI uri = toUri();
    if (uri == null) {
      return "";
    }
    return uri.toString();
  }

  /**
   * Returns the Uri as a Java URI object. This method computes the URI using the current URI
   * parameters.
   *
   * Returns null, if the URI cannot be created.
   *
   * @return A URI equivalent to this Uri or null.
   */
  public URI toUri() {
    try {
      return new URI(protocol_.length() > 0 ? protocol_ : null,
                     user_.length() > 0 ? user_ : null,
                     server_.length() > 0 ? server_ : null,
                     port_,
                     path_,
                     getQueryString(),
                     fragment_.length() > 0 ? fragment_ : null);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Parses a URI string and returns it as a Java URI.
   *
   * @param The URI string to parse.
   * @throws IllegalArgumentException if the uri_string cannot be parsed.
   */
  private static URI parseUriString(String uri_string) throws IllegalArgumentException {
    try {
      return new URI(uri_string);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Returns the URI query string based on the current parameters.
   * If no parameters are specified, returns null.
   *
   * @return The URI query string or null.
   */
  private String getQueryString() {
    StringBuilder query_string = new StringBuilder();
    for (Map.Entry<String, String> entry : parameters_.entrySet()) {
      if (query_string.length() > 0) {
        query_string.append('&');
      }
      query_string.append(entry.getKey());
      if (entry.getValue().length() > 0) {
        query_string.append('=');
        query_string.append(entry.getValue());
      }
    }
    return query_string.length() > 0 ? query_string.toString() : null;
  }

  private String fragment_;  // Optional fragment string.
  private HashMap<String, String> parameters_;  // Query parameters.
  private int port_;  // The network port. -1 if default port is used.
  private String protocol_;  // The network protocol name.
  private String path_;  // Path to the resource.
  private String server_;  // The server hostname or IP address.
  private String user_;  // Optional user information.
}
