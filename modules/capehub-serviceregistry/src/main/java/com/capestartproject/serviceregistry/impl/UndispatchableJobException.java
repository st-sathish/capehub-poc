package com.capestartproject.serviceregistry.impl;

/**
 * Exception that is thrown if a job is not dispatchable by any service that would normally accept this type of work.
 * <p>
 * The exception indicates that there may be something wrong with the job or that the job cannot be dispatched because
 * of related circumstances.
 */
public class UndispatchableJobException extends Exception {

  /** The serial version UID */
  private static final long serialVersionUID = 6255027328266035849L;

  /**
   * Creates an exception with an error message.
   *
   * @param message
   *          the error message
   */
  public UndispatchableJobException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a cause.
   *
   * @param cause
   *          the original cause for failure
   */
  public UndispatchableJobException(Throwable cause) {
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
  public UndispatchableJobException(String message, Throwable cause) {
    super(message, cause);
  }

}
