/* General AI - Directory
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory;

import ai.general.net.Uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents a request directed at a resource represented by a directory node.
 *
 * Requests have a request type and are associated with a set of request arguments. The arguments
 * are processed by request handlers. The result of the processing can be accessed via the result
 * property.
 *
 * A Request is directed to a resource identified by a URI and represented by a Directory node.
 * Request handlers registered with the target node and any catch-all handlers along the path
 * to the Directory node process the request.
 *
 * The request URI may specify query parameters which may be used by the request handlers.
 */
public class Request {

  /** Name of type parameter in URI. */
  public static final String kRequestType = "type";

  /** Type parameter for call requests. */
  public static final String kRequestTypeCall = "call";

  /** Type paramter value for publish requests. */
  public static final String kRequestTypePublish = "publish";

  /**
   * Specifies the type of action requested.
   */
  public enum RequestType {
    /** Unknown or unspecified action. */
    Unspecified,

    /** Execute a method and return the result. Caller expects return value. */
    Call,

    /** Publish data. This request type is fire and forget. No value is returned. */
    Publish,
  }

  /**
   * The URI must at least specify the resource path and request type, but may also specify user
   * information.
   * Specifically, the URI has the format:
   * {@literal [<protocol>://<user>@<server>/]<resource path> }
   * where protocol, user and server are optional.
   *
   * This constructor extracts the requests type from the URI.
   *
   * Publish requests must specify one argument. Call requests may specify any number of arguments.
   *
   * Arguments can be also added using the {@link #addArgument(Object)} and
   * {@link #addArguments(Object...)} methods.
   *
   * @param uri The URI of the target.
   * @param arguments Request arguments.
   */
  public Request(Uri uri, Object ... arguments) {
    this(uri, RequestType.Unspecified, arguments);
    if (uri.hasParameter(kRequestType)) {
      switch (uri_.getParameter(kRequestType)) {
        case kRequestTypePublish: request_type_ = RequestType.Publish; break;
        case kRequestTypeCall: request_type_ = RequestType.Call; break;
      }
    }
  }

  /**
   * The URI must at least specify the resource path, but may also specify user information.
   * Specifically, the URI has the format:
   * {@literal [<protocol>://<user>@<server>/]<resource path> }
   * where protocol, user and server are optional.
   *
   * Publish requests must specify one argument. Call requests may specify any number of arguments.
   *
   * Arguments can be also added using the {@link #addArgument(Object)} and
   * {@link #addArguments(Object...)} methods.
   *
   * @param uri The URI of the target resource.
   * @param request_type The request type.
   * @param arguments Request arguments.
   */
  public Request(Uri uri, RequestType request_type, Object ... arguments) {
    this.uri_ = uri;
    this.request_type_ = request_type;
    arguments_ = new ArrayList<Object>();
    Collections.addAll(this.arguments_, arguments);
    result_ = new Result();
  }

  /**
   * Adds a single argument to the request.
   *
   * @param argument The argument to add to the request.
   */
  public void addArgument(Object argument) {
    arguments_.add(argument);
  }

  /**
   * Adds a set of arguments to the request. Each argument in the array is added as a separate
   * argument to the request.
   *
   * @param arguments Set of arguments to add to the request.
   */
  public void addArguments(Object ... arguments) {
    Collections.addAll(arguments_, arguments);
  }

  /**
   * Returns the request argument at specified index.
   * Returns null if there is no request argument atthe specified index.
   *
   * @param index The request argument index.
   * @return The request argument at the specified index or null.
   */
  public Object getArgument(int index) {
    if (arguments_.size() <= index) {
      return null;
    }
    return arguments_.get(index);
  }

  /**
   * Returns the request arguments.
   *
   * @return Arguments associated with the request.
   */
  public Collection<Object> getArguments() {
    return arguments_;
  }

  /**
   * Returns the request type.
   *
   * @return The type of request.
   */
  public RequestType getRequestType() {
    return request_type_;
  }

  /**
   * Returns the result of processing the request.
   * The result object is populated as the request is processed by the request handlers.
   *
   * @return The result of processing the request.
   */
  public Result getResult() {
    return result_;
  }

  /**
   * Returns the resource URI.
   *
   * @return The URI of the resource targeted by this request.
   */
  public Uri getUri() {
    return uri_;
  }

  /**
   * Creates a URI query parameter that specifies the request type.
   *
   * @return A query parameter for the specified request type or empty string for Unspecified.
   */
  public static String makeRequestTypeParameter(RequestType request_type) {
    switch (request_type) {
      case Publish: return kRequestType + "=" + kRequestTypePublish;
      case Call: return kRequestType + "=" + kRequestTypeCall;
      default: return "";
    }
  }

  /**
   * Returns the number of request arguments.
   *
   * @return The number of request arguments.
   */
  public int numArguments() {
    return arguments_.size();
  }

  /**
   * Allows explicitly specifying the request type.
   *
   * @param type Updated request type.
   */
  public void setRequestType(RequestType type) {
    this.request_type_ = type;
  }

  private ArrayList<Object> arguments_;  // Request arguments.
  private RequestType request_type_;  // Request type.
  private Result result_;  // The result of processing the request.
  private Uri uri_;  // Resource URI.
}
