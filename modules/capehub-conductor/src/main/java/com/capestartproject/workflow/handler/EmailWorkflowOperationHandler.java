/**
 * 
 */
package com.capestartproject.workflow.handler;

import com.capestartproject.common.job.api.JobContext;
import com.capestartproject.workflow.api.AbstractWorkflowOperationHandler;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowOperationException;
import com.capestartproject.workflow.api.WorkflowOperationResult;

/**
 * @author S T
 *
 */
public class EmailWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.AbstractWorkflowOperationHandler#start(
	 * com.capestartproject.workflow.api.WorkflowInstance,
	 * com.capestartproject.common.job.api.JobContext)
	 */
	@Override
	public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
			throws WorkflowOperationException {
		// TODO Auto-generated method stub
		return null;
	}
}
