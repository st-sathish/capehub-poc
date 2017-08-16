package com.capestartproject.workflow.api;

import java.util.SortedMap;

import com.capestartproject.common.job.api.JobContext;

/**
 * Handler for workflow operations.
 */
public interface WorkflowOperationHandler {

  /**
   * The identifier used to map a workflow operation to its handler
   *
   * @return This handler's identifier
   */
  String getId();

  /**
   * Returns a description of what this handler does.
   *
   * @return The handler's description
   */
  String getDescription();

  /**
   * Returns the configuration keys that this handler accepts, along with a description of their purpose.
   *
   * @return The configuration keys and their meaning
   */
  SortedMap<String, String> getConfigurationOptions();

  /**
   * Runs the workflow operation on this {@link WorkflowInstance}. If the execution fails for some reason, this must
   * throw a {@link WorkflowOperationException} in order to handle the problem gracefully. Runtime exceptions will cause
   * the entire workflow instance to fail.
   *
   * @param workflowInstance
   *          the workflow instance
   * @param context
   *          the job context
   * @return the {@link WorkflowOperationResult} containing a potentially modified MediaPackage and whether to put the
   *         workflow instance into a wait state.
   *
   * @throws WorkflowOperationException
   *           If the workflow operation fails to execute properly, and the default error handling should be invoked.
   */
  WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException;

  /**
   * Skips the workflow operation on this {@link WorkflowInstance}. If the execution fails for some reason, this must
   * throw a {@link WorkflowOperationException} in order to handle the problem gracefully. Runtime exceptions will cause
   * the entire workflow instance to fail.
   *
   * @param workflowInstance
   *          the workflow instance
   * @param context
   *          the job context
   * @return TODO
   * @throws WorkflowOperationException
   *           If the workflow operation fails to execute properly, and the default error handling should be invoked.
   */
  WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException;

  /**
   * Clean up after a workflow operation finishes
   *
   * @param workflowInstance
   *          The workflow instance
   * @param context
   *          the job context
   * @throws WorkflowOperationException
   *           If the workflow operation fails to clean up properly.
   */
  void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException;

}
