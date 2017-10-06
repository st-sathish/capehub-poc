package com.capestartproject.workflow.api;

import java.util.List;
import java.util.Set;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.User;

public class WorkflowInstanceImpl implements WorkflowInstance {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.Configurable#getConfiguration(java.lang
	 * .String)
	 */
	@Override
	public String getConfiguration(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.Configurable#setConfiguration(java.lang
	 * .String, java.lang.String)
	 */
	@Override
	public void setConfiguration(String key, String value) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.Configurable#getConfigurationKeys()
	 */
	@Override
	public Set<String> getConfigurationKeys() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.Configurable#removeConfiguration(java.
	 * lang.String)
	 */
	@Override
	public void removeConfiguration(String key) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getId()
	 */
	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#setId(long)
	 */
	@Override
	public void setId(long id) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getTitle()
	 */
	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getTemplate()
	 */
	@Override
	public String getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getParentId()
	 */
	@Override
	public Long getParentId() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getCreator()
	 */
	@Override
	public User getCreator() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getOrganization()
	 */
	@Override
	public Organization getOrganization() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getOperations()
	 */
	@Override
	public List<WorkflowOperationInstance> getOperations() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowInstance#setOperations(java.
	 * util.List)
	 */
	@Override
	public void setOperations(List<WorkflowOperationInstance> operations) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowInstance#getCurrentOperation()
	 */
	@Override
	public WorkflowOperationInstance getCurrentOperation() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#getState()
	 */
	@Override
	public WorkflowState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#setState(com.
	 * capestartproject.workflow.api.WorkflowInstance.WorkflowState)
	 */
	@Override
	public void setState(WorkflowState state) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#next()
	 */
	@Override
	public WorkflowOperationInstance next() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#hasNext()
	 */
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowInstance#extend(com.
	 * capestartproject.workflow.api.WorkflowDefinition)
	 */
	@Override
	public void extend(WorkflowDefinition workflowDefinition) {
		// TODO Auto-generated method stub

	}
}
