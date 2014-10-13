/* General AI - Plugin Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.net.Connection;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * The PluginManager loads and manages plugins.
 *
 * The PluginManager loads plugins from a jar file, initializes the plugins and unitializes them
 * before shutdown.
 *
 * PluginManager is a singleton class.
 */
public class PluginManager {

  /**
   * Singleton instance.
   */
  public static final PluginManager Instance = new PluginManager();

  /**
   * PluginManager is a singleton.
   * The singleton instance can be obtained via {@link #Instance}.
   */
  private PluginManager() {
    plugins_ = new HashMap<String, Plugin>();
  }

  /**
   * Connects enabled plugins to the specified connection.
   * Calls the onConnect method of all enabled Plugins.
   *
   * This method is called by the {@link ai.general.net.ConnectionManager}.
   *
   * @param connection The new connection.
   */
  public void connect(Connection connection) {
    for (Plugin plugin : plugins_.values()) {
      if (plugin.isEnabled()) {
        plugin.onConnect(connection);
      }
    }
  }

  /**
   * Disables all loaded plugins.
   */
  public void disableAll() {
    for (Plugin plugin : plugins_.values()) {
      plugin.setEnabled(false);
    }
  }

  /**
   * Disables the plugin with the specified name.
   *
   * This method returns false if the plugin has not been loaded.
   * This method has no effect if the plugin has already been disabled.
   *
   * @param name The name of the plugin.
   */
  public void disablePlugin(String name) {
    Plugin plugin = plugins_.get(name);
    if (plugin != null) {
      plugin.setEnabled(false);
    }
  }

  /**
   * Disconnects enabled plugins from the specified connection.
   * Calls the onDisconnect method of all enabled Plugins.
   *
   * This method is called by the {@link ai.general.net.ConnectionManager}.
   *
   * @param connection The connection being closed.
   */
  public void disconnect(Connection connection) {
    for (Plugin plugin : plugins_.values()) {
      if (plugin.isEnabled()) {
        plugin.onDisconnect(connection);
      }
    }
  }

  /**
   * Enables all loaded plugins.
   *
   * Returns true if all plugins have been enabled.
   */
  public boolean enableAll() {
    boolean success = true;
    for (Plugin plugin : plugins_.values()) {
      if (!plugin.setEnabled(true)) {
        success = false;
      }
    }
    return success;
  }

  /**
   * Enables the plugin with the specified name.
   *
   * This method returns false if the plugin has not been loaded or could not be enabled.
   * This method has no effect if the plugin has already been enabled.
   *
   * @param name The name of the plugin.
   * @return True if the plugin has been enabled.
   */
  public boolean enablePlugin(String name) {
    Plugin plugin = plugins_.get(name);
    if (plugin != null) {
      return plugin.setEnabled(true);
    } else {
      return false;
    }
  }

  /**
   * Returns the plugin with the specified name or null if no such plugin has been loaded.
   *
   * @param name The name of the plugin.
   * @return The plugin or null if no such plugin has been loaded.
   */
  public Plugin getPlugin(String name) {
    return plugins_.get(name);
  }

  /**
   * Returns the number of loaded plugins.
   *
   * @return The number of loaded plugins.
   */
  public int getPluginCount() {
    return plugins_.size();
  }

  /**
   * Returns true if a plugin with the specified name has been enabled.
   *
   * @param name The name of the plugin.
   * @return True if the plugin is enabled.
   */
  public boolean isPluginEnabled(String name) {
    Plugin plugin = plugins_.get(name);
    if (plugin != null) {
      return plugin.isEnabled();
    } else {
      return false;
    }
  }

  /**
   * Returns true if a plugin with the specified name has been loaded.
   *
   * @param name The name of the plugin.
   * @return True if the plugin is loaded.
   */
  public boolean isPluginLoaded(String name) {
    return plugins_.containsKey(name);
  }

  /**
   * Loads all plugins in the specified jar file.
   *
   * This plugins must enabled after they have been loaded.
   *
   * @param jar_filepath Path to a jar file.
   * @return The number of plugins that have been loaded. -1 if the file could not be opened.
   */
  public int load(String jar_filepath) {
    log.debug("loading {}", jar_filepath);
    try {
      JarFile jar = new JarFile(jar_filepath);
      Enumeration<JarEntry> entries = jar.entries();
      ArrayList<String> class_names = new ArrayList<String>();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.getName().endsWith(".class")) {
          class_names.add(
              entry.getName().substring(0, entry.getName().length() - 6).
              replace('/', '.'));
        }
      }
      jar.close();
      final File file = new File(jar_filepath);
      URLClassLoader class_loader =
        AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
              try {
                return new URLClassLoader(new URL[] { file.toURI().toURL() },
                                          Plugin.class.getClassLoader());
              } catch (MalformedURLException e) {
                log.catching(Level.TRACE, e);
                return null;
              }
            }
          });
      int plugin_count = 0;
      if (class_loader != null) {
        for (String class_name : class_names) {
          try {
            Class<?> class_def = class_loader.loadClass(class_name);
            if (load(class_def)) {
              plugin_count++;
            }
          } catch (ClassNotFoundException e) { /* cannot be thrown by construction */ }
        }
        class_loader.close();
      }
      log.debug("Loaded {} plugins from {}", plugin_count, jar_filepath);
      return plugin_count;
    } catch (IOException e) {
      return -1;
    }
  }

  /**
   * Loads the plugin specified by the plugin class definition.
   *
   * The class definition must be a subclass of {@link Plugin}.
   *
   * The name of the plugin must be unique. This method returns false if the a plugin with
   * the same name has already been loaded.
   *
   * The plugin must be enabled after it has been loaded.
   *
   * @param class_def Plugin class definition. Must be a subclass of Plugin.
   * @return True if the plugin was successfully loaded.
   */
  public boolean load(Class<?> class_def) {
    if (Plugin.class.isAssignableFrom(class_def)) {
      try {
        Plugin plugin = (Plugin) class_def.getConstructor().newInstance();
        if (plugins_.containsKey(plugin.getName())) {
          return false;
        }
        if (!plugin.onLoad()) {
          return false;
        }
        plugins_.put(plugin.getName(), plugin);
        log.debug("loaded plugin {}", plugin.getName());
        return true;
      } catch (ReflectiveOperationException e) {}
    }
    return false;
  }

  /**
   * Loads all plugins in the specified directory.
   *
   * This method scans all jar files in the directory and loads all plugin classes in those
   * jar files.
   *
   * @return The total number of plugins loaded.
   */
  public int loadAll(String directory) {
    File dir = new File(directory);
    File[] list = dir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });
    if (list == null) {
      return 0;
    }
    int plugin_count = 0;
    for (File file : list) {
      plugin_count += load(file.getAbsolutePath());
    }
    return plugin_count;
  }

  /**
   * Disables and unloads all plugins.
   */
  public void unloadAll() {
    for (Plugin plugin : plugins_.values()) {
      plugin.unload();
    }
    plugins_.clear();
  }

  private static Logger log = LogManager.getLogger();

  private HashMap<String, Plugin> plugins_;  // All plugins. (name, plugin).
}
