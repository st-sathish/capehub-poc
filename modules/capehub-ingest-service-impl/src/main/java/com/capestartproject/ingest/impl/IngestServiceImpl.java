/**
 * 
 */
package com.capestartproject.ingest.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.management.ObjectInstance;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.AbstractJobProducer;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.TrustedHttpClient;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.ingest.api.IngestService;
import com.capestartproject.ingest.impl.jmx.IngestStatistics;
import com.capestartproject.workflow.api.WorkflowInstance;
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

	/** The capehub service registry */
	private ServiceRegistry serviceRegistry;

	/** The security service */
	protected SecurityService securityService = null;

	/** The user directory service */
	protected UserDirectoryService userDirectoryService = null;

	/** The organization directory service */
	protected OrganizationDirectoryService organizationDirectoryService = null;

	/** The default workflow identifier, if one is configured */
	protected String defaultWorkflowDefinionId;

	/** The JMX business object for ingest statistics */
	private IngestStatistics ingestStatistics = new IngestStatistics();

	/** The JMX bean object instance */
	private ObjectInstance registerMXBean;

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

	private Map<String, String> mergeWorkflowConfiguration(Map<String, String> properties, Long workflowId) {
		return properties;
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
