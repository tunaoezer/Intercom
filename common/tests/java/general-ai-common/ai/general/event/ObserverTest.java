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
 * Tests for the {@link Observer} class.
 */
public class ObserverTest {

  /**
   * Tests observing of events.
   */
  @Test
  public void observe() {
    Observer<String> observer = new Observer<String>(3);
    assertThat(observer.numPendingEvents(), is(0));
    Assert.assertNull(observer.poll(0));
    Assert.assertNull(observer.poll(1));
    observer.event("event1");
    observer.event("event2");
    assertThat(observer.numPendingEvents(), is(2));
    assertThat(observer.poll(0), is("event1"));
    assertThat(observer.numPendingEvents(), is(1));
    observer.event("event3");
    assertThat(observer.poll(0), is("event2"));
    assertThat(observer.poll(0), is("event3"));
    assertThat(observer.numPendingEvents(), is(0));
    Assert.assertNull(observer.poll(0));
    observer.event("event4");
    observer.event("event5");
    observer.event("event6");
    assertThat(observer.numPendingEvents(), is(3));

    // exceed event queue limit
    observer.event("event7");
    assertThat(observer.numPendingEvents(), is(3));
    observer.clear();
    assertThat(observer.numPendingEvents(), is(0));
    Assert.assertNull(observer.poll(0));
    observer.event("event8");
    assertThat(observer.numPendingEvents(), is(1));
    assertThat(observer.poll(0), is("event8"));
  }
}
