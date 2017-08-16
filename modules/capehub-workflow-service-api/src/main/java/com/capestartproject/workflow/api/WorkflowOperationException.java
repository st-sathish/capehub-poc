package com.capestartproject.workflow.api;

/**
 * Thrown when a {@link WorkflowOperationInstance} fails to run.
 */
public class WorkflowOperationException extends Exception {

  private static final long serialVersionUID = 5840096157653799867L;

  /**
   * Constructs a new {@link WorkflowOperationException} with a message and a root cause.
   *
   * @param message
   *          The message describing what went wrong
   * @param cause
   *          The exception that triggered this problem
   */
  public WorkflowOperationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new {@link WorkflowOperationException} with a message, but no root cause.
   *
   * @param message
   *          The message describing what went wrong
   */
  public WorkflowOperationException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link WorkflowOperationException} with a root cause.
   *
   * @param cause
   *          The exception that caused this problem
   */
  public WorkflowOperationException(Throwable cause) {
    super(cause);
  }

}
