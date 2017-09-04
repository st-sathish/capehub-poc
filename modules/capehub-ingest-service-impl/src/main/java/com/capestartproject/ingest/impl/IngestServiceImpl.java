/**
 * 
 */
package com.capestartproject.ingest.impl;

import java.io.IOException;
import java.util.Map;

import com.capestartproject.common.emppackage.EmployeePackage;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.security.api.UnauthorizedException;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.UndispatchableJobException;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.ingest.api.IngestException;
import com.capestartproject.ingest.api.IngestService;
import com.capestartproject.workflow.api.WorkflowInstance;

/**
 * @author CS39
 *
 */
public class IngestServiceImpl implements IngestService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.JobProducer#getJobType()
	 */
	@Override
	public String getJobType() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.JobProducer#countJobs(com.
	 * capestartproject.common.job.api.Job.Status)
	 */
	@Override
	public long countJobs(Status status) throws ServiceRegistryException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.JobProducer#acceptJob(com.
	 * capestartproject.common.job.api.Job)
	 */
	@Override
	public void acceptJob(Job job) throws ServiceRegistryException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.job.api.JobProducer#isReadyToAcceptJobs(java.
	 * lang.String)
	 */
	@Override
	public boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.JobProducer#isReadyToAccept(com.
	 * capestartproject.common.job.api.Job)
	 */
	@Override
	public boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage employeePackage) throws IllegalStateException, IngestException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage, java.lang.String)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID)
			throws IllegalStateException, IngestException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage, java.lang.String, java.util.Map)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID,
			Map<String, String> properties) throws IllegalStateException, IngestException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage, java.lang.String, java.util.Map,
	 * java.lang.Long)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID,
			Map<String, String> properties, Long workflowId)
			throws IllegalStateException, IngestException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#discardEmployeePackage(com.
	 * capestartproject.common.emppackage.EmployeePackage)
	 */
	@Override
	public void discardEmployeePackage(EmployeePackage employeePackage) throws IOException, IngestException {
		// TODO Auto-generated method stub

	}

}
