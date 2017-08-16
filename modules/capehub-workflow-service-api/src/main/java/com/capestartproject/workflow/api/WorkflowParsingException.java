package com.capestartproject.workflow.api;

/**
 * Exception that is thrown for failing database operations.
 */
public class WorkflowParsingException extends WorkflowException {


  /** Serial version uid */
  private static final long serialVersionUID = -8203912582435200347L;

  /**
   * Constructs a new workflow parsing exception without a message or a cause.
   */
  public WorkflowParsingException() {
  }

  /**
   * Constructs a new workflow parsing exception with a message.
   *
   * @param message
   *          the message describing the exception
   */
  public WorkflowParsingException(String message) {
    super(message);
  }

  /**
   * Constructs a new workflow parsing exception with the throwable causing this exception to be thrown.
   *
   * @param cause
   *          the cause of this exception
   */
  public WorkflowParsingException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new workflow parsing exception with a message and the throwable that caused this exception to be
   * thrown.
   *
   * @param message
   *          the message describing the exception
   * @param cause
   *          the cause of this exception
   */
  public WorkflowParsingException(String message, Throwable cause) {
    super(message, cause);
  }

}
