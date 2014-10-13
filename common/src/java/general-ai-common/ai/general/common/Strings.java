/* General AI - Common
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.common;

import java.util.ArrayList;

/**
 * Common string routines.
 */
public class Strings {

  /**
   * Joins an ArrayList of strings using the specified delimiter.
   *
   * @param array ArrayList of strings to join.
   * @param delimeter Delmiter to separate the strings.
   * @return Joined string.
   */
  public static String join(ArrayList<String> array, String delimeter) {
    if (array.size() == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder(array.get(0));
    for (int i = 1; i < array.size(); i++) {
      result.append(delimeter);
      result.append(array.get(i));
    }
    return result.toString();
  }
}
