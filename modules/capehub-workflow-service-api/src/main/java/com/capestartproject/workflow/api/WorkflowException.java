package com.capestartproject.workflow.api;

/**
 * The superclass for any workflow related exceptions.
 */
public class WorkflowException extends Exception {

  /** The java.io.serialization class version */
  private static final long serialVersionUID = 1220788011273100329L;

  /**
   * Constructs a new workflow exception without a message or a cause.
   */
  public WorkflowException() {
  }

  /**
   * Constructs a new workflow exception with a message.
   *
   * @param message
   *          the message describing the exception
   */
  public WorkflowException(String message) {
    super(message);
  }

  /**
   * Constructs a new workflow exception with the throwable causing this exception to be thrown.
   *
   * @param cause
   *          the cause of this exception
   */
  public WorkflowException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new workflow exception with a message and the throwable that caused this exception to be thrown.
   *
   * @param message
   *          the message describing the exception
   * @param cause
   *          the cause of this exception
   */
  public WorkflowException(String message, Throwable cause) {
    super(message, cause);
  }
}
