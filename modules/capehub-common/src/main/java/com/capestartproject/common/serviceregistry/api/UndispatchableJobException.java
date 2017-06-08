package com.capestartproject.common.serviceregistry.api;

/**
 * Exception that is thrown if a job is not dispatchable by any service that would normally accept this type of work.
 * <p>
 * The exception indicates that there may be something wrong with the job or that the job cannot be dispatched because
 * of related circumstances.
 */
public class UndispatchableJobException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = 5006552593095889618L;

  /**
   * Creates a new undispatchable job exception
   *
   * @param message
   *          the error message
   * @param t
   *          the exception causing the error
   */
  public UndispatchableJobException(String message, Throwable t) {
    super(message, t);
  }

  /**
   * Creates a new undispatchable job exception
   *
   * @param message
   *          the error message
   */
  public UndispatchableJobException(String message) {
    super(message);
  }

  /**
   * Creates a new undispatchable job exception
   *
   * @param t
   *          the exception causing the error
   */
  public UndispatchableJobException(Throwable t) {
    super(t);
  }

}
