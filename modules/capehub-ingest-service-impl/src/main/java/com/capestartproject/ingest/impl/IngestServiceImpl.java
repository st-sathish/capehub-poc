/**
 * 
 */
package com.capestartproject.ingest.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.management.ObjectInstance;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.emppackage.EmployeePackage;
import com.capestartproject.common.emppackage.EmployeePackageElement;
import com.capestartproject.common.job.api.AbstractJobProducer;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.TrustedHttpClient;
import com.capestartproject.common.security.api.UnauthorizedException;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.ingest.api.IngestException;
import com.capestartproject.ingest.api.IngestService;
import com.capestartproject.ingest.impl.jmx.IngestStatistics;
import com.capestartproject.workflow.api.WorkflowDatabaseException;
import com.capestartproject.workflow.api.WorkflowDefinition;
import com.capestartproject.workflow.api.WorkflowException;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowOperationInstance;
import com.capestartproject.workflow.api.WorkflowOperationInstance.OperationState;
import com.capestartproject.workflow.api.WorkflowService;
import com.capestartproject.workingfilerepository.api.WorkingFileRepository;

/**
 * @author CS39
 *
 */
public class IngestServiceImpl extends AbstractJobProducer implements IngestService {

	/** The logger */
	private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

	/** The configuration key that defines the default workflow definition */
	protected static final String WORKFLOW_DEFINITION_DEFAULT = "com.capestartproject.workflow.default.definition";

	public static final String JOB_TYPE = "com.capestartproject.ingest";

	/** The opencast service registry */
	private ServiceRegistry serviceRegistry;

	/** The authorization service */
	// private AuthorizationService authorizationService = null;

	/** The security service */
	protected SecurityService securityService = null;

	/** The user directory service */
	protected UserDirectoryService userDirectoryService = null;

	/** The organization directory service */
	protected OrganizationDirectoryService organizationDirectoryService = null;

	/** The scheduler service */
	// private SchedulerService schedulerService = null;

	/** The default workflow identifier, if one is configured */
	protected String defaultWorkflowDefinionId;

	/** The JMX business object for ingest statistics */
	private IngestStatistics ingestStatistics = new IngestStatistics();

	/** The JMX bean object instance */
	private ObjectInstance registerMXBean;

	/** The workflow service */
	private WorkflowService workflowService;

	/** The working file repository */
	private WorkingFileRepository workingFileRepository;

	/** The http client */
	private TrustedHttpClient httpClient;

	/**
	 * Creates a new ingest service instance.
	 */
	public IngestServiceImpl() {
		super(JOB_TYPE);
	}

	/**
	 * The formatter for reading in dates provided by the rest wrapper around
	 * this service
	 */
	protected DateFormat formatter = new SimpleDateFormat(UTC_DATE_FORMAT);

	/**
	 * OSGI callback for activating this component
	 *
	 * @param cc
	 *            the osgi component context
	 */
	protected void activate(ComponentContext cc) {
		logger.info("Ingest Service started.");
		defaultWorkflowDefinionId = StringUtils
				.trimToNull(cc.getBundleContext().getProperty(WORKFLOW_DEFINITION_DEFAULT));
		if (defaultWorkflowDefinionId == null) {
			logger.info("No default workflow definition specified. Ingest operations without a specified workflow "
					+ "definition will fail");
		}
		registerMXBean = JmxUtil.registerMXBean(ingestStatistics, "IngestStatistics");
	}

	/**
	 * Callback from OSGi on service deactivation.
	 */
	public void deactivate() {
		JmxUtil.unregisterMXBean(registerMXBean);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage ep) throws IllegalStateException, IngestException {
		try {
			return ingest(ep, null, null, null);
		} catch (NotFoundException e) {
			throw new IngestException(e);
		} catch (UnauthorizedException e) {
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage, java.lang.String)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage ep, String wd)
			throws IllegalStateException, IngestException, NotFoundException {
		try {
			return ingest(ep, wd, null, null);
		} catch (UnauthorizedException e) {
			throw new IllegalStateException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#ingest(com.capestartproject
	 * .common.emppackage.EmployeePackage, java.lang.String, java.util.Map)
	 */
	@Override
	public WorkflowInstance ingest(EmployeePackage ep, String wd,
			Map<String, String> properties) throws IllegalStateException, IngestException, NotFoundException {
		try {
			return ingest(ep, wd, properties, null);
		} catch (UnauthorizedException e) {
			throw new IllegalStateException(e);
		}
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
	public WorkflowInstance ingest(EmployeePackage ep, String workflowDefinitionId, Map<String, String> properties,
			Long workflowInstanceId)
			throws IllegalStateException, IngestException, NotFoundException, UnauthorizedException {
		// Done, update the job status and return the created workflow instance
		if (workflowInstanceId != null) {
			logger.info("Resuming workflow {} with ingested employeepackage {}", workflowInstanceId, ep);
		} else if (workflowDefinitionId == null) {
			logger.info(
					"Starting a new workflow with ingested employeepackage {} based on the default workflow definition '{}'",
					ep, defaultWorkflowDefinionId);
		} else {
			logger.info("Starting a new workflow with ingested employeepackage {} based on workflow definition '{}'",
					ep,
					workflowDefinitionId);
		}

		try {
			// Look for the workflow instance (if provided)
			WorkflowInstance workflow = null;
			if (workflowInstanceId != null) {
				try {
					workflow = workflowService.getWorkflowById(workflowInstanceId.longValue());
				} catch (NotFoundException e) {
					logger.warn("Failed to find a workflow with id '{}'", workflowInstanceId);
				}
			}

			// Determine the workflow definition
			WorkflowDefinition workflowDef = getWorkflowDefinition(workflowDefinitionId, workflowInstanceId, ep);

			// Get the final set of workflow properties
			properties = mergeWorkflowConfiguration(properties, workflowInstanceId);

			// If the indicated workflow does not exist, start a new workflow
			// with the given workflow definition
			if (workflow == null) {
				// setPublicAclIfEmpty(ep);
				ingestStatistics.successful();
				if (workflowDef != null) {
					logger.info(
							"Starting new workflow with ingested employeepackage '{}' using the specified template '{}'",
							ep.getIdentifier().toString(), workflowDefinitionId);
				} else {
					logger.info(
							"Starting new workflow with ingested employeepackage '{}' using the default template '{}'",
							ep.getIdentifier().toString(), defaultWorkflowDefinionId);
				}
				return workflowService.start(workflowDef, ep, properties);
			}

			// Make sure the workflow is in an acceptable state to be continued.
			// If not, start over, but use the workflow
			// definition and recording properties from the original workflow,
			// unless provided by the ingesting parties
			boolean startOver = verifyWorkflowState(workflow);

			WorkflowInstance workflowInstance;

			// Is it ok to go with the given workflow or do we need to start
			// over?
			if (startOver) {
				workflowInstance = workflowService.start(workflowDef, ep, properties);
			} else {
				WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
				if (currentOperation == null) {
					ingestStatistics.failed();
					throw new IllegalStateException(
							workflow + " has no current operation, so can not be resumed with a new employeepackage");
				}
				String currentOperationTemplate = currentOperation.getTemplate();
				// if
				// (!Arrays.asList(PRE_PROCESSING_OPERATIONS).contains(currentOperationTemplate))
				// {
				// ingestStatistics.failed();
				// throw new IllegalStateException(workflow + " is already in
				// operation " + currentOperationTemplate
				// + ", so we can not ingest");
				// }

				int preProcessingOperations = workflow.getOperations().size();

				// Merge the current employeepackage with the new one
				EmployeePackage existingEmployeePackage = workflow.getEmployeePackage();
				for (EmployeePackageElement element : ep.getElements()) {
					existingEmployeePackage.add(element);
				}

				// setPublicAclIfEmpty(ep);

				// Extend the workflow operations
				workflow.extend(workflowDef);

				// Advance the workflow
				int currentPosition = workflow.getOperations().indexOf(currentOperation);
				while (currentPosition < preProcessingOperations - 1) {
					currentOperation = workflow.getCurrentOperation();
					logger.debug("Advancing workflow (skipping {})", currentOperation);
					if (currentOperation.getId() != null) {
						try {
							Job job = serviceRegistry.getJob(currentOperation.getId());
							job.setStatus(Status.FINISHED);
							serviceRegistry.updateJob(job);
						} catch (ServiceRegistryException e) {
							ingestStatistics.failed();
							throw new IllegalStateException(
									"Error updating job associated with skipped operation " + currentOperation, e);
						}
					}
					currentOperation = workflow.next();
					currentPosition++;
				}

				// Ingest succeeded
				currentOperation.setState(OperationState.SUCCEEDED);

				// Update
				workflowService.update(workflow);

				// resume the workflow
				workflowInstance = workflowService.resume(workflowInstanceId.longValue(), properties);
			}

			ingestStatistics.successful();

			// Return the updated workflow instance
			return workflowInstance;
		} catch (WorkflowException e) {
			ingestStatistics.failed();
			throw new IngestException(e);
		}
	}

//	private void setPublicAclIfEmpty(EmployeePackage ep) {
//		AccessControlList activeAcl = authorizationService.getActiveAcl(ep).getA();
//		if (activeAcl.getEntries().size() == 0) {
//			String anonymousRole = securityService.getOrganization().getAnonymousRole();
//			activeAcl = new AccessControlList(new AccessControlEntry(anonymousRole, "read", true));
//			authorizationService.setAcl(ep, AclScope.Series, activeAcl);
//		}
//	}

	private Map<String, String> mergeWorkflowConfiguration(Map<String, String> properties, Long workflowId) {
		return properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.ingest.api.IngestService#discardEmployeePackage(com.
	 * capestartproject.common.emppackage.EmployeePackage)
	 */
	@Override
	public void discardEmployeePackage(EmployeePackage ep) throws IOException, IngestException {
		String empPackageId = ep.getIdentifier().compact();
		for (EmployeePackageElement element : ep.getElements()) {
			if (!workingFileRepository.delete(empPackageId, element.getIdentifier()))
				logger.warn("Unable to find (and hence, delete), this employeepackage element");
		}
	}

	private WorkflowDefinition getWorkflowDefinition(String workflowDefinitionID, Long workflowId,
			EmployeePackage employeepackage) throws NotFoundException, WorkflowDatabaseException, IngestException {
		// If the workflow definition and instance ID are null, use the default,
		// or throw if there is none
		if (StringUtils.isBlank(workflowDefinitionID)) {

		} else {
			logger.info(
					"Ingested employeepackage {} is processed using workflow template '{}', specified during ingest",
					employeepackage, workflowDefinitionID);
		}

		// Use the default workflow definition if nothing was determined
		if (StringUtils.isBlank(workflowDefinitionID) && defaultWorkflowDefinionId != null) {
			logger.info("Using default workflow definition '{}' to process ingested employeepackage {}",
					defaultWorkflowDefinionId, employeepackage);
			workflowDefinitionID = defaultWorkflowDefinionId;
		}

		// Have we been able to find a workflow definition id?
		if (StringUtils.isBlank(workflowDefinitionID)) {
			ingestStatistics.failed();
			throw new IllegalStateException(
					"Can not ingest a workflow without a workflow definition or an existing instance. No default definition is specified");
		}

		// Let's make sure the workflow definition exists
		WorkflowDefinition workflowDef = workflowService.getWorkflowDefinitionById(workflowDefinitionID);
		if (workflowDef == null)
			throw new IngestException("Workflow definition '" + workflowDefinitionID + "' does not exist anymore");

		return workflowDef;
	}

	private boolean verifyWorkflowState(WorkflowInstance workflow) {
		if (workflow != null) {
			switch (workflow.getState()) {
			case FAILED:
			case FAILING:
			case STOPPED:
				logger.info("The workflow with id '{}' is failed, starting a new workflow for this recording",
						workflow.getId());
				return true;
			case SUCCEEDED:
				logger.info("The workflow with id '{}' already succeeded, starting a new workflow for this recording",
						workflow.getId());
				return true;
			case RUNNING:
				logger.info("The workflow with id '{}' is already running, starting a new workflow for this recording",
						workflow.getId());
				return true;
			case INSTANTIATED:
			case PAUSED:
				// This is the expected state
			default:
				break;
			}
		}
		return false;
	}

	/**
	 * Sets the trusted http client
	 *
	 * @param httpClient
	 *            the http client
	 */
	public void setHttpClient(TrustedHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Sets the service registry
	 *
	 * @param serviceRegistry
	 *            the serviceRegistry to set
	 */
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	/**
	 * Sets the authorization service
	 *
	 * @param authorizationService
	 *            the authorization service to set
	 */
	// public void setAuthorizationService(AuthorizationService
	// authorizationService) {
	// this.authorizationService = authorizationService;
	// }

	public void setWorkflowService(WorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	public void setWorkingFileRepository(WorkingFileRepository workingFileRepository) {
		this.workingFileRepository = workingFileRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.AbstractJobProducer#
	 * getServiceRegistry()
	 */
	@Override
	protected ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.AbstractJobProducer#
	 * getSecurityService()
	 */
	@Override
	protected SecurityService getSecurityService() {
		return securityService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.AbstractJobProducer#
	 * getUserDirectoryService()
	 */
	@Override
	protected UserDirectoryService getUserDirectoryService() {
		return userDirectoryService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.AbstractJobProducer#
	 * getOrganizationDirectoryService()
	 */
	@Override
	protected OrganizationDirectoryService getOrganizationDirectoryService() {
		return organizationDirectoryService;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.job.api.AbstractJobProducer#process(com.
	 * capestartproject.common.job.api.Job)
	 */
	@Override
	protected String process(Job job) throws Exception {
		throw new IllegalStateException("Ingest jobs are not expected to be dispatched");
	}
}
