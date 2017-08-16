package com.capestartproject.workflow.api;

/**
 * A listener that is notified when the workflow service updates a workflow instance.
 */
public interface WorkflowListener {

  /**
   * Called when the current operation of a workflow has changed.
   *
   * @param workflow the workflow instance
   */
  void operationChanged(WorkflowInstance workflow);

  /**
   * Called when the state of a workflow instance has changed.
   *
   * @param workflow
   */
  void stateChanged(WorkflowInstance workflow);

}
