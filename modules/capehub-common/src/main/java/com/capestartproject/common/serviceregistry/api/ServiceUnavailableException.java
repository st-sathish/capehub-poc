package com.capestartproject.common.serviceregistry.api;

/**
 * Exception that is thrown during service lookups.
 */
public class ServiceUnavailableException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = -5986792578531437980L;

  /**
   * Creates a new service unavailable exception.
   *
   * @param message
   *          the error message
   * @param t
   *          the exception causing the error
   */
  public ServiceUnavailableException(String message, Throwable t) {
    super(message, t);
  }

  /**
   * Creates a new service unavailable exception.
   *
   * @param message
   *          the error message
   */
  public ServiceUnavailableException(String message) {
    super(message);
  }

  /**
   * Creates a new service unavailable exception.
   *
   * @param t
   *          the exception causing the error
   */
  public ServiceUnavailableException(Throwable t) {
    super(t);
  }

}
