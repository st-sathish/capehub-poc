package com.capestartproject.workflow.impl;

import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.workflow.api.WorkflowDatabaseException;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowInstance.WorkflowState;
import com.capestartproject.workflow.api.WorkflowQuery;
import com.capestartproject.workflow.api.WorkflowSet;
import com.capestartproject.workflow.api.WorkflowStatistics;

/**
 * Provides persistence services to the workflow service implementation.
 */
public interface WorkflowServiceIndex {

  /**
   * Update the workflow instance, or add it to persistence if it is not already stored.
   *
   * @param instance
   *          The workflow instance to store
   * @throws WorkflowDatabaseException
   *           if there is a problem storing the workflow instance
   */
  void update(WorkflowInstance instance) throws WorkflowDatabaseException;

  /**
   * Remove the workflow instance with this id.
   *
   * @param id
   *          The workflow instance id
   *          @return the job that was set to deleted
   * @throws WorkflowDatabaseException
   *           if there is a problem removing the workflow instance from persistence
   * @throws NotFoundException
   *           if there is no workflow instance with this identifier
   */
  void remove(long id) throws WorkflowDatabaseException, NotFoundException;

  /**
   * Gets the total number of workflows that have been created to date.
   *
   * @param state
   *          the workflow state
   * @param operation
   *          the current operation identifier
   * @return The number of workflow instances, regardless of their state
   * @throws WorkflowDatabaseException
   *           if there is a problem retrieving the workflow instance count from persistence
   */
  long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException;

  /**
   * Gets a set of workflow instances using a custom query
   *
   * @param query
   *          the query to use in the search for workflow instances
   * @param action TODO
   * @param applyPermissions TODO
   *
   * @return the set of matching workflow instances
   * @throws WorkflowDatabaseException
   *           if there is a problem retrieving the workflow instances from persistence
   */
  WorkflowSet getWorkflowInstances(WorkflowQuery query, String action, boolean applyPermissions) throws WorkflowDatabaseException;

  /**
   * Returns the workflow statistics.
   *
   * @return workflow statistics
   * @throws WorkflowDatabaseException
   *           if there is a problem accessing the workflow instances in persistence
   */
  WorkflowStatistics getStatistics() throws WorkflowDatabaseException;

}
