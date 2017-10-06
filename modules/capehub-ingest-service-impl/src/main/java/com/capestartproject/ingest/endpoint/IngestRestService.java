/**
 * 
 */
package com.capestartproject.ingest.endpoint;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.rest.AbstractJobProducerEndpoint;
import com.capestartproject.common.security.api.TrustedHttpClient;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.util.doc.rest.RestService;
import com.capestartproject.ingest.api.IngestService;
import com.google.common.collect.MapMaker;

/**
 * @author CS39
 *
 */
@Path("/")
@RestService(name = "ingestService", title = "Ingest Service", abstractText = "This service ingest capehub employee package", notes = {
		"All paths above are relative to the REST endpoint base (something like http://your.server/files)",
		"If the service is down or not working it will return a status 503, this means the the underlying service is "
				+ "not working and is either restarting or has failed",
		"A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
				+ "other words, there is a bug! You should file an error report with your server logs from the time when the "
				+ "error occurred" })
public class IngestRestService extends AbstractJobProducerEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(IngestRestService.class);

	/** Key for the default workflow definition in config.properties */
	protected static final String DEFAULT_WORKFLOW_DEFINITION = "com.capestartproject.workflow.default.definition";

	/** Key for the default maximum number of ingests in config.properties */
	protected static final String MAX_INGESTS_KEY = "com.capestartproject.ingest.max.concurrent";

	/** The http request parameter used to provide the workflow instance id */
	protected static final String WORKFLOW_INSTANCE_ID_PARAM = "workflowInstanceId";

	/** The http request parameter used to provide the workflow definition id */
	protected static final String WORKFLOW_DEFINITION_ID_PARAM = "workflowDefinitionId";

	/** The default workflow definition */
	private String defaultWorkflowDefinitionId = null;

	/** The http client */
	private TrustedHttpClient httpClient;

	// For the progress bar -1 bug workaround, keeping UploadJobs in memory
	// rather than saving them using JPA
	private HashMap<String, UploadJob> jobs;

	// The number of ingests this service can handle concurrently.
	private int ingestLimit = -1;
	/*
	 * Stores a map workflow ID and date to update the ingest start times
	 * post-hoc
	 */
	private ConcurrentMap<String, Date> startCache = null;


	private IngestService ingestService = null;
	private ServiceRegistry serviceRegistry = null;

	public IngestRestService() {
		jobs = new HashMap<String, UploadJob>();
		startCache = new MapMaker().expireAfterAccess(1, TimeUnit.DAYS).makeMap();
	}

	/**
	 * Returns the maximum number of concurrent ingest operations or
	 * <code>-1</code> if no limit is enforced.
	 *
	 * @return the maximum number of concurrent ingest operations
	 * @see #isIngestLimitEnabled()
	 */
	protected synchronized int getIngestLimit() {
		return ingestLimit;
	}

	/**
	 * Sets the maximum number of concurrent ingest operations. Use
	 * <code>-1</code> to indicate no limit.
	 *
	 * @param ingestLimit
	 *            the limit
	 */
	private synchronized void setIngestLimit(int ingestLimit) {
		this.ingestLimit = ingestLimit;
	}

	/**
	 * Returns <code>true</code> if a maximum number of concurrent ingest
	 * operations has been defined.
	 *
	 * @return <code>true</code> if there is a maximum number of concurrent
	 *         ingests
	 */
	protected synchronized boolean isIngestLimitEnabled() {
		return ingestLimit >= 0;
	}

	/**
	 * Sets the trusted http client
	 *
	 * @param httpClient
	 *          the http client
	 */
	public void setHttpClient(TrustedHttpClient httpClient) {
	   this.httpClient = httpClient;
	}

	@Override
	public JobProducer getService() {
		if (ingestService instanceof JobProducer)
			return (JobProducer) ingestService;
		else
			return null;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/**
	 * OSGi Declarative Services callback to set the reference to the ingest
	 * service.
	 *
	 * @param ingestService
	 *            the ingest service
	 */
	void setIngestService(IngestService ingestService) {
		this.ingestService = ingestService;
	}

	/**
	 * OSGi Declarative Services callback to set the reference to the service
	 * registry.
	 *
	 * @param serviceRegistry
	 *            the service registry
	 */
	void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected UploadJob createUploadJob() {
		UploadJob job = new UploadJob();
		jobs.put(job.getId(), job);
		return job;
	}
}
