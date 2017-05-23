
package com.capehub.common.util;

/**
 * An exception that indicates that a resource that was expected to exist does not exist.
 */
public class NotFoundException extends Exception {

  private static final long serialVersionUID = -6781286820876007809L;

  /**
   * Constructs a NotFoundException without a detail message.
   */
  public NotFoundException() {
    super();
  }

  /**
   * Constructs a NotFoundException with a detail message.
   */
  public NotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a NotFoundException with a cause.
   */
  public NotFoundException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a NotFoundException with a detail message and a cause.
   */
  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
