/* General AI - Test Plugin
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.plugin.test;

import ai.general.plugin.annotation.RpcMethod;
import ai.general.plugin.annotation.Subscribe;

/**
 * Service used for testing purposes.
 */
public class TestService {

  /**
   * Constructs a TestService.
   */
  public TestService() {
    last_event_data_ = "nothing received";
  }

  /**
   * Test RPC method.
   *
   * @param x A floating point number.
   * @param y A floating point number.
   * @return x + y
   */
  @RpcMethod("test_service/add")
  public double add(double x, double y) {
    return x + y;
  }

  /**
   * Provides access to the last data received by the {@link pluginEvent(String)} method.
   *
   * @return The last event data received.
   */
  @RpcMethod("test_service/getEventData")
  public String getEventData() {
    return last_event_data_;
  }

  /**
   * Listens to events on the test plugin topic.
   * The event data received by this method is stored in the last_event_data_ variable and
   * can be accessed via {@link #getEventData()}.
   *
   * @param data Event data received by server.
   */
  @Subscribe("plugin_test/plugin_event")
  public void pluginEvent(String data) {
    last_event_data_ = data;
  }

  private String last_event_data_;  // Last event data received by pluginEvent(String).
}
