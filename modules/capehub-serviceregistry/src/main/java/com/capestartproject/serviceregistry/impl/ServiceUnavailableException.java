package com.capestartproject.serviceregistry.impl;

/**
 * Exception to indicate general service unavailability.
 * <p>
 * This exception is usually thrown if a service is not ready to momentarily accept no work at all.
 */
public class ServiceUnavailableException extends Exception {

  /** The serial version UID */
  private static final long serialVersionUID = -4874687215095488910L;

  /**
   * Creates an exception with an error message.
   *
   * @param message
   *          the error message
   */
  public ServiceUnavailableException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a cause.
   *
   * @param cause
   *          the original cause for failure
   */
  public ServiceUnavailableException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates an exception with an error message and a cause.
   *
   * @param message
   *          the error message
   * @param cause
   *          the original cause
   */
  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

}
