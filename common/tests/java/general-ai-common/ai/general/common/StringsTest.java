/* General AI - Common
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.common;

import java.util.ArrayList;

import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests for {@link Strings} class.
 */
public class StringsTest {

  /**
   * Tests the joining strings.
   */
  @Test
  public void join() {
    ArrayList<String> array = new ArrayList<String>();
    assertThat(Strings.join(array, ","), is(""));
    array.add("one");
    assertThat(Strings.join(array, ","), is("one"));
    array.add("two");
    assertThat(Strings.join(array, ","), is("one,two"));
    array.add("three");
    assertThat(Strings.join(array, ","), is("one,two,three"));
    assertThat(Strings.join(array, "-"), is("one-two-three"));
    assertThat(Strings.join(array, "<->"), is("one<->two<->three"));
  }
}
