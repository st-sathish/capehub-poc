package com.capestartproject.workflow.api;

import java.util.Map;

//import com.capestartproject.common.employeepackage.EmployeePackage;

/**
 * The result of a workflow operation.
 */
public interface WorkflowOperationResult {
  public enum Action {
    CONTINUE, PAUSE, SKIP
  }

  	/**
	 * @return The employee package that results from the execution of a
	 *         workflow operation.
	 */
	// EmployeePackage getEmployeePackage();

  /**
   * Operations may optionally set properties on a workflow operation.
   *
   * @return The properties to set
   */
  Map<String, String> getProperties();

  /**
   * Sets the action to take.
   *
   * @param action
   *          the action
   */
  void setAction(Action action);

  /**
   * Operations may optionally request that the workflow be placed in a certain state.
   *
   * @return The action that the workflow service should take on this workflow instance.
   */
  Action getAction();

  /**
   * Specifies whether the operation should be continuable by the user.
   *
   * @param isContinuable
   */
  void setAllowsContinue(boolean isContinuable);

  /**
   * Returns <code>true</code> if this operation can be continued by the user from an optional hold state. This value is
   * only considered if the action returned by this result equals {@link Action#PAUSE}.
   *
   * @return <code>true</code> if a paused operation should be continuable
   */
  boolean allowsContinue();

  /**
   * Specifies whether the operation should be abortable by the user.
   *
   * @param isAbortable
   */
  void setAllowsAbort(boolean isAbortable);

  /**
   * Returns <code>true</code> if this operation can be canceled by the user from an optional hold state. This value is
   * only considered if the action returned by this result equals {@link Action#PAUSE}.
   *
   * @return <code>true</code> if a paused operation should be abortable
   */
  boolean allowsAbort();

  /**
   * The number of milliseconds this operation sat in a queue before finishing.
   *
   * @return The time spent in a queue
   */
  long getTimeInQueue();

}
