package com.capestartproject.ingest.api;

import java.io.IOException;
import java.util.Map;

import com.capestartproject.common.emppackage.EmployeePackage;
import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.security.api.UnauthorizedException;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.workflow.api.WorkflowInstance;

/**
 * @author CS39
 *
 */
public interface IngestService extends JobProducer {

	String UTC_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";

	/**
	 * Ingests the employeepackage and starts the default workflow as defined by
	 * the <code>com.capestartproject.workflow.default.definition</code> key,
	 * found in the system configuration.
	 *
	 * @param mediaPackage
	 *            The specific Capehub EmployeePackage being ingested
	 * @return Workflow instance id.
	 * @throws IngestException
	 *             if an unexpected error occurs
	 */
	WorkflowInstance ingest(EmployeePackage employeePackage) throws IllegalStateException, IngestException;

	/**
	 * Ingests the employee and starts the workflow as defined by
	 * <code>workflowDefinitionID</code>.
	 *
	 * @param employeePackage
	 *            The specific Capehub EmployeePackage being ingested
	 * @param workflowDefinitionID
	 *            workflow to be used with this employee package
	 * @return Workflow instance id.
	 * @throws IngestException
	 *             if an unexpected error occurs
	 * @throws NotFoundException
	 *             if the workflow defintion can't be found
	 */
	WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID)
			throws IllegalStateException, IngestException, NotFoundException;

	/**
	 * Ingests the employeePackage and starts the workflow as defined by
	 * <code>workflowDefinitionID</code>. The properties specified in
	 * <code>properties</code> will be submitted as configuration data to the
	 * workflow.
	 *
	 * @param employeePackage
	 *            The specific Capehub EmployeePackage being ingested
	 * @param workflowDefinitionID
	 *            workflow to be used with this media package
	 * @param properties
	 *            configuration properties for the workflow
	 * @return Workflow instance id.
	 * @throws IngestException
	 *             if an unexpected error occurs
	 * @throws NotFoundException
	 *             if the workflow defintion can't be found
	 */
	WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID,
			Map<String, String> properties)
			throws IllegalStateException, IngestException, NotFoundException;

	/**
	 * Ingests the employeePackage and starts the workflow as defined by
	 * <code>workflowDefinitionID</code>. The properties specified in
	 * <code>properties</code> will be submitted as configuration data to the
	 * workflow.
	 * <p>
	 * The steps defined in that workflow will be appended to the already
	 * running workflow instance <code>workflowId</code>. If that workflow can't
	 * be found, a {@link NotFoundException} will be thrown. If the
	 * <code>workflowId</code> is null, a new {@link WorkflowInstance} is
	 * created.
	 *
	 * @param employeePackage
	 *            The specific Capehub EmployeePackage being ingested
	 * @param workflowDefinitionID
	 *            workflow to be used with this employee package
	 * @param properties
	 *            configuration properties for the workflow
	 * @param workflowId
	 *            the workflow identifier
	 * @return Workflow instance id.
	 * @throws IngestException
	 *             if an unexpected error occurs
	 * @throws NotFoundException
	 *             if either one of the workflow definition or workflow instance
	 *             was not found
	 * @throws UnauthorizedException
	 *             if the current user does not have {@link #READ_PERMISSION} on
	 *             the workflow instance's employeePackage.
	 */
	WorkflowInstance ingest(EmployeePackage employeePackage, String workflowDefinitionID,
			Map<String, String> properties,
			Long workflowId) throws IllegalStateException, IngestException, NotFoundException, UnauthorizedException;

	/**
	 * Delete an existing EmployeePackage and any linked files from the
	 * temporary ingest filestore.
	 *
	 * @param employeePackage
	 *            The specific Capehub EmployeePackage
	 * @throws IngestException
	 *             if an unexpected error occurs
	 */
	void discardEmployeePackage(EmployeePackage employeePackage) throws IOException, IngestException;
}
