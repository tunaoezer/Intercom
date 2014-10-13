/* General AI - Plugin Support
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.directory.Directory;
import ai.general.directory.Handler;
import ai.general.directory.Request;
import ai.general.net.Connection;
import ai.general.net.MethodHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Defines a service.
 *
 * A Service is a class that declares one or more service methods either with the
 * {@link ai.general.plugin.annotation.RpcMethod} annotation or
 * {@link ai.general.plugin.annotation.Subscribe} annotation. These service methods handle
 * client requests identified by {@link Directory} paths.
 *
 * Each ServiceDefinition is associated with a service instance. The ServiceDefinition class
 * contains meta-information about service instances and is used to manage the life-cycle of the
 * service instance.
 *
 * A service is associated with a home path. All service paths are interpreted with respect to
 * the service home path. This allows instantiating the same service class for different directory
 * paths. For example, a user service can be associated with the home path of the user account
 * allowing to instantiate the service for each user.
 *
 * If the service shall apply to all directory paths, the service home path can be set to the root
 * of the directory.
 *
 * Service definitions are automatically created and maintained by the {@link ServiceManager}.
 */
public class ServiceDefinition {

  /**
   * Represents a service handler definition. A Service handler definition consists of the handler,
   * the path to the directory node with which the handler is associated, and information about
   * the requests handled by the handler.
   */
  private class ServiceHandlerDefinition {

    /**
     * Creates a ServiceHandlerDefinition for the specified request path, request type and handler.
     *
     * @param request_path The request path handled by the handler.
     * @param request_type The type of request handled by the service handler.
     * @param handler The request handler.
     */
    public ServiceHandlerDefinition(String request_path,
                                    Request.RequestType request_type,
                                    Handler handler) {
      this.request_type_ = request_type;
      this.handler_ = handler;
      if (request_path.startsWith("/")) {
        this.request_path_ = request_path;
        node_path_ = home_path_ + request_path.substring(1);
      } else {
        this.request_path_ = "/" + request_path;
        node_path_ = home_path_ + request_path;
      }
      if (node_path_.endsWith("/*")) {
        node_path_ = node_path_.substring(0, node_path_.length() - 2);
      }
    }

    /**
     * Returns the method that handles service requests.
     *
     * @return The handler.
     */
    public Handler getHandler() {
      return handler_;
    }

    /**
     * Returns the full directory node path of the handler. The handler is added to the
     * directory node at this path.
     *
     * @param The full directory node path of the handler.
     */
    public String getNodePath() {
      return node_path_;
    }

    /**
     * Returns the request path that is handled by the handler. The request path is used to
     * subscribe and unsubscribe event handlers from remote endpoints.
     *
     * @return The request path of the handler.
     */
    public String getRequestPath() {
      return request_path_;
    }

    /**
     * Returns the type of request handled by the service handler.
     *
     * @return Type of request handled by the service handler.
     */
    public Request.RequestType getRequestType() {
      return request_type_;
    }

    private Handler handler_;  // Handler that handles service requests.
    private String node_path_;  // Path to the directory node of the handler.
    private String request_path_;  // Request path that is handled by handler.
    private Request.RequestType request_type_;  // The type of request handled by handler.
  }

  /**
   * Creates a service definition with the specified parameters.
   *
   * @param name The unique service name.
   * @param service The service instance.
   * @param home_path The service home path.
   */
  public ServiceDefinition(String name, Object service, String home_path) {
    this.name_ = name;
    this.service_ = service;
    this.home_path_ = home_path;
    handler_definitions_ = new ArrayList<ServiceHandlerDefinition>();
    auto_connect_ = true;
  }

  /**
   * Adds a handler for a service method to the directory.
   *
   * If necessary, this method creates the directory path to the handler node.
   *
   * @param handler_path The directory path to the handler node relative to the service home path.
   * @param request_type The type of requests handled by the service method.
   * @param method The service method. Must be a method of the service class.
   * @return True if the handler was successfully added.
   */
  public boolean addHandler(String handler_path, Request.RequestType request_type, Method method) {
    String handler_name = name_ + ":" + request_type.name() + ":" + method.toString();
    ServiceHandlerDefinition handler_def =
      new ServiceHandlerDefinition(handler_path,
                                   request_type,
                                   new MethodHandler(handler_name,
                                                     handler_path.endsWith("/*"),
                                                     service_,
                                                     method));
    if (Directory.Instance.createPath(handler_def.getNodePath()) &&
        Directory.Instance.addHandler(handler_def.getNodePath(), handler_def.getHandler())) {
      handler_definitions_.add(handler_def);
      log.trace("Added service handler {} @ {}", handler_name, handler_def.getNodePath());
      return true;
    } else {
      return false;
    }
  }

  /**
   * Connects the service to the connection by subscribing all event methods of the service to
   * the connection.
   *
   * @param connection The connection to connect to.
   * @return True if all event methods have been successfully subscribed.
   */
  public boolean connect(Connection connection) {
    boolean success = true;
    for (ServiceHandlerDefinition handler_def : handler_definitions_) {
      if (handler_def.getRequestType() == Request.RequestType.Publish) {
        if (!connection.subscribe(handler_def.getRequestPath())) {
          success = false;
        }
      }
    }
    return success;
  }


  /**
   * Disconnects the service from the connection by unsubscribing all event methods of the service
   * from the connection.
   *
   * @param connection The connection to disconnect from.
   * @return True if all event methods have been successfully unsubscribed.
   */
  public boolean disconnect(Connection connection) {
    boolean success = true;
    for (ServiceHandlerDefinition handler_def : handler_definitions_) {
      if (handler_def.getRequestType() == Request.RequestType.Publish) {
        if (!connection.unsubscribe(handler_def.getRequestPath())) {
          success = false;
        }
      }
    }
    return success;
  }

  /**
   * Returns whether the service should be automatically connected to new connections.
   *
   * @return Whether the service should be automatically connected to new connections.
   */
  public boolean getAutoConnect() {
    return auto_connect_;
  }

  /**
   * Returns the service home path.
   *
   * @return The service home path.
   */
  public String getHomePath() {
    return home_path_;
  }

  /**
   * Returns the service name.
   *
   * @return The service name.
   */
  public String getName() {
    return name_;
  }

  /**
   * Returns the service instance, i.e. the instance which implements the service.
   *
   * @return The service instance.
   */
  public Object getService() {
    return service_;
  }

  /**
   * Removes all service handlers from the directory.
   *
   * This method removes all handles added by
   * {@link #addHandler(String, Request.RequestType, Method)}. This method does not remove any
   * paths created by the addHandler method since those paths may be still in use.
   */
  public void removeAllHandlers() {
    for (ServiceHandlerDefinition handler_def : handler_definitions_) {
      Directory.Instance.removeHandler(handler_def.getNodePath(),
                                       handler_def.getHandler().getName());
      log.trace("Removed service handler {} @ {}",
                handler_def.getHandler().getName(), handler_def.getNodePath());
    }
    handler_definitions_.clear();
  }

  /**
   * Sets whether the service should be automatically connected to new connections.
   *
   * If this value is true, the {@link Plugin} that created this instance automatically
   * connects the service when a new connection becomes ready. If this value is false, the
   * {@link #connect(Connection)} method must be manually called to connect the service.
   *
   * If this value is true, the plugin also automatically disconnects the service when the
   * connection is closed.
   *
   * If the auto connect value is changed while a service is connected, the current value will
   * be used by the plugin to decide whether to automatically to disconnect the service.
   *
   * @param auto_connect Whether the service should be automatically connected to new connections.
   */
  public void setAutoConnect(boolean auto_connect) {
  }

  private static Logger log = LogManager.getLogger();

  private boolean auto_connect_;  // Whether to automatically connect to new connections.
  private String name_;  // The service name.
  private ArrayList<ServiceHandlerDefinition> handler_definitions_;  // Service handlers.
  private String home_path_;  // The service home path.
  private Object service_;  // The service instance.
}
