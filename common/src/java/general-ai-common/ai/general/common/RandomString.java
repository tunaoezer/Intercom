/* General AI - Common
 * Copyright (C) 2014 Tuna Oezer, General AI.
 * See license.txt for copyright information.
 */

package ai.general.common;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Random string generator.
 *
 * The random strings are generated with a random source that is suitable for cryptographic use.
 */
public class RandomString {

  /**
   * Generates and returns a random string.
   *
   * The returned string never exceeds the specified length, but may be shorter.
   * With very high probability, the string will have the specified length.
   *
   * @param length The maximum length of the string.
   * @return A random string.
   */
  public static String nextString(int length) {
    return (new BigInteger(5 * length, rand)).toString(32);
  }

  private static Random rand = new SecureRandom();
}
