/* General AI - Event API
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

/**
 * Base class for event processors. A Processor observes events of type TEventData and
 * asynchronously procsses any trigerred events on a separate thread.
 *
 * Subclasses must implement the specific processing logic by overriding the
 * {@link #process(Object)} method.
 *
 * Once constructed, a processor must be started with the {@link #start()} method.
 * A started processor must be stopped via the {@link #halt()} method.
 *
 * In order to receive events, the processor must be subscribed to one or more event sources
 * via the {@link #subscribe(Event)}.
 */
public abstract class Processor<TEventData> extends Thread {

  /**
   * Maximum amount of time to wait for an event during a single event poll.
   * This number primarily affects how quickly the processor halts. It has no affect on the
   * latency of processing events.
   */
  public static final int kPollTimeoutMillis = 10;

  /**
   * Constructs a Processor.
   *
   * @param name The name of the processor.
   */
  protected Processor(String name) {
    super(name);
    observer_ = new Observer<TEventData>();
    running_ = false;
  }

  /**
   * Stops a started processor.
   *
   * This method initiates the stop of the processor but does not wait for the processor to
   * stop. Callers that want to wait for the processor to stop may join the processor thread.
   *
   * Once a processor has been halted, it cannot be restarted again.
   */
  public void halt() {
    running_ = false;
  }

  /**
   * The main method of the processor.
   *
   * Waits until an event is received and processes the event.
   * Loops until the processor is halted.
   */
  @Override
  public void run() {
    running_ = true;
    do {
      TEventData event_data = observer_.poll(kPollTimeoutMillis);
      if (event_data != null) {
        process(event_data);
      }
    } while (running_);
  }

  /**
   * Subscribes this processor to receive events from the specified event source.
   *
   * @param event The event to subscribe to.
   */
  public void subscribe(Event<TEventData> event) {
    event.subscribe(observer_);
  }

  /**
   * Unsubscribes this processor from the specified event source.
   * This method is the counterpart to the {@link #subscribe(Event)} method.
   *
   * @param event The event to subscribe to.
   */
  public void unsubscribe(Event<TEventData> event) {
    event.unsubscribe(observer_);
  }

  /**
   * Returns the Observer associated with this processor.
   *
   * @return The Observer of this processor.
   */
  protected Observer<TEventData> getObserver() {
    return observer_;
  }

  /**
   * Processes an event. This method must be implemented by a specific Processor subclass.
   *
   * @param event_data The data associated with the event.
   */
  protected abstract void process(TEventData event_data);

  private Observer<TEventData> observer_;  // The event observer associated with this processor.
  private volatile boolean running_;  // True if the processor is running.
}
