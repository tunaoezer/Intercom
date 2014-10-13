/* General AI - Test Plugin
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin.test;

import ai.general.net.Connection;
import ai.general.plugin.Plugin;

import java.net.URI;

/**
 * Plugin used for testing purposes.
 */
public class TestPlugin extends Plugin {

  /**
   * Constructs a TestPlugin.
   */
  public TestPlugin() {
    super("TestPlugin",
          1.0,
          "Plugin used for testing purposes.",
          "General AI");
  }

  /**
   * Called when a new connection is established.
   *
   * @param connection The new connection.
   * @return True if the plugin has been successfully connected.
   */
  @Override
  protected boolean onConnect(Connection connection) {
    if (!connection.getUri().getPath().endsWith("plugin_test/client")) {
      return false;
    }
    registerService("test_service", new TestService(), connection.getHomePath(), true);
    return super.onConnect(connection);
  }

  /**
   * Called when a connection is closed.
   *
   * @param connection The connection being closed.
   * @return True if the plugin has been successfully disconnected.
   */
  @Override
  protected boolean onDisconnect(Connection connection) {
    if (!connection.getUri().getPath().endsWith("plugin_test/client")) {
      return true;
    }
    super.onDisconnect(connection);
    unregisterService("test_service");
    return true;
  }
}
