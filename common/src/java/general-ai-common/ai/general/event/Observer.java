/* General AI - Event API
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Observes asynchronous events in the order they are triggered. Observers are consumers of events
 * triggered by a producer thread.
 *
 * Observers process events on a thread that is different from the thread that triggered the
 * event. More events may be triggered while an event is being processed. The Observer class
 * queues events until the Observer thread is ready to process them. The Observer class ensures
 * that no events are missed and all events are processed in order.
 *
 * If no events are available, an Observer can block until an event is triggered.
 *
 * Observer is thread-safe.
 */
public class Observer<TEventData> {

  /**
   * Constructs an Observer with an unbounded event queue.
   */
  public Observer() {
    event_queue_ = new LinkedBlockingQueue<TEventData>();
  }

  /**
   * Constructs an Observer with a bounded event queue that can buffer up to event_queue_size_limit
   * many events.
   *
   * If the event queue fills up, the Observer ignores any further events until the next event in
   * the queue is processed or the event queue is cleared.
   *
   * @param event_queue_size_limit The maximum size of the event queue.
   */
  public Observer(int event_queue_size_limit) {
    event_queue_ = new LinkedBlockingQueue<TEventData>(event_queue_size_limit);
  }

  /**
   * Clears the event queue. This causes all currently queued events to be dropped.
   */
  public void clear() {
    event_queue_.clear();
  }

  /**
   * Used by the event thread to signal an event. The event data is queued up for the Observer to
   * process when it is ready. If the Observer is currently polling for the event, it is woken
   * up.
   *
   * If the event queue is bounded and has reached its capacity, this method ingores the event.
   *
   * @param event_data Data associated with event.
   */
  public void event(TEventData event_data) {
    event_queue_.offer(event_data);
  }

  /**
   * Returns the number of unprocessed events in the event queue of the observer.
   *
   * @return The number of unprocessed events.
   */
  public int numPendingEvents() {
    return event_queue_.size();
  }

  /**
   * Used by the Observer thread to poll for the next event. If an event has been triggered
   * since the last poll(), the data associated with the event is immediately returned.
   *
   * If there are no unprocessed events, this method blocks for the specified duration and
   * returns null if no events have been observed while waiting. The caller may continue to
   * wait for events by calling poll again.
   *
   * If timeout_millis is set to zero, this method returns without blocking.
   *
   * @param timeout_millis Wait timeout in milliseconds.
   * @return Event data or null if a timeout has occurred.
   */
  public TEventData poll(long timeout_millis) {
    try {
      return event_queue_.poll(timeout_millis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      return null;
    }
  }

  private LinkedBlockingQueue<TEventData> event_queue_;  // Queue of unprocessed events.
}
