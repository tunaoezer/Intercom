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
 * Tests for the {@link Event} class.
 */
public class EventTest {

  /**
   * Tests subscribing, triggering and unsubscribing.
   */
  @Test
  public void trigger() {
    Event<String> event = new Event<String>();
    Observer<String> observer1 = new Observer<String>(3);
    Observer<String> observer2 = new Observer<String>(3);
    event.trigger("zero");
    assertThat(observer1.numPendingEvents(), is(0));
    assertThat(observer2.numPendingEvents(), is(0));
    event.subscribe(observer1);
    event.subscribe(observer2);
    event.trigger("one");
    assertThat(observer1.numPendingEvents(), is(1));
    assertThat(observer2.numPendingEvents(), is(1));
    event.trigger("two");
    assertThat(observer1.numPendingEvents(), is(2));
    assertThat(observer2.numPendingEvents(), is(2));
    assertThat(observer1.poll(0), is("one"));
    event.trigger("three");
    event.trigger("four");
    event.trigger("five");
    assertThat(observer1.poll(0), is("two"));
    assertThat(observer1.poll(0), is("three"));
    assertThat(observer1.poll(0), is("four"));
    Assert.assertNull(observer1.poll(0));
    assertThat(observer2.poll(0), is("one"));
    assertThat(observer2.poll(0), is("two"));
    assertThat(observer2.poll(0), is("three"));
    Assert.assertNull(observer2.poll(0));
  }
}
