package com.capestartproject.workflow.api;

import java.util.Map;

import com.capestartproject.common.emppackage.EmployeePackage;

public class WorkflowOperationResultImpl implements WorkflowOperationResult {
	protected EmployeePackage resultingEmployeePackage;

  protected Map<String, String> properties;

  protected Action action;

  protected long timeInQueue;

  protected boolean wait;

  protected boolean isContinuable;

  protected boolean isAbortable;

  /**
   * No arg constructor needed by JAXB
   */
  public WorkflowOperationResultImpl() {
  }

  	/**
	 * Constructs a new WorkflowOperationResultImpl from a employeepackage and
	 * an action.
	 *
	 * @param resultingEmployeePackage
	 * @param action
	 */
	public WorkflowOperationResultImpl(EmployeePackage resultingEmployeePackage, Map<String, String> properties,
			Action action,
          long timeInQueue) {
		this.resultingEmployeePackage = resultingEmployeePackage;
    this.properties = properties;
    this.timeInQueue = timeInQueue;
    this.isAbortable = true;
    this.isContinuable = true;
    if (action == null) {
      throw new IllegalArgumentException("action must not be null.");
    } else {
      this.action = action;
    }
  }

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#getMediaPackage()
	 */
	
	public EmployeePackage getEmployeePackage() {
		return resultingEmployeePackage;
	}
	  
	 /**
	 * Sets the resulting media package.
	 *
	 * @param mediaPackage
	 */
	public void setMediaPackage(EmployeePackage employeePackage) {
		this.resultingEmployeePackage = employeePackage;
	}


  	/**
	 *
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#getAction()
	 */
  public Action getAction() {
    return action;
  }

  /**
   * Sets the action that the workflow service should take on the workflow instance
   *
   * @param action
   */
  public void setAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("action must not be null.");
    this.action = action;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#getProperties()
	 */
  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#getTimeInQueue()
	 */
  @Override
  public long getTimeInQueue() {
    return timeInQueue;
  }

  /**
   * Specifies whether the operation should be abortable by the user.
   *
   * @param isAbortable
   */
  public void setAllowsAbort(boolean isAbortable) {
    this.isAbortable = isAbortable;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#allowsAbort()
	 */
  @Override
  public boolean allowsAbort() {
    return isAbortable;
  }

  /**
   * Specifies whether the operation should be continuable by the user.
   *
   * @param isContinuable
   */
  public void setAllowsContinue(boolean isContinuable) {
    this.isContinuable = isContinuable;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationResult#allowsContinue()
	 */
  @Override
  public boolean allowsContinue() {
    return isContinuable;
  }

}
