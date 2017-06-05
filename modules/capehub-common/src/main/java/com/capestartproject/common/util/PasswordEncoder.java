package com.capestartproject.common.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * A password encoder that md5 hashes a password with a salt.
 */
public final class PasswordEncoder {

  /**
   * Private constructor to disallow construction of this utility class.
   */
  private PasswordEncoder() {
  }

  /**
   * Encode a clear text password.
   *
   * @param clearText
   *          the password
   * @param salt
   *          the salt. See {@link http://en.wikipedia.org/wiki/Salt_%28cryptography%29}
   * @return the encoded password
   * @throws IllegalArgumentException
   *           if clearText or salt are null
   */
  public static String encode(String clearText, Object salt) throws IllegalArgumentException {
    if (clearText == null || salt == null)
      throw new IllegalArgumentException("clearText and salt must not be null");
    return DigestUtils.md5Hex(clearText + "{" + salt.toString() + "}");
  }

}
