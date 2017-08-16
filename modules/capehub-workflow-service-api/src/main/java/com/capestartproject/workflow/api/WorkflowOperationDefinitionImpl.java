package com.capestartproject.workflow.api;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * See {@link WorkflowOperationDefinition}
 */
@XmlType(name = "operation-definition", namespace = "http://workflow.capestartproject.com")
@XmlRootElement(name = "operation-definition", namespace = "http://workflow.capestartproject.com")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationDefinitionImpl implements WorkflowOperationDefinition {

  @XmlAttribute(name = "id")
  protected String id;

  @XmlAttribute(name = "description")
  protected String description;

  @XmlAttribute(name = "fail-on-error")
  protected boolean failWorkflowOnException;

  @XmlAttribute(name = "if")
  protected String executeCondition;

  @XmlAttribute(name = "unless")
  protected String skipCondition;

  @XmlAttribute(name = "exception-handler-workflow")
  protected String exceptionHandlingWorkflow;

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  protected Set<WorkflowConfiguration> configurations;

  @XmlAttribute(name = "max-attempts")
  protected int maxAttempts;

  @XmlJavaTypeAdapter(RetryStrategy.Adapter.class)
  @XmlAttribute(name = "retry-strategy")
  protected RetryStrategy retryStrategy;

  /** A no-arg constructor is needed by JAXB */
  public WorkflowOperationDefinitionImpl() {
    super();
    this.maxAttempts = 1;
    this.failWorkflowOnException = true;
    this.retryStrategy = RetryStrategy.NONE;
  }

  /**
   * @param id
   *          The unique name of this operation
   * @param description
   *          The description of what this operation does
   * @param exceptionHandlingWorkflow
   *          The workflow to run when a workflow operation exception is thrown
   * @param failWorkflowOnException
   *          Whether an exception thrown by this operation should fail the entire {@link WorkflowInstance}
   */
  public WorkflowOperationDefinitionImpl(String id, String description, String exceptionHandlingWorkflow,
          boolean failWorkflowOnException) {
    this();
    this.id = id;
    this.description = description;
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
    this.failWorkflowOnException = failWorkflowOnException;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setExecutionCondition(String condition) {
    this.executeCondition = condition;
  }

  public String getExecutionCondition() {
    return executeCondition;
  }

  public void setSkipCondition(String condition) {
    this.skipCondition = condition;
  }

  public String getSkipCondition() {
    return skipCondition;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationDefinition#getExceptionHandlingWorkflow()
	 */
  public String getExceptionHandlingWorkflow() {
    return exceptionHandlingWorkflow;
  }

  public void setExceptionHandlingWorkflow(String exceptionHandlingWorkflow) {
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationDefinition#isFailWorkflowOnException()
	 */
  public boolean isFailWorkflowOnException() {
    return failWorkflowOnException;
  }

  public void setFailWorkflowOnException(boolean failWorkflowOnException) {
    this.failWorkflowOnException = failWorkflowOnException;
  }

  /**
   * Allows JAXB handling of {@link WorkflowOperationDefinition} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowOperationDefinitionImpl, WorkflowOperationDefinition> {
    public WorkflowOperationDefinitionImpl marshal(WorkflowOperationDefinition op) throws Exception {
      return (WorkflowOperationDefinitionImpl) op;
    }

    public WorkflowOperationDefinition unmarshal(WorkflowOperationDefinitionImpl op) throws Exception {
      return op;
    }
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
	 */
  public String getConfiguration(String key) {
    if (key == null || configurations == null)
      return null;
    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key))
        return config.getValue();
    }
    return null;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
	 */
  public void removeConfiguration(String key) {
    if (key == null || configurations == null)
      return;
    for (Iterator<WorkflowConfiguration> configIter = configurations.iterator(); configIter.hasNext();) {
      WorkflowConfiguration config = configIter.next();
      if (config.getKey().equals(key)) {
        configIter.remove();
        return;
      }
    }
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String,
	 *      java.lang.String)
	 */
  public void setConfiguration(String key, String value) {
    if (key == null || configurations == null)
      return;
    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key)) {
        ((WorkflowConfigurationImpl) config).setValue(value);
        return;
      }
    }
    // No configurations were found, so add a new one
    configurations.add(new WorkflowConfigurationImpl(key, value));
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationDefinition#getConfigurationKeys()
	 */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> set = new TreeSet<String>();
    if (configurations != null) {
      for (WorkflowConfiguration config : configurations) {
        set.add(config.getKey());
      }
    }
    return set;
  }

  	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationDefinition#getRetryStrategy()
	 */
  @Override
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  /**
   * @param retryStrategy
   *          the retry strategy to set
   */
  public void setRetryStrategy(RetryStrategy retryStrategy) {
    this.retryStrategy = retryStrategy;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationDefinition#getMaxAttempts()
	 */
  @Override
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * @param maxAttempts
   *          the maxAttempts to set
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "operation definition:'" + id + "'";
  }

}
