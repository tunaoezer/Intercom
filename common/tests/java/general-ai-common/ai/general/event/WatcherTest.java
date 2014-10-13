/* General AI - Event API
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for the {@link Watcher} class.
 */
public class WatcherTest {

  /**
   * Implements a test watcher.
   */
  private static class TestWatcher extends Watcher {

    /**
     * Constructs a TestWatcher.
     *
     * @param watch_period_millis The watch period in milliseconds.
     */
    public TestWatcher(long watch_period_millis) {
      super("test-watcher", watch_period_millis);
      watch_count_ = 0;
    }

    /**
     * Returns the number of times the watch method has been called.
     *
     * @return The number of times the watch method has been called.
     */
    public int getWatchCount() {
      return watch_count_;
    }

    /**
     * Increments a watch count.
     */
    @Override
    protected void watch() {
      watch_count_++;
    }

    private volatile int watch_count_;  // Counts the number of times watch is called.
  }

  /**
   * Tests the watch() and reschedule() methods.
   */
  @Test
  public void watch() {
    TestWatcher watcher = new TestWatcher(10);
    assertThat(watcher.getWatchCount(), is(0));
    watcher.start();
    try {
      Thread.sleep(15);
    } catch (InterruptedException e) {}
    int watch_count = watcher.getWatchCount();
    Assert.assertTrue(watcher.getWatchCount() > 0);
    watcher.reschedule(1);
    try {
      // allow sufficient time to process at least 5 watch calls
      Thread.sleep(25);
    } catch (InterruptedException e) {}
    Assert.assertTrue(watcher.getWatchCount() - watch_count > 5);
    watcher.stop();
    try {
      // allow time for watcher thread to stop
      Thread.sleep(10);
    } catch (InterruptedException e) {}
    watch_count = watcher.getWatchCount();
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {}
    assertThat(watcher.getWatchCount(), is(watch_count));
  }
}
