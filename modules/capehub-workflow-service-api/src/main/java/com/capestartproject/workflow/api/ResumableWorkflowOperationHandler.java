package com.capestartproject.workflow.api;

import java.util.Map;

import com.capestartproject.common.job.api.JobContext;

/**
 * A {@link WorkflowOperationHandler} that is allowed to return Action.PAUSE to pause (and later resume) a workflow.
 */
public interface ResumableWorkflowOperationHandler extends WorkflowOperationHandler {

  /**
   * Continues a suspended {@link WorkflowInstance}. If the execution fails for some reason, this must throw a
   * {@link WorkflowOperationException} in order to handle the problem gracefully. Runtime exceptions will cause the
   * entire workflow instance to fail.
   *
   * If the workflow instance is not in a suspended state, this method should throw an {@link IllegalStateException}.
   *
   * @param workflowInstance
   *          The workflow instance
   * @param context
   *          the job context
   * @param properties
   *          The properties added while the operation was on hold
   * @return the result of this operation
   * @throws WorkflowOperationException
   *           If the workflow operation fails to execute properly.
   */
  WorkflowOperationResult resume(WorkflowInstance workflowInstance, JobContext context, Map<String, String> properties)
          throws WorkflowOperationException;

  /**
   * Whether this operation handler will always pause. The workflow service may give preferential dispatching to
   * operations that are guaranteed to pause.
   *
   * @return whether this handler always pauses
   */
  boolean isAlwaysPause();

  /**
   * Gets the URL for the user interface for resuming the workflow.
   *
   * @param workflowInstance
   *          The workflow instance
   * @return The URL for the user interface
   * @throws WorkflowOperationException
   *           If the url to the hold state ui can't be created
   */
  String getHoldStateUserInterfaceURL(WorkflowInstance workflowInstance) throws WorkflowOperationException;

  /**
   * Returns the title for the link to this operations hold state UI.
   *
   * @return title to be displayed
   */
  String getHoldActionTitle();

}
