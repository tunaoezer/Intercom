/* General AI - Plugin Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.net.Connection;
import ai.general.net.ConnectionManager;

import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Base class for plugins.
 *
 * An plugin represents a set of services that provide some functionality.
 * Plugins are compiled into their own jar files and loaded dynamically by the
 * {@link PluginManager}. The PluginManager scans a jar file for classes that are subclasses of
 * Plugin and automatically installs them.
 *
 * Plugin contains methods that provide general information about the plugin. In addition, the
 * methods {@link #onLoad()} and {@link #onUnload()} are called when the plugin is loaded and
 * unloaded, respectively. The methods {@link #onEnable()} and {@link #onDisable()} are called when
 * a loaded plugin is activated and deactivated. Plugins may use these methods to initialize
 * data structures or register their shared services using the
 * {@link #registerService(String, Object, String, boolean)} method.
 *
 * The PluginManager calls the {@link #onConnect(Connection)} method when a new connection is
 * created and becomes ready. The onConnect method can be used to initialize and register
 * connection specific data structures and services. When the connection is closed, the
 * {@link #onDisconnect(Connection)} method is called.
 *
 * A concrete subclass of Plugin must be public and define a public constructor that takes no
 * arguments.
 */
public abstract class Plugin {

  /**
   * Subclasses must define a constructor that takes no arguments.
   *
   * @param name The name of this plugin.
   * @param version The version of this plugin. Larger numbers corresponds to later versions.
   * @param description A brief description of the plugin.
   * @param publisher The publisher of the plugin.
   */
  public Plugin(String name, double version, String description, String publisher) {
    this.name_ = name;
    this.version_ = version;
    this.description_ = description;
    this.publisher_ = publisher;
    services_ = new HashMap<String, ServiceDefinition>();
    enabled_ = false;
  }

  /**
   * Plugin description.
   *
   * @return Plugin description.
   */
  public String getDescription() {
    return description_;
  }

  /**
   * Plugin name.
   *
   * @return The name of this plugin.
   */
  public String getName() {
    return name_;
  }

  /**
   * Plugin publisher name.
   *
   * @return The publisher of the plugin.
   */
  public String getPublisher() {
    return publisher_;
  }

  /**
   * Plugin version.
   *
   * @return The version of this plugin. A larger number corresponds to a later version.
   */
  public double getVersion() {
    return version_;
  }

  /**
   * Returns the service with the specified name or null if no such service is registered via
   * this plugin.
   * The service name does not include the name of this plugin.
   *
   * @param service_name The name of the service.
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
   * Returns the service definition for the service with the specified name if it has been
   * registered via this plugin. If no service with the specified name is registered via this
   * plugin, returns null.
   * The service name does not include the name of this plugin.
   *
   * @param service_name The name of the service.
   * @return The service definition of the service or null.
   */
  public ServiceDefinition getServiceDefinition(String service_name) {
    return services_.get(service_name);
  }

  /**
   * Returns true if the plugin has a service with the specified name registered.
   *
   * @param name The unique name of the service.
   * @return True if the plugin has a service with the specified name registered. 
   */
  public boolean hasService(String name) {
    return services_.containsKey(name);
  }

  /**
   * Returns true if the plugin has been enabled.
   *
   * @return True if the plugin has been enabled.
   */
  public boolean isEnabled() {
    return enabled_;
  }

  /**
   * Connects all services of this plugin that are configured to auto-connect to the connection by
   * subscribing all event methods of all services to the connection.
   *
   * Services that are configured not to auto-connect are skipped by this method and must be
   * manually connected.
   *
   * This method is automatically called by the {@link #onConnect(Connection)} method. This
   * method should not be called explicitly by a subclasses that calls the base class
   * implementation of the onConnect(Connection) method.
   *
   * @param connection The connection to connect to.
   * @return True if all event methods have been successfully subscribed.
   */
  protected boolean connectServices(Connection connection) {
    boolean success = true;
    for (ServiceDefinition service_def : services_.values()) {
      if (service_def.getAutoConnect()) {
        if (!service_def.connect(connection)) {
          success = false;
        }
      }
    }
    return success;
  }

  /**
   * Disconnects all services of this plugin that are configured to auto-connect from the
   * connection by unsubscribing all event methods of all services from the connection.
   *
   * Services that are configured not to auto-connect are skipped by this method and must be
   * manually disconnected.
   *
   * This method is automatically called by the {@link #onDisconnect(Connection)} method.
   * This method should not be called explicitly by a subclasses that calls the base class
   * implementation of the onDisconnect(Connection) method.
   *
   * @param connection The connection to disconnect from.
   * @return True if all event methods have been successfully subscribed.
   */
  protected boolean disconnectServices(Connection connection) {
    boolean success = true;
    for (ServiceDefinition service_def : services_.values()) {
      if (!service_def.disconnect(connection)) {
        success = false;
      }
    }
    return success;
  }

  /**
   * Registers a service of this plugin.
   *
   * This method can be called by subclasses when this plugin is enabled to register any services
   * of the plugin.
   *
   * The service instance can be any object that declares one or more methods with the
   * {@link ai.general.plugin.annotation.RpcMethod} annotation or
   * {@link ai.general.plugin.annotation.Subscribe} annotation.
   *
   * The service name must be globally unique. The service is registered with the plugin name
   * prepended to the service name. Specifically, the registered service name is
   * plugin_name/service_name.
   *
   * All paths within the service are interpreted with respect to the service home path.
   *
   * The Plugin class can automatically manage the connection of a service. If a new service
   * becomes ready, all event methods of the service will be automatically subscribed to the new
   * connection. If a connection is closed, all event methods will be automatically unsubscribed.
   * If this is not desirable, automatic management of service connections can be disabled by
   * setting the auto_connect parameter to false and manually connecting or disconnecting the
   * {@link ServiceDefinition} during a {@link #onConnect(Connection)} or
   * {@link #onDisconnect(Connection)} event.
   *
   * Services can be unregistered with the {@link #unregisterService(String)} or
   * {@link #unregisterAllServices()} method.
   *
   * @param service_name A unique service name.
   * @param service The service instance.
   * @param service_home_path The home path associated with the service.
   * @param auto_connect Whether the connections of the service are managed by the plugin.
   * @return True if the service was successfully added.
   * @throws IllegalArgumentException if the service name is a duplicate name.
   */
  protected boolean registerService(String service_name,
                                    Object service,
                                    String service_home_path,
                                    boolean auto_connect) {
    if (services_.containsKey(service_name)) {
      throw new IllegalArgumentException(
          "Duplicate service name: " + getName() + "/" + service_name);
    }
    ServiceDefinition service_def =
      ServiceManager.Instance.addService(getName() + "/" + service_name,
                                         service,
                                         service_home_path);
    service_def.setAutoConnect(auto_connect);
    services_.put(service_name, service_def);
    return true;
  }

  /**
   * Called when a new connection is opened and becomes ready.
   * The connection may be a client or server connection.
   *
   * This method can be used by subclasses to register connection specific services or interact
   * with the remote endpoint.
   *
   * By default, this method connects all registered services of this plugin to the provided
   * connection. If a subclasses overrides this method, this base class implementation should be
   * called to connect the services. This can be done after any connection specific services have
   * been created.
   *
   * Services can be registered with the {@link #registerService(String, Object, String, boolean)}
   * method.
   *
   * This method is called for all enabled plugins.
   *
   * @param connection The new connection.
   * @return True if the plugin has been successfully connected.
   */
  protected boolean onConnect(Connection connection) {
    return connectServices(connection);
  }

  /**
   * Called when a plugin is disabled.
   *
   * A plugin could be disabled just before system shutdown. Disabling should be done quickly and
   * focus on essential clean up operations.
   */
  protected void onDisable() {}

  /**
   * Called just before a connection is closed. Subclasses should use this method to clean up any
   * data structures or unregister any services created in the
   * {@link #onConnect(Connection)} method.
   *
   * By default, this method disconnects all registered services of this plugin from the provided
   * connection. If a subclass overrides this method, this base class implementation should be
   * called to disconnect the services. This must be done before any services are unregistered.
   *
   * Services can be manually unregistered with the {@link #unregisterService(String)} or
   * {@link #unregisterAllServices()} method.
   *
   * Subclasses should not assume that the remote endpoint is still alive. It is possible, that
   * the underlying connection has already been closed by the remote endpoint.
   *
   * @param connection The connection being closed.
   * @return True if the plugin has been successfully disconnected.
   */
  protected boolean onDisconnect(Connection connection) {
    return disconnectServices(connection);
  }


  /**
   * Called when a loaded plugin is enabled. An enabled plugin can respond to incoming service
   * requests.
   *
   * If the plugin cannot be enabled, this method should return false. If this method returns
   * false, the plugin will not be enabled.
   *
   * A plugin may add services to the {@link ServiceManager} in the onEnable() method.
   *
   * @return True if the plugin can be enabled.
   */
  protected boolean onEnable() {
    return true;
  }

  /**
   * Called when the plugin is loaded, but not necessarily enabled.
   *
   * If the plugin cannot be loaded, this method should return false. If this method returns
   * false, the loading of the plugin will be aborted.
   *
   * @return True if the plugin was initialized successfully.
   */
  protected boolean onLoad() {
    return true;
  }

  /**
   * Called when the plugin is unloaded. This could happen just before a system shutdown.
   * Unloading should be done quickly and focus on essential clean up operations.
   */
  protected void onUnload() {}

  /**
   * Unregisters all registered services of this plugin.
   */
  protected void unregisterAllServices() {
    for (ServiceDefinition service_def : services_.values()) {
      ServiceManager.Instance.removeService(service_def.getName());
    }
    services_.clear();
  }

  /**
   * Unregisters a previously registered service with the specified name.
   *
   * @param service_name The service name.
   * @return True if the service has been unregistered.
   */
  protected boolean unregisterService(String service_name) {
    ServiceDefinition service_def = services_.get(service_name);
    if (service_def != null) {
      ServiceManager.Instance.removeService(service_def.getName());
      services_.remove(service_name);
    }
    return true;
  }

  /**
   * Enables or disables the plugin. If the plugin is enabled, the {@link #onEnable()} method
   * will be called. If the plugin is disabled, the {@link #onDisable()} method will be called.
   * If the plugin is already enabled or disabled, this method has no effect.
   *
   * If the plugin is enabled, it is automatically connected to all ready connections. If the
   * plugin is disabled, it is disconnected from all connections.
   *
   * Returns true if the plugin has been enabled. The plugin may not be enabled if the onEnable()
   * method returns false.
   *
   * @param enabled True to enable the plugin, false to disable the plugin.
   * @return True if the plugin is enabled.
   */
  boolean setEnabled(boolean enabled) {
    if (this.enabled_ != enabled) {
      if (enabled) {
        this.enabled_ = onEnable();
        if (this.enabled_) {
          for (Connection connection :
                 ConnectionManager.Instance.getConnections().toArray(new Connection[] {})) {
            onConnect(connection);
          }
        }
        log.debug("enabled plugin {}", name_);
      } else {
        for (Connection connection :
               ConnectionManager.Instance.getConnections().toArray(new Connection[] {})) {
          onDisconnect(connection);
        }
        this.enabled_ = false;
        onDisable();
        unregisterAllServices();
        log.debug("disabled plugin {}", name_);
      }
    }
    return this.enabled_;
  }

  /**
   * Unloads the plugin.
   *
   * This method first disables the plugin and then calls the {@link #onUnload()} method.
   */
  void unload() {
    setEnabled(false);
    onUnload();
    log.debug("unloaded plugin {}", name_);
  }

  private static Logger log = LogManager.getLogger();

  private String description_;  // Plugin description.
  private boolean enabled_;  // True if the plugin has been enabled.
  private String name_;  // Plugin name.
  private String publisher_;  // Publisher information.

  // Services associated with plugin (service name, service definition)
  private HashMap<String, ServiceDefinition> services_;

  private double version_;  // Plugin version.
}
