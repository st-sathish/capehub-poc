package com.capestartproject.common.security.api;

/**
 * An exception that indicates that a stream did not contain properly formatted access control list.
 */
public class AccessControlParsingException extends Exception {

  /** The java.io serialization UID */
  private static final long serialVersionUID = 8131459988616958901L;

  /**
   * Constructs a new exception from a specific cause.
   */
  public AccessControlParsingException(Throwable cause) {
    super(cause);
  }
}
