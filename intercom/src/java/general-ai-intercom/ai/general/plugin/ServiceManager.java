/* General AI - Plugin Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.directory.Request;
import ai.general.plugin.annotation.RpcMethod;
import ai.general.plugin.annotation.Subscribe;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Manages Service instances and definitions.
 *
 * A Service is a class that declares one or more methods either with the {@link RpcMethod}
 * annotation or {@link Subscribe} annotation.
 *
 * The ServiceManager manages the creation and life-cycle of the service instances.
 *
 * Each Service instance is associated with a home path. The RPC methods and subscriptions
 * declared in a class are interpreted with respected to the home path of the Service instance.
 *
 * ServiceManager is a singleton class.
 */
public class ServiceManager {

  /**
   * Singleton instance.
   */
  public static final ServiceManager Instance = new ServiceManager();

  /**
   * ServiceManager is a singleton.
   * The singleton instance can be obtained via {@link #Instance}.
   */
  private ServiceManager() {
    services_ = new HashMap<String, ServiceDefinition>();
  }

  /**
   * Adds a new service instance with the specified home path.
   * Returns the associated {@link ServiceDefinition}.
   *
   * This method scans the service instance for service annotations and automatically generates
   * handlers for annotated methods. The class of the service instance and all of its super
   * classes are scanned.
   *
   * Multiple services can shares the same service home path. The service home path is typically
   * the home path of the user account or the root of the directory.
   * This method creates all directory paths as necessary.
   *
   * This method does not abort if it fails to add a handler, but continues with adding the
   * remaining handlers.
   *
   * The service name must be unique.
   *
   * @param service_name A unique service name.
   * @param service The service instance.
   * @param home_path The home path associated with the service instance. Must be absolute.
   * @return The service definition of the added service.
   * @throws IllegalArgumentException if a service with the same name already exists.
   */
  public ServiceDefinition addService(String service_name, Object service, String home_path) {
    if (services_.containsKey(service_name)) {
      throw new IllegalArgumentException("Duplicate service name: " + service_name);
    }
    if (!home_path.endsWith("/")) {
      home_path = home_path + "/";
    }
    ServiceDefinition service_def = new ServiceDefinition(service_name, service, home_path);
    services_.put(service_name, service_def);
    for (Method method : service.getClass().getMethods()) {
      Subscribe subscription = method.getAnnotation(Subscribe.class);
      if (subscription != null) {
        service_def.addHandler(subscription.value(), Request.RequestType.Publish, method);
      }
      RpcMethod rpc_declaration = method.getAnnotation(RpcMethod.class);
      if (rpc_declaration != null) {
        service_def.addHandler(rpc_declaration.value(), Request.RequestType.Call, method);
      }
    }
    log.trace("Added service {} @ {}", service_name, home_path);
    return service_def;
  }

  /**
   * Returns the service with the specified name or null if no such service is registered.
   * The service name must include the plugin name if the service has been registered via a
   * plugin.
   *
   * @param service_name The full name of the service.
   * @return The service instance or null.
   */
  public Object getService(String service_name) {
    ServiceDefinition service_def = services_.get(service_name);
    if (service_def != null) {
      return service_def.getService();
    }
    return null;
  }

  /**
   * Returns the service definition for the service with the specified name. If no service with
   * the specified name is registered, returns null.
   * The service name must include the plugin name if the service has been registered via a
   * plugin.
   *
   * @param service_name The full name of the service.
   * @return The service definition of the service or null.
   */
  public ServiceDefinition getServiceDefinition(String service_name) {
    return services_.get(service_name);
  }

  /**
   * Returns true if a service with the specified name is registered.
   * The service name must include the plugin name if the service has been registered via a
   * plugin.
   *
   * @param service_name The full name of the service.
   * @return True if the service is registered.
   */
  public boolean hasService(String service_name) {
    return services_.containsKey(service_name);
  }

  /**
   * Removes a service that was previous added via {@link #addService(String, Object, String)}.
   *
   * All handlers associated with the service are removed. Paths that were created for the service
   * are not deleted since they may still be used.
   *
   * @param service_name The service name.
   */
  public void removeService(String service_name) {
    ServiceDefinition service_def = services_.get(service_name);
    if (service_def == null) {
      return;
    }
    service_def.removeAllHandlers();
    services_.remove(service_name);
    log.trace("Removed service {}", service_name);
  }

  private static Logger log = LogManager.getLogger();

  // List of services: service name -> ServiceDefinition.
  private HashMap<String, ServiceDefinition> services_;
}
