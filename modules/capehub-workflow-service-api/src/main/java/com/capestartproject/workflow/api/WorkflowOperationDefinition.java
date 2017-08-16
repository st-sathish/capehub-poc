package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Describes an operation or action to be performed as part of a workflow.
 */
@XmlJavaTypeAdapter(WorkflowOperationDefinitionImpl.Adapter.class)
public interface WorkflowOperationDefinition extends Configurable {

  String getId();

  String getDescription();

  /** The workflow to run if an exception is thrown while this operation is running. */
  String getExceptionHandlingWorkflow();

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  boolean isFailWorkflowOnException();

  /**
   * Returns either <code>null</code> or <code>true</code> to have the operation executed. Any other value is
   * interpreted as <code>false</code> and will skip the operation.
   * <p>
   * Usually, this will be a variable name such as <code>${foo}</code>, which will be replaced with its acutal value
   * once the workflow is executed.
   * <p>
   * If both <code>getExecuteCondition()</code> and <code>getSkipCondition</code> return a non-null value, the execute
   * condition takes precedence.
   *
   * @return the excecution condition.
   */
  String getExecutionCondition();

  /**
   * Returns either <code>null</code> or <code>true</code> to have the operation skipped. Any other value is interpreted
   * as <code>false</code> and will execute the operation.
   * <p>
   * Usually, this will be a variable name such as <code>${foo}</code>, which will be replaced with its actual value
   * once the workflow is executed.
   * <p>
   * If both <code>getExecuteCondition()</code> and <code>getSkipCondition</code> return a non-null value, the execute
   * condition takes precedence.
   *
   * @return the excecution condition.
   */
  String getSkipCondition();

  /**
   * Return the retry strategy
   *
   * @return the retry strategy
   */
  RetryStrategy getRetryStrategy();

  /**
   * Returns the number of attempts the workflow service will make to execute this operation.
   *
   * @return the maximum number of retries before failing
   */
  int getMaxAttempts();
}
