/* General AI - Event API
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.event;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for the {@link Processor} class.
 */
public class ProcessorTest {

  /**
   * Implements a processor for testing purposes.
   */
  private static class TestProcessor extends Processor<String> {

    /**
     * Constructs a TestProcessor.
     */
    public TestProcessor() {
      super("TestProcessor");
      processed_events_ = new ArrayList<String>();
    }

    /**
     * Returns the list of processed events.
     *
     * @return The list of processed events.
     */
    public ArrayList<String> getProcessedEvents() {
      return processed_events_;
    }

    /**
     * Implements the event processing logic.
     * Appends processed events to the processed_events_ list.
     *
     * Once all pending events have been processed, this methods notifies any waiting threads.
     * This is used to synchronize the test thread with the processor thread.
     */
    @Override
    protected synchronized void process(String event_data) {
      processed_events_.add(event_data);
      if (getObserver().numPendingEvents() == 0) {
        notifyAll();
      }
    }

    private ArrayList<String> processed_events_;  // List of processed events.
  }

  /**
   * Tests processing of events.
   */
  @Test
  public void process() {
    TestProcessor processor = new TestProcessor();
    processor.start();
    Event<String> event1 = new Event<String>();
    event1.trigger("pre-subscribe");
    assertThat(processor.getProcessedEvents().size(), is(0));

    // subscribe to single event
    processor.subscribe(event1);
    event1.trigger("post-subscribe");
    synchronized (processor) {
      try {
        processor.wait();
      } catch (InterruptedException e) {}
    }
    assertThat(processor.getProcessedEvents().size(), is(1));
    assertThat(processor.getProcessedEvents().get(0), is("post-subscribe"));
    processor.getProcessedEvents().clear();
    event1.trigger("trigger1");
    event1.trigger("trigger2");
    event1.trigger("trigger3");
    synchronized (processor) {
      try {
        processor.wait();
      } catch (InterruptedException e) {}
    }
    assertThat(processor.getProcessedEvents().size(), is(3));
    assertThat(processor.getProcessedEvents().get(0), is("trigger1"));
    assertThat(processor.getProcessedEvents().get(1), is("trigger2"));
    assertThat(processor.getProcessedEvents().get(2), is("trigger3"));
    processor.getProcessedEvents().clear();

    // subscribe to multiple events
    Event<String> event2 = new Event<String>();
    processor.subscribe(event2);
    Event<String> event3 = new Event<String>();
    processor.subscribe(event3);
    event1.trigger("event-1-A");
    event2.trigger("event-2-A");
    event3.trigger("event-3-A");
    event1.trigger("event-1-B");
    synchronized (processor) {
      try {
        processor.wait();
      } catch (InterruptedException e) {}
    }
    assertThat(processor.getProcessedEvents().size(), is(4));
    assertThat(processor.getProcessedEvents().get(0), is("event-1-A"));
    assertThat(processor.getProcessedEvents().get(1), is("event-2-A"));
    assertThat(processor.getProcessedEvents().get(2), is("event-3-A"));
    assertThat(processor.getProcessedEvents().get(3), is("event-1-B"));
    processor.getProcessedEvents().clear();

    // unsubscribe
    processor.unsubscribe(event1);
    event1.trigger("event-1-C");
    event2.trigger("event-2-B");
    event3.trigger("event-3-B");
    synchronized (processor) {
      try {
        processor.wait();
      } catch (InterruptedException e) {}
    }
    assertThat(processor.getProcessedEvents().size(), is(2));
    assertThat(processor.getProcessedEvents().get(0), is("event-2-B"));
    assertThat(processor.getProcessedEvents().get(1), is("event-3-B"));
    processor.getProcessedEvents().clear();
    processor.unsubscribe(event2);
    event1.trigger("event-1-D");
    event2.trigger("event-2-C");
    event3.trigger("event-3-C");
    synchronized (processor) {
      try {
        processor.wait();
      } catch (InterruptedException e) {}
    }
    assertThat(processor.getProcessedEvents().size(), is(1));
    assertThat(processor.getProcessedEvents().get(0), is("event-3-C"));
    processor.getProcessedEvents().clear();
    processor.unsubscribe(event3);
    event1.trigger("event-1-E");
    event2.trigger("event-2-D");
    event3.trigger("event-3-D");
    // The test must yield rather than wait here, since the processor is unsubscribed from all
    // events and thus will not notify this thread via its process method.
    Thread.yield();
    assertThat(processor.getProcessedEvents().size(), is(0));

    // unsubscribe from unsubscribed event
    processor.unsubscribe(event1);

    // ensure that no events are processed after processor halts
    processor.subscribe(event1);
    event1.trigger("pre-halt");
    try {
      // allow processing of pre-halt event
      Thread.sleep(10);
    } catch (InterruptedException e) {}
    processor.halt();
    try {
      processor.join();
    } catch (InterruptedException e) {}
    event1.trigger("post-halt");
    assertThat(processor.getProcessedEvents().size(), is(1));
    assertThat(processor.getProcessedEvents().get(0), is("pre-halt"));
  }
}
