package com.capestartproject.workflow.api;

/**
 * This exception is used if there is an error related to a WorkflowState
 */
public class WorkflowStateException extends WorkflowException {

  /** The serial version ID */
  private static final long serialVersionUID = -4118946974032043379L;

  /**
   * Constructs a new workflow state exception without a message or a cause.
   */
  public WorkflowStateException() {
  }

  /**
   * Constructs a new workflow state exception with a message.
   * 
   * @param message
   *          the message describing the exception
   */
  public WorkflowStateException(String message) {
    super(message);
  }

  /**
   * Constructs a new workflow state exception with the throwable causing this exception to be thrown.
   * 
   * @param cause
   *          the cause of this exception
   */
  public WorkflowStateException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new workflow state exception with a message and the throwable that caused this exception to be thrown.
   * 
   * @param message
   *          the message describing the exception
   * @param cause
   *          the cause of this exception
   */
  public WorkflowStateException(String message, Throwable cause) {
    super(message, cause);
  }

}
