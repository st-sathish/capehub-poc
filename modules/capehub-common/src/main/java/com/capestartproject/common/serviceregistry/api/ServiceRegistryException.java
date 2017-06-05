package com.capestartproject.common.serviceregistry.api;

/**
 * Exception that is thrown during service lookups.
 */
public class ServiceRegistryException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = 5006552593095889618L;

  /**
   * Creates a new service registry exception.
   *
   * @param message
   *          the error message
   * @param t
   *          the exception causing the error
   */
  public ServiceRegistryException(String message, Throwable t) {
    super(message, t);
  }

  /**
   * Creates a new service registry exception.
   *
   * @param message
   *          the error message
   */
  public ServiceRegistryException(String message) {
    super(message);
  }

  /**
   * Creates a new service registry exception.
   *
   * @param t
   *          the exception causing the error
   */
  public ServiceRegistryException(Throwable t) {
    super(t);
  }

}
