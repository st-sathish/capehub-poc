package com.capestartproject.workflow.api;

/**
 * Exception that is thrown for failing database operations.
 */
public class WorkflowDatabaseException extends WorkflowException {

  /** Serial version uid */
  private static final long serialVersionUID = -7411693851983157126L;

  /**
   * Constructs a new workflow database exception without a message or a cause.
   */
  public WorkflowDatabaseException() {
  }

  /**
   * Constructs a new workflow database exception with a message.
   *
   * @param message
   *          the message describing the exception
   */
  public WorkflowDatabaseException(String message) {
    super(message);
  }

  /**
   * Constructs a new workflow database exception with the throwable causing this exception to be thrown.
   *
   * @param cause
   *          the cause of this exception
   */
  public WorkflowDatabaseException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new workflow database exception with a message and the throwable that caused this exception to be
   * thrown.
   *
   * @param message
   *          the message describing the exception
   * @param cause
   *          the cause of this exception
   */
  public WorkflowDatabaseException(String message, Throwable cause) {
    super(message, cause);
  }

}
