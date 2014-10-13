/* General AI - Event API
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Base class for all watcher threads. Watchers are used to periodically check on a condition
 * and take action as appropriate.
 *
 * Subclasses must implement the {@link #watch()} method to define watch actions.
 * Watchers are daemon threads. All watchers automatically exit when the program exits.
 *
 * The watch() method is called with the specified period relative to the last run, rather than at
 * fixed time intervals. Thus, the watch() method is called again after the specified period after
 * the current execution of the run method has exited.
 */
public abstract class Watcher {

  /**
   * The WatcherThread periodically invokes the watch() method of the Watcher.
   */
  private class WatcherThread extends Thread {

    /**
     * Creates a watcher thread.
     *
     * @param name The name of the thread.
     * @param sleep_time_millis The amount of time the thread sleeps between each watch iteration.
     */
    public WatcherThread(String name, long sleep_time_millis) {
      super(name);
      setDaemon(true);
      this.sleep_time_millis_ = sleep_time_millis;
    }

    /**
     * Initiates the stop of the watcher thread and immediately exits.
     * Does not wait for the thread to stop.
     */
    public void halt() {
      run_ = false;
    }

    /**
     * Main method of thread.
     */
    @Override
    public void run() {
      run_ = true;
      while (run_) {
        try {
          Thread.sleep(sleep_time_millis_);
        } catch (InterruptedException e) {}
        if (!run_) {
          return;
        }
        watch();
      }
    }

    /**
     * Updates the amount of time this thread sleeps during each watch iteration.
     *
     * @param sleep_time_millis Updated sleep time in milliseconds.
     */
    public void setSleepTime(long sleep_time_millis) {
      this.sleep_time_millis_ = sleep_time_millis;
    }

    private volatile boolean run_;  // True if the watcher thread runs.
    private volatile long sleep_time_millis_;  // Time between consecutive watch calls.
  }

  /**
   * Constructs a watcher with the specified name and watch period. The watch method of the watcher
   * is called periodically this many milliseconds after the last run.
   *
   * @param name The name of the watcher.
   * @param watch_period_millis The watch period in milliseconds.
   */
  public Watcher(String name, long watch_period_millis) {
    this.name_ = name;
    this.watch_period_millis_ = watch_period_millis;
    this.watcher_thread_ = null;
  }

  /**
   * Returns the watch period in milliseconds.
   *
   * @return The watch period in milliseconds.
   */
  public long getWatchPeriodMillis() {
    return watch_period_millis_;
  }

  /**
   * Reschedules this watcher with a new watch period.
   */
  public synchronized void reschedule(long watch_period_millis) {
    this.watch_period_millis_ = watch_period_millis;
    if (watcher_thread_ != null) {
      watcher_thread_.setSleepTime(watch_period_millis);
    }
  }

  /**
   * Starts the watcher if it has not been started already.
   */
  public synchronized void start() {
    if (watcher_thread_ != null) {
      return;
    }
    log.debug("Starting watcher thread {}.", name_);
    watcher_thread_ = new WatcherThread(name_, watch_period_millis_);
    watcher_thread_.start();
  }

  /**
   * Stops the watcher.
   *
   * This method initiates the exit of the watcher thread but does not wait for the thread to exit.
   */
  public synchronized void stop() {
    if (watcher_thread_ == null) {
      return;
    }
    log.debug("Stopping watcher thread {}.", name_);
    watcher_thread_.halt();
    watcher_thread_ = null;
  }

  /**
   * The watch method is periodically called according to the current watch schedule.
   * Subclasses must implement specific watch tasks by overriding the watch method.
   */
  protected abstract void watch();

  private static Logger log = LogManager.getLogger();

  private String name_;  // The name of the watcher thread.
  private long watch_period_millis_;  // Time between consecutive watch calls.
  private WatcherThread watcher_thread_;  // The watcher thread.
}
