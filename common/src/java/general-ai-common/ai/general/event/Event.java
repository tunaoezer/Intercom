/* General AI - Event API
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

import java.util.ArrayList;

/**
 * Represents an event that can be observed by one or more {@link Observer} instances. Events are
 * triggered by producer threads which are consumed by Observer threads asynchronously. The
 * producer threads are not blocked while the Observers process the event. Multiple observers
 * can process the event simultaneously.
 *
 * An event is associated with an event data that is supplied to the observers.
 *
 * All events are processed in the order they are triggered and no events will be missed by
 * observers.
 *
 * Event is thread-safe.
 */
public class Event<TEventData> {

  /**
   * Constructs an Event.
   */
  public Event() {
    observers_ = new ArrayList<Observer<TEventData>>();
  }

  /**
   * Subscribes an {@link Observer} to this event. The Observer will be notified whenever this
   * event is triggered until it is unsubscribed.
   *
   * @param observer Observer to subscribe to this event.
   */
  public synchronized void subscribe(Observer<TEventData> observer) {
    observers_.add(observer);
  }

  /**
   * Used by the event thread to trigger the event. All observers are notified about the event.
   *
   * @param event_data Data associated with event.
   */
  public synchronized void trigger(TEventData event_data) {
    for (Observer<TEventData> observer : observers_) {
      observer.event(event_data);
    }
  }

  /**
   * Unsubscribes a subscribed {@link Observer} from this event.
   *
   * @param observer Observer to unsubscribe from this event.
   */
  public synchronized void unsubscribe(Observer<TEventData> observer) {
    observers_.remove(observer);
  }

  private ArrayList<Observer<TEventData> > observers_;  // List of event observers.
}
