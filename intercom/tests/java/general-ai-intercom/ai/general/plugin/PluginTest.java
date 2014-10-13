/* General AI - Plugin Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin;

import ai.general.directory.Directory;
import ai.general.directory.Request;
import ai.general.directory.Result;
import ai.general.directory.test.TestUtilities;
import ai.general.net.wamp.WampConnectionTest;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link Plugin} and {@link PluginManager} classes.
 */
public class PluginTest {

  // The path is relative to the project build file.
  private static final String kTestPluginPath = "../build/jar/test-plugin-0.jar";

  /**
   * Tests loading of plugins.
   *
   * This test loads a test plugin located in test_plugins.jar. It then verifies that the service
   * registered by plugin is correctly registered both on the client and server side.
   */
  @Test
  public void load() {
    final String kHostname = "general.ai";
    final String kUserAccount = "plugin@test.top";
    final String kClientHomePath = "/plugin_test/client";
    final String kServerHomePath = "/plugin_test/server";
    final String kPluginName = "TestPlugin";
    final String kServiceName = "test_service";
    final String kRpcMethodPath = "/" + kServiceName + "/add";
    final String kTopicPath = "/plugin_test/plugin_event";
    final String kTopicData = "test plugin test data";
    final String kGetEventDataPath = "/" + kServiceName + "/getEventData";
    final String kHandlerName = kPluginName + "/" + kServiceName +
      ":Call:public double ai.general.plugin.test.TestService.add(double,double)";

    WampConnectionTest.TestConnection connection =
      new WampConnectionTest.TestConnection("/plugin_test",
                                            kUserAccount,
                                            kClientHomePath,
                                            kServerHomePath);

    // Load.
    PluginManager plugin_manager = PluginManager.Instance;
    Assert.assertFalse(plugin_manager.isPluginLoaded(kPluginName));
    assertThat(plugin_manager.load(kTestPluginPath), is(1));
    assertThat(plugin_manager.getPluginCount(), is(1));
    Assert.assertTrue(plugin_manager.isPluginLoaded(kPluginName));
    Plugin plugin = plugin_manager.getPlugin(kPluginName);
    Assert.assertNotNull(plugin);

    // Enable.
    Assert.assertFalse(plugin.isEnabled());
    Assert.assertTrue(plugin_manager.enableAll());
    Assert.assertTrue(plugin.isEnabled());

    // Connect.
    connection.open();
    Assert.assertTrue(plugin.hasService(kServiceName));
    Assert.assertNotNull(plugin.getService(kServiceName));
    Assert.assertNotNull(plugin.getServiceDefinition(kServiceName));

    // Client side request tests.
    Directory directory = Directory.Instance;
    Assert.assertTrue(directory.pathExists(kClientHomePath + kRpcMethodPath));
    Assert.assertTrue(directory.hasHandler(kClientHomePath + kRpcMethodPath, kHandlerName));
    Object[] args = { 123, 0.321 };
    Request request =
      TestUtilities.createRequest(kClientHomePath + kRpcMethodPath + "?type=call", args);
    assertThat(directory.handle("/", request), is(1));
    Result result = request.getResult();
    assertThat(result.numValues(), is(1));
    assertThat((double) result.getValue(0), is(123.321));

    // Server side requests tests.
    connection.server().publish(kTopicPath, kTopicData);
    request = TestUtilities.createRequest(kClientHomePath + kGetEventDataPath + "?type=call");
    assertThat(directory.handle("/", request), is(1));
    result = request.getResult();
    assertThat(result.numValues(), is(1));
    assertThat((String) result.getValue(0), is(kTopicData));

    // Disable.
    plugin_manager.disablePlugin(kPluginName);
    Assert.assertFalse(plugin.isEnabled());
    Assert.assertFalse(plugin.hasService(kServiceName));
    Assert.assertFalse(directory.hasHandler(kClientHomePath + kRpcMethodPath, kHandlerName));
    Assert.assertTrue(plugin_manager.isPluginLoaded(kPluginName));

    // Re-enable.
    Assert.assertTrue(plugin_manager.enablePlugin(kPluginName));
    Assert.assertTrue(plugin.isEnabled());
    Assert.assertTrue(plugin.hasService(kServiceName));
    Assert.assertTrue(directory.hasHandler(kClientHomePath + kRpcMethodPath, kHandlerName));

    // Disconnect and unload.
    connection.close();
    Assert.assertFalse(plugin.hasService(kServiceName));
    Assert.assertFalse(directory.hasHandler(kClientHomePath + kRpcMethodPath, kHandlerName));
    plugin_manager.unloadAll();
    Assert.assertFalse(plugin.isEnabled());
    Assert.assertFalse(plugin_manager.isPluginLoaded(kPluginName));
  }
}
