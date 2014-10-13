/* General AI - Directory Test Support
 * Copyright (C) 2013 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.directory.test;

/**
 * Main test handler used by test classes.
 * TestHandler expects data of type String.
 */
public class TestHandler extends GenericTestHandler {

  /**
   * Constructs a regular TestHandler with the specified name.
   *
   * @param name Name of handler.
   */
  public TestHandler(String name) {
    this(name, false);
  }

  /**
   * Constructs a regular or catch-all TestHandler with the specified name.
   *
   * @param name Name of handler.
   * @param catch_all True if this a catch-all handler.
   */
  public TestHandler(String name, boolean catch_all) {
    super(name, catch_all);
  }

  /**
   * Returns the request argument at specified index.
   * Returns null if no request arguments have been captured, there is no request argument
   * at the specified index or the request argument at the specified index is not a string.
   *
   * @param index The request argument index.
   * @return The request argument at the specified index or null.
   */
  @Override
  public String getArgument(int index) {
    Object argument = super.getArgument(0);
    if (argument != null && argument instanceof String) {
      return (String) argument;
    } else {
      return null;
    }
  }
}
