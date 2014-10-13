/* General AI - Directory Test Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory.test;

import ai.general.directory.Request;
import ai.general.net.Uri;

import org.junit.Assert;

/**
 * Collection of common utility methods for tests.
 */
public class TestUtilities {

  /**
   * Helper method to create requests. This method fails the test when an exception is encountered.
   *
   * @param uri Request URI string.
   * @param arguments Request arguments.
   * @return A request for the specified URI and with the specified data. Null on error.
   */
  public static Request createRequest(String uri, Object ... arguments) {
    try {
      return new Request(new Uri(uri), arguments);
    } catch (IllegalArgumentException e) {
      Assert.fail(e.toString());
      return null;
    }
  }
}
