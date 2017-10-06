package com.capestartproject.workflow.api;

import java.util.List;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.User;

/**
 * An single instance of a running, paused, or stopped workflow. WorkflowInstance objects are snapshots in time for a
 * particular workflow. They are not thread-safe, and will not be updated by other threads.
 */

public interface WorkflowInstance extends Configurable {
  enum WorkflowState {
    INSTANTIATED, RUNNING, STOPPED, PAUSED, SUCCEEDED, FAILED, FAILING
  }

  /**
   * The unique ID of this {@link WorkflowInstance}.
   */
  long getId();

  /**
   * Sets the workflow identifier.
   *
   * @param id
   *          the identifier
   */
  void setId(long id);

  /**
   * The short title of the workflow definition used to create this workflow instance.
   */
  String getTitle();

  /**
   * The identifier of the workflow definition used to create this workflow instance.
   */
  String getTemplate();

  /**
   * The longer description of the workflow definition used to create this workflow instance.
   */
  String getDescription();

  /**
   * The parent workflow instance ID, if any.
   */
  Long getParentId();

  /**
   * Returns the user that created this workflow.
   *
   * @return the workflow's creator
   */
  User getCreator();

  /**
   * Returns the organization that this workflow belongs to.
   *
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Returns a copy of the {@link WorkflowOperationInstance}s that make up this workflow. In order to modify the
   * operations, call setOperations.
   *
   * @return the workflow operations
   */
  List<WorkflowOperationInstance> getOperations();

  /**
   * Sets the list of workflow operations.
   *
   * @param operations
   *          the new list of operations
   */
  void setOperations(List<WorkflowOperationInstance> operations);

  /**
   * Returns the {@link WorkflowOperationInstance} that is currently either in {@link WorkflowState#RUNNING} or
   * {@link WorkflowState#PAUSED}.
   *
   * @return the current operation
   * @throws IllegalStateException
   *           if the workflow instance has no operations
   */
  WorkflowOperationInstance getCurrentOperation() throws IllegalStateException;

  /**
   * The current {@link WorkflowState} of this {@link WorkflowInstance}.
   */
  WorkflowState getState();

  /**
   * Set the state of the workflow.
   *
   * @param state
   *          the new workflow state
   */
  void setState(WorkflowState state);

	/**
	 * Returns the next operation, and marks it as current. If there is no next
	 * operation, this method will return null.
	 */
  WorkflowOperationInstance next();

  /**
   * Return whether there is another operation after the current operation. If there is no next operation, this will
   * return null.
   */
  boolean hasNext();

	/**
	 * Appends the operations found in the workflow definition to the end of
	 * this workflow instance.
	 *
	 * @param workflowDefinition
	 *            the workflow definition
	 */
  void extend(WorkflowDefinition workflowDefinition);
}
