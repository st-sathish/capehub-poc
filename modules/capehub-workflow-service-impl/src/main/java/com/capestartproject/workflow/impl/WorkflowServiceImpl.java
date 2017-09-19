package com.capestartproject.workflow.impl;

import static com.capestartproject.common.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static com.capestartproject.common.util.data.Collections.mkString;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.FAILED;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.FAILING;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.INSTANTIATED;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.PAUSED;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.RUNNING;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.STOPPED;
import static com.capestartproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.ObjectInstance;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.emppackage.EmployeePackage;
import com.capestartproject.common.emppackage.EmployeePackageElement;
import com.capestartproject.common.emppackage.EmployeePackageParser;
import com.capestartproject.common.emppackage.EmployeePackageSupport;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.UnauthorizedException;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.UndispatchableJobException;
import com.capestartproject.common.util.Log;
import com.capestartproject.common.util.MultiResourceLock;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.workflow.api.ResumableWorkflowOperationHandler;
import com.capestartproject.workflow.api.RetryStrategy;
import com.capestartproject.workflow.api.WorkflowDatabaseException;
import com.capestartproject.workflow.api.WorkflowDefinition;
import com.capestartproject.workflow.api.WorkflowException;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowInstance.WorkflowState;
import com.capestartproject.workflow.api.WorkflowInstanceImpl;
import com.capestartproject.workflow.api.WorkflowListener;
import com.capestartproject.workflow.api.WorkflowOperationDefinition;
import com.capestartproject.workflow.api.WorkflowOperationDefinitionImpl;
import com.capestartproject.workflow.api.WorkflowOperationException;
import com.capestartproject.workflow.api.WorkflowOperationHandler;
import com.capestartproject.workflow.api.WorkflowOperationInstance;
import com.capestartproject.workflow.api.WorkflowOperationInstance.OperationState;
import com.capestartproject.workflow.api.WorkflowOperationInstanceImpl;
import com.capestartproject.workflow.api.WorkflowOperationResult;
import com.capestartproject.workflow.api.WorkflowOperationResult.Action;
import com.capestartproject.workflow.api.WorkflowOperationResultImpl;
import com.capestartproject.workflow.api.WorkflowParser;
import com.capestartproject.workflow.api.WorkflowParsingException;
import com.capestartproject.workflow.api.WorkflowQuery;
import com.capestartproject.workflow.api.WorkflowService;
import com.capestartproject.workflow.api.WorkflowSet;
import com.capestartproject.workflow.api.WorkflowStateException;
import com.capestartproject.workflow.api.WorkflowStatistics;
import com.capestartproject.workflow.impl.jmx.WorkflowsStatistics;
import com.capestartproject.workspace.api.Workspace;

/**
 * Implements WorkflowService with in-memory data structures to hold WorkflowOperations and WorkflowInstances.
 * WorkflowOperationHandlers are looked up in the OSGi service registry based on the "workflow.operation" property. If
 * the WorkflowOperationHandler's "workflow.operation" service registration property matches
 * WorkflowOperation.getName(), then the factory returns a WorkflowOperationRunner to handle that operation. This allows
 * for custom runners to be added or modified without affecting the workflow service itself.
 */
public class WorkflowServiceImpl implements WorkflowService, JobProducer, ManagedService {

  /** Retry strategy property name */
  private static final String RETRY_STRATEGY = "retryStrategy";

  /** Logging facility */
  // private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);
  private static final Log logger = new Log(LoggerFactory.getLogger(WorkflowServiceImpl.class));

  /** List of available operations on jobs */
  enum Operation {
    START_WORKFLOW, RESUME, START_OPERATION
  }

  ;

  /** The pattern used by workfow operation configuration keys * */
  public static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{.+?\\}");

  /** The set of yes values */
  public static final Set<String> YES;

  /** The set of 'no' values */
  public static final Set<String> NO;

  /** The configuration key for setting {@link #maxConcurrentWorkflows} */
  public static final String MAX_CONCURRENT_CONFIG_KEY = "max.concurrent";

  /** The configuration key for setting {@link #workflowStatsCollect} */
  public static final String STATS_COLLECT_CONFIG_KEY = "workflowstats.collect";

  /** The default value for {@link #workflowStatsCollect} */
  public static final Boolean DEFAULT_STATS_COLLECT_CONFIG = false;

  /** Configuration value for the maximum number of parallel workflows based on the number of cores in the cluster */
  public static final String OPT_NUM_CORES = "cores";

  /** Constant value indicating a <code>null</code> parent id */
  private static final String NULL_PARENT_ID = "-";

  /** Workflow statistics JMX type */
  private static final String JMX_WORKFLOWS_STATISTICS_TYPE = "WorkflowsStatistics";

  /** The list of registered JMX beans */
  private final List<ObjectInstance> jmxBeans = new ArrayList<ObjectInstance>();

  /** The JMX business object for workflows statistics */
  private WorkflowsStatistics workflowsStatistics;
  /** Error resolution handler id constant */
  public static final String ERROR_RESOLUTION_HANDLER_ID = "error-resolution";

  /** Remove references to the component context once felix scr 1.2 becomes available */
  protected ComponentContext componentContext = null;

  /** The maximum number of cluster-wide workflows that will cause this service to stop accepting new jobs */
  protected int maxConcurrentWorkflows = -1;

  /** Flag whether to collect JMX statistics */
  protected boolean workflowStatsCollect = DEFAULT_STATS_COLLECT_CONFIG;

	/** The metadata services */
	// private SortedSet<MediaPackageMetadataService> metadataServices;

	/**
	 * The data access object responsible for storing and retrieving workflow
	 * instances
	 */
	protected WorkflowServiceIndex index;

  /** The list of workflow listeners */
  private final List<WorkflowListener> listeners = new CopyOnWriteArrayList<WorkflowListener>();

  /** The thread pool to use for firing listeners and handling dispatched jobs */
  protected ThreadPoolExecutor executorService;

  /** The workspace */
	protected Workspace workspace = null;

  /** The service registry */
	protected ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The workflow definition scanner */
  private WorkflowDefinitionScanner workflowDefinitionScanner;

  /** List of initially delayed workflows */
  private List<Long> delayedWorkflows = new ArrayList<Long>();

  /** Concurrent maps for lock objects */
  private final MultiResourceLock lock = new MultiResourceLock();
  private final MultiResourceLock updateLock = new MultiResourceLock();

  static {
    YES = new HashSet<String>(Arrays.asList(new String[] { "yes", "true", "on" }));
    NO = new HashSet<String>(Arrays.asList(new String[] { "no", "false", "off" }));
  }

	/**
	 * Constructs a new workflow service impl, with a priority-sorted map of
	 * metadata services
	 */
	public WorkflowServiceImpl() {
		/*
		 * metadataServices = new TreeSet<MediaPackageMetadataService>(new
		 * Comparator<MediaPackageMetadataService>() {
		 * 
		 * @Override public int compare(MediaPackageMetadataService o1,
		 * MediaPackageMetadataService o2) { return o1.getPriority() -
		 * o2.getPriority(); } });
		 */
	}

	/**
	 * Activate this service implementation via the OSGI service component
	 * runtime.
	 *
	 * @param componentContext
	 *            the component context
	 */
	public void activate(ComponentContext componentContext) {
		this.componentContext = componentContext;
		executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		try {
			logger.info("Generating JMX workflow statistics");
			workflowsStatistics = new WorkflowsStatistics(getBeanStatistics(), getHoldWorkflows());
			jmxBeans.add(JmxUtil.registerMXBean(workflowsStatistics, JMX_WORKFLOWS_STATISTICS_TYPE));
		} catch (WorkflowDatabaseException e) {
			logger.error("Error registarting JMX statistic beans {}", e);
		}
		logger.info("Activate Workflow service");
	}
	/**
	 * Converts a Map<String, String> to s key=value\n string, suitable for the
	 * properties form parameter expected by the workflow rest endpoint.
	 *
	 * @param props
	 *            The map of strings
	 * @return the string representation
	 */
	private String mapToString(Map<String, String> props) {
		if (props == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : props.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * Callback for the OSGi environment to register with the
	 * <code>Workspace</code>.
	 *
	 * @param workspace
	 *            the workspace
	 */
	protected void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

  	/**
	 * Callback for the OSGi environment to register with the
	 * <code>ServiceRegistry</code>.
	 *
	 * @param registry
	 *            the service registry
	 */

	protected void setServiceRegistry(ServiceRegistry registry) {
		this.serviceRegistry = registry;
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	/**
	 * Callback for setting the security service.
	 *
	 * @param securityService
	 *            the securityService to set
	 */
	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	/**
	 * Callback for setting the user directory service
	 *
	 * @param userDirectoryService
	 *            the userDirectoryService to set
	 */
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	/**
	 * Sets a reference to the organization directory service.
	 *
	 * @param organizationDirectory
	 *            the organization directory
	 */
	public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
		this.organizationDirectoryService = organizationDirectory;
	}

	/**
	 * Sets the search indexer to use in this service.
	 *
	 * @param index
	 *            The search index
	 */
	protected void setDao(WorkflowServiceIndex index) {
		this.index = index;
	}

	/**
	 * Callback to set the workflow definition scanner
	 *
	 * @param scanner
	 *            the workflow definition scanner
	 */
	protected void addWorkflowDefinitionScanner(WorkflowDefinitionScanner scanner) {
		workflowDefinitionScanner = scanner;
	}

	public void deactivate() {
		for (ObjectInstance mxbean : jmxBeans) {
			JmxUtil.unregisterMXBean(mxbean);
		}
	}

	private WorkflowStatistics getBeanStatistics() throws WorkflowDatabaseException {
		WorkflowStatistics stats = new WorkflowStatistics();
		long total = 0L;
		long failed = 0L;
		long failing = 0L;
		long instantiated = 0L;
		long paused = 0L;
		long running = 0L;
		long stopped = 0L;
		long finished = 0L;

		Organization organization = securityService.getOrganization();
		try {
			for (Organization org : organizationDirectoryService.getOrganizations()) {
				securityService.setOrganization(org);
				WorkflowStatistics statistics = getStatistics();
				total += statistics.getTotal();
				failed += statistics.getFailed();
				failing += statistics.getFailing();
				instantiated += statistics.getInstantiated();
				paused += statistics.getPaused();
				running += statistics.getRunning();
				stopped += statistics.getStopped();
				finished += statistics.getFinished();
			}
		} finally {
			securityService.setOrganization(organization);
		}

		stats.setTotal(total);
		stats.setFailed(failed);
		stats.setFailing(failing);
		stats.setInstantiated(instantiated);
		stats.setPaused(paused);
		stats.setRunning(running);
		stats.setStopped(stopped);
		stats.setFinished(finished);
		return stats;
	}

	private List<WorkflowInstance> getHoldWorkflows() throws WorkflowDatabaseException {
		List<WorkflowInstance> workflows = new ArrayList<WorkflowInstance>();
		Organization organization = securityService.getOrganization();
		try {
			for (Organization org : organizationDirectoryService.getOrganizations()) {
				securityService.setOrganization(org);
				WorkflowQuery workflowQuery = new WorkflowQuery().withState(WorkflowInstance.WorkflowState.PAUSED)
						.withCount(Integer.MAX_VALUE);
				WorkflowSet workflowSet = getWorkflowInstances(workflowQuery);
				workflows.addAll(Arrays.asList(workflowSet.getItems()));
			}
		} finally {
			securityService.setOrganization(organization);
		}
		return workflows;
	}

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowService#addWorkflowListener(com.capestartproject.workflow.api.WorkflowListener)
	 */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    listeners.add(listener);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowService#removeWorkflowListener(com.capestartproject.workflow.api.WorkflowListener)
	 */
  @Override
  public void removeWorkflowListener(WorkflowListener listener) {
    listeners.remove(listener);
  }

  /**
   * Fires the workflow listeners on workflow updates.
   */
  protected void fireListeners(final WorkflowInstance oldWorkflowInstance, final WorkflowInstance newWorkflowInstance) {
    final User currentUser = securityService.getUser();
    final Organization currentOrganization = securityService.getOrganization();
    for (final WorkflowListener listener : listeners) {
      if (oldWorkflowInstance == null || !oldWorkflowInstance.getState().equals(newWorkflowInstance.getState())) {
        Runnable runnable = new Runnable() {
          @Override
          public void run() {
            try {
              securityService.setUser(currentUser);
              securityService.setOrganization(currentOrganization);
              listener.stateChanged(newWorkflowInstance);
            } finally {
              securityService.setUser(null);
              securityService.setOrganization(null);
            }
          }
        };
        executorService.execute(runnable);
      } else {
        logger.debug("Not notifying %s because the workflow state has not changed", listener);
      }

      if (newWorkflowInstance.getCurrentOperation() != null) {
        if (oldWorkflowInstance == null || oldWorkflowInstance.getCurrentOperation() == null
                || !oldWorkflowInstance.getCurrentOperation().equals(newWorkflowInstance.getCurrentOperation())) {
          Runnable runnable = new Runnable() {
            @Override
            public void run() {
              try {
                securityService.setUser(currentUser);
                securityService.setOrganization(currentOrganization);
                listener.operationChanged(newWorkflowInstance);
              } finally {
                securityService.setUser(null);
                securityService.setOrganization(null);
              }
            }
          };
          executorService.execute(runnable);
        }
      } else {
        logger.debug("Not notifying %s because the workflow operation has not changed", listener);
      }
    }
  }

  	/**
	 * Handles the workflow for a failing operation.
	 *
	 * @param workflow
	 *            the workflow
	 * @param currentOperation
	 *            the failing workflow operation instance
	 * @throws WorkflowDatabaseException
	 *             If the exception handler workflow is not found
	 */
	private void handleFailedOperation(WorkflowInstance workflow, WorkflowOperationInstance currentOperation)
			throws WorkflowDatabaseException {
		String errorDefId = currentOperation.getExceptionHandlingWorkflow();

		// Adjust the workflow state according to the setting on the operation
		if (currentOperation.isFailWorkflowOnException()) {
			if (StringUtils.isBlank(errorDefId)) {
				workflow.setState(FAILED);
			} else {
				workflow.setState(FAILING);

				// Remove the rest of the original workflow
				int currentOperationPosition = workflow.getOperations().indexOf(currentOperation);
				List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>();
				operations.addAll(workflow.getOperations().subList(0, currentOperationPosition + 1));
				workflow.setOperations(operations);

				// Determine the current workflow configuration
				Map<String, String> configuration = new HashMap<String, String>();
				for (String configKey : workflow.getConfigurationKeys()) {
					configuration.put(configKey, workflow.getConfiguration(configKey));
				}

				// Append the operations
				WorkflowDefinition errorDef = null;
				try {
					errorDef = getWorkflowDefinitionById(errorDefId);
					workflow.extend(errorDef);
					workflow.setOperations(updateConfiguration(workflow, configuration).getOperations());
				} catch (NotFoundException notFoundException) {
					throw new IllegalStateException(
							"Unable to find the error workflow definition '" + errorDefId + "'");
				}
			}
		}

		// Fail the current operation
		currentOperation.setState(OperationState.FAILED);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isRunnable(WorkflowDefinition workflowDefinition) {
		List<String> availableOperations = listAvailableOperationNames();
		List<WorkflowDefinition> checkedWorkflows = new ArrayList<WorkflowDefinition>();
		boolean runnable = isRunnable(workflowDefinition, availableOperations, checkedWorkflows);
		int wfCount = checkedWorkflows.size() - 1;
		if (runnable)
			logger.info("Workflow %s, containing %d derived workflows, is runnable", workflowDefinition, wfCount);
		else
			logger.warn("Workflow %s, containing %d derived workflows, is not runnable", workflowDefinition, wfCount);
		return runnable;
	}

	/**
	 * Tests the workflow definition for its runnability. This method is a
	 * helper for {@link #isRunnable(WorkflowDefinition)} that is suited for
	 * recursive calling.
	 *
	 * @param workflowDefinition
	 *            the definition to test
	 * @param availableOperations
	 *            list of currently available operation handlers
	 * @param checkedWorkflows
	 *            list of checked workflows, used to avoid circular checking
	 * @return <code>true</code> if all bits and pieces used for executing
	 *         <code>workflowDefinition</code> are in place
	 */
  private boolean isRunnable(WorkflowDefinition workflowDefinition, List<String> availableOperations,
          List<WorkflowDefinition> checkedWorkflows) {
    if (checkedWorkflows.contains(workflowDefinition))
      return true;

    // Test availability of operation handler and catch workflows
    for (WorkflowOperationDefinition op : workflowDefinition.getOperations()) {
      if (!availableOperations.contains(op.getId())) {
        logger.info("%s is not runnable due to missing operation %s", workflowDefinition, op);
        return false;
      }
      String catchWorkflow = op.getExceptionHandlingWorkflow();
      if (catchWorkflow != null) {
        WorkflowDefinition catchWorkflowDefinition;
        try {
          catchWorkflowDefinition = getWorkflowDefinitionById(catchWorkflow);
        } catch (NotFoundException e) {
          logger.info("%s is not runnable due to missing catch workflow %s on operation %s", workflowDefinition,
                  catchWorkflow, op);
          return false;
        } catch (WorkflowDatabaseException e) {
          logger.info("%s is not runnable because we can not load the catch workflow %s on operation %s",
                  workflowDefinition, catchWorkflow, op);
          return false;
        }
        if (!isRunnable(catchWorkflowDefinition, availableOperations, checkedWorkflows))
          return false;
      }
    }

    // Add the workflow to the list of checked workflows
    if (!checkedWorkflows.contains(workflowDefinition))
      checkedWorkflows.add(workflowDefinition);
    return true;
  }

	/**
	 * Callback for workflow operation handlers that executed and finished
	 * without exception. This implementation assumes that the operation worker
	 * has already adjusted the current operation's state appropriately.
	 *
	 * @param workflow
	 *            the workflow instance
	 * @param result
	 *            the workflow operation result
	 * @return the workflow instance
	 * @throws WorkflowDatabaseException
	 *             if updating the workflow fails
	 */
	protected WorkflowInstance handleOperationResult(WorkflowInstance workflow, WorkflowOperationResult result)
			throws WorkflowDatabaseException {

		// Get the operation and its handler
		WorkflowOperationInstanceImpl currentOperation = (WorkflowOperationInstanceImpl) workflow.getCurrentOperation();
		WorkflowOperationHandler handler = getWorkflowOperationHandler(currentOperation.getTemplate());

		// Create an operation result for the lazy or else update the workflow's
		// employee package
		if (result == null) {
			logger.warn("Handling a null operation result for workflow %s in operation %s", workflow.getId(),
					currentOperation.getTemplate());
			result = new WorkflowOperationResultImpl(workflow.getEmployeePackage(), null, Action.CONTINUE, 0);
		} else {
			EmployeePackage mp = result.getEmployeePackage();
			if (mp != null) {
				workflow.setEmployeePackage(mp);
			}
		}

		// The action to take
		Action action = result.getAction();

		// Update the workflow configuration. Update the reference to the
		// current operation as well, since the workflow has
		// been serialized and deserialized in the meantime.
		int currentOperationPosition = currentOperation.getPosition();
		workflow = updateConfiguration(workflow, result.getProperties());
		currentOperation = (WorkflowOperationInstanceImpl) workflow.getOperations().get(currentOperationPosition);

		// Adjust workflow statistics
		currentOperation.setTimeInQueue(result.getTimeInQueue());

		// Adjust the operation state
		switch (action) {
		case CONTINUE:
			currentOperation.setState(OperationState.SUCCEEDED);
			break;
		case PAUSE:
			if (!(handler instanceof ResumableWorkflowOperationHandler)) {
				throw new IllegalStateException("Operation " + currentOperation.getTemplate() + " is not resumable");
			}

			// Set abortable and continuable to default values
			currentOperation.setContinuable(result.allowsContinue());
			currentOperation.setAbortable(result.allowsAbort());

			ResumableWorkflowOperationHandler resumableHandler = (ResumableWorkflowOperationHandler) handler;
			try {
				String url = resumableHandler.getHoldStateUserInterfaceURL(workflow);
				if (url != null) {
					String holdActionTitle = resumableHandler.getHoldActionTitle();
					currentOperation.setHoldActionTitle(holdActionTitle);
					currentOperation.setHoldStateUserInterfaceUrl(url);
				}
			} catch (WorkflowOperationException e) {
				logger.warn(e, "unable to replace workflow ID in the hold state URL");
			}

			workflow.setState(PAUSED);
			currentOperation.setState(OperationState.PAUSED);
			break;
		case SKIP:
			currentOperation.setState(OperationState.SKIPPED);
			break;
		default:
			throw new IllegalStateException("Unknown action '" + action + "' returned");
		}

		if (ERROR_RESOLUTION_HANDLER_ID.equals(currentOperation.getTemplate())
				&& result.getAction() == Action.CONTINUE) {

			Map<String, String> resultProperties = result.getProperties();
			if (resultProperties == null || StringUtils.isBlank(resultProperties.get(RETRY_STRATEGY)))
				throw new WorkflowDatabaseException("Retry strategy not present in properties!");

			RetryStrategy retryStrategy = RetryStrategy.valueOf(resultProperties.get(RETRY_STRATEGY));
			switch (retryStrategy) {
			case NONE:
				handleFailedOperation(workflow, workflow.getCurrentOperation());
				break;
			case RETRY:
				currentOperation = (WorkflowOperationInstanceImpl) workflow.getCurrentOperation();
				currentOperation.setRetryStrategy(RetryStrategy.NONE);
				break;
			default:
				throw new WorkflowDatabaseException("Retry strategy not implemented yet!");
			}
		}

		return workflow;
	}

	/**
	 * Reads the available metadata from the dublin core catalog (if there is
	 * one) and updates the employeepackage.
	 *
	 * @param mp
	 *            the media package
	 */
	/*
	 * protected void populateEmployeePackageMetadata(EmployeePackage ep) { if
	 * (metadataServices.size() == 0) { logger.warn(
	 * "No metadata services are registered, so no media package metadata can be extracted from catalogs"
	 * ); return; } for (EmployeePackageMetadataService metadataService :
	 * metadataServices) { EmployeePackageMetadata metadata =
	 * metadataService.getMetadata(ep); if (metadata != null) {
	 * 
	 * } } }
	 */

	/**
	 * {@inheritDoc}
	 *
	 * If we are already running the maximum number of workflows, don't accept
	 * another START_WORKFLOW job.
	 *
	 * @see com.capestartproject.job.api.JobProducer#isReadyToAcceptJobs(String)
	 */
	@Override
	public boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException {
		if (!Operation.START_WORKFLOW.toString().equals(operation))
			return true;

		long runningWorkflows;
		try {
			runningWorkflows = serviceRegistry.countByOperation(JOB_TYPE, Operation.START_WORKFLOW.toString(),
					Job.Status.RUNNING);
		} catch (ServiceRegistryException e) {
			logger.warn(e, "Unable to determine the number of running workflows");
			return false;
		}

		// If no hard maximum has been configured, ask the service registry for
		// the number of cores in the system
		int maxWorkflows = maxConcurrentWorkflows;
		if (maxWorkflows < 1) {
			maxWorkflows = serviceRegistry.getMaxConcurrentJobs();
		}

		// Reject if there's enough going on already.
		if (runningWorkflows >= maxWorkflows) {
			logger.debug("Refused to accept new workflow. This server is already running %s workflows.",
					runningWorkflows);
			return false;
		}

		return true;
	}

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
	 */
  @Override
	public List<WorkflowDefinition> listAvailableWorkflowDefinitions() {
		List<WorkflowDefinition> list = new ArrayList<WorkflowDefinition>();
		for (Entry<String, WorkflowDefinition> entry : workflowDefinitionScanner.getWorkflowDefinitions().entrySet()) {
			list.add(entry.getValue());
    }
		Collections.sort(list, new Comparator<WorkflowDefinition>() {
      @Override
			public int compare(WorkflowDefinition o1, WorkflowDefinition o2) {
				return o1.getId().compareTo(o2.getId());
      }
    });
		return list;
  }

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.job.api.JobProducer#getJobType()
	 */
	@Override
	public String getJobType() {
		return JOB_TYPE;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public void updated(Dictionary properties) throws ConfigurationException {
		String maxConfiguration = StringUtils.trimToNull((String) properties.get(MAX_CONCURRENT_CONFIG_KEY));
		if (StringUtils.isNotEmpty(maxConfiguration)) {
			try {
				maxConcurrentWorkflows = Integer.parseInt(maxConfiguration);
				logger.info("Set maximum concurrent workflows to %d", maxConcurrentWorkflows);
			} catch (NumberFormatException e) {
				logger.warn("Can not set max concurrent workflows to %s. %s must be an integer", maxConfiguration,
						MAX_CONCURRENT_CONFIG_KEY);
			}
		}
		String workflowStatsConfiguration = StringUtils.trimToNull((String) properties.get(STATS_COLLECT_CONFIG_KEY));
		if (StringUtils.isNotEmpty(workflowStatsConfiguration)) {
			try {
				workflowStatsCollect = Boolean.parseBoolean(workflowStatsConfiguration);
				logger.info("Workflow statistics collection is set to %s", workflowStatsConfiguration);
			} catch (Exception e) {
				logger.warn("Workflow statistics collection flag '%s' is malformed, setting to %s",
						workflowStatsConfiguration, DEFAULT_STATS_COLLECT_CONFIG.toString());
				workflowStatsCollect = DEFAULT_STATS_COLLECT_CONFIG;
			}
		}
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
		return serviceRegistry.count(JOB_TYPE, status);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.opencastproject.job.api.AbstractJobProducer#acceptJob(org.opencastproject.job.api.Job)
	 */
	@Override
	public synchronized void acceptJob(Job job) throws ServiceRegistryException {
		User originalUser = securityService.getUser();
		Organization originalOrg = securityService.getOrganization();
		try {
			Organization organization = organizationDirectoryService.getOrganization(job.getOrganization());
			securityService.setOrganization(organization);
			User user = userDirectoryService.loadUser(job.getCreator());
			securityService.setUser(user);
			job.setStatus(Job.Status.RUNNING);
			serviceRegistry.updateJob(job);

			// Check if this workflow was initially delayed
			if (delayedWorkflows.contains(job.getId())) {
				delayedWorkflows.remove(job.getId());
				logger.info("Starting initially delayed workflow %s, %d more waiting", job.getId(),
						delayedWorkflows.size());
			}

			executorService.submit(new JobRunner(job, serviceRegistry.getCurrentJob()));
		} catch (Exception e) {
			if (e instanceof ServiceRegistryException)
				throw (ServiceRegistryException) e;
			throw new ServiceRegistryException(e);
		} finally {
			securityService.setUser(originalUser);
			securityService.setOrganization(originalOrg);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * If we are already running the maximum number of workflows, don't accept
	 * another START_WORKFLOW job
	 *
	 * @see org.opencastproject.job.api.AbstractJobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
	 */
	@Override
	public boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException {
		String operation = job.getOperation();

		// Only restrict execution of new jobs
		if (!Operation.START_WORKFLOW.toString().equals(operation))
			return true;

		// If the first operation is guaranteed to pause, run the job.
		if (job.getArguments().size() > 1 && job.getArguments().get(0) != null) {
			try {
				WorkflowDefinition workflowDef = WorkflowParser.parseWorkflowDefinition(job.getArguments().get(0));
				if (workflowDef.getOperations().size() > 0) {
					String firstOperationId = workflowDef.getOperations().get(0).getId();
					WorkflowOperationHandler handler = getWorkflowOperationHandler(firstOperationId);
					if (handler instanceof ResumableWorkflowOperationHandler) {
						if (((ResumableWorkflowOperationHandler) handler).isAlwaysPause()) {
							return true;
						}
					}
				}
			} catch (WorkflowParsingException e) {
				throw new UndispatchableJobException(job + " is not a proper job to start a workflow", e);
			}
		}

		WorkflowInstance workflow = null;
		WorkflowSet workflowInstances = null;
		String mediaPackageId = null;

		// Fetch all workflows that are running with the current employeepackage
		try {
			workflow = getWorkflowById(job.getId());
			mediaPackageId = workflow.getEmployeePackage().getIdentifier().toString();
			workflowInstances = getWorkflowInstances(
					new WorkflowQuery().withMediaPackage(workflow.getEmployeePackage().getIdentifier().toString())
							.withState(RUNNING).withState(PAUSED).withState(FAILING));

		} catch (NotFoundException e) {
			logger.error(
					"Trying to start workflow with id %s but no corresponding instance is available from the workflow service",
					job.getId());
			throw new UndispatchableJobException(e);
		} catch (UnauthorizedException e) {
			logger.error("Authorization denied while requesting to loading workflow instance %s: %s", job.getId(),
					e.getMessage());
			throw new UndispatchableJobException(e);
		} catch (WorkflowDatabaseException e) {
			logger.error("Error loading workflow instance %s: %s", job.getId(), e.getMessage());
			return false;
		}

		// If more than one workflow is running working on this employeepackage,
		// then we don't start this one
		boolean toomany = workflowInstances.size() > 1;

		// Make sure we are not excluding ourselves
		toomany |= workflowInstances.size() == 1 && workflow.getId() != workflowInstances.getItems()[0].getId();

		// Avoid running multiple workflows with same employee package id at the
		// same time
		if (toomany) {
			if (!delayedWorkflows.contains(workflow.getId())) {
				logger.info("Delaying start of workflow %s, another workflow on media package %s is still running",
						workflow.getId(), mediaPackageId);
				delayedWorkflows.add(workflow.getId());
			}
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * registerWorkflowDefinition(com.capestartproject.workflow.api.
	 * WorkflowDefinition)
	 */
	@Override
	public void registerWorkflowDefinition(WorkflowDefinition workflow) throws WorkflowDatabaseException {
		if (workflow == null || workflow.getId() == null) {
			throw new IllegalArgumentException("Workflow must not be null, and must contain an ID");
		}
		String id = workflow.getId();
		if (workflowDefinitionScanner.getWorkflowDefinitions().containsKey(id)) {
			throw new IllegalStateException("A workflow definition with ID '" + id + "' is already registered.");
		}
		workflowDefinitionScanner.putWorkflowDefinition(id, workflow);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * unregisterWorkflowDefinition(java.lang.String)
	 */
	@Override
	public void unregisterWorkflowDefinition(String workflowDefinitionId)
			throws NotFoundException, WorkflowDatabaseException {
		if (workflowDefinitionScanner.removeWorkflowDefinition(workflowDefinitionId) == null) {
			throw new NotFoundException("Workflow definition not found");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * getWorkflowDefinitionById(java.lang.String)
	 */
	@Override
	public WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException {
		WorkflowDefinition def = workflowDefinitionScanner.getWorkflowDefinition(id);
		if (def == null)
			throw new NotFoundException("Workflow definition '" + id + "' not found");
		return def;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowService#getWorkflowById(long)
	 */
	@Override
	public WorkflowInstanceImpl getWorkflowById(long id)
			throws WorkflowDatabaseException, NotFoundException, UnauthorizedException {
		try {
			Job job = serviceRegistry.getJob(id);
			if (Status.DELETED.equals(job.getStatus())) {
				throw new NotFoundException("Workflow '" + id + "' has been deleted");
			}
			if (JOB_TYPE.equals(job.getJobType()) && Operation.START_WORKFLOW.toString().equals(job.getOperation())) {
				WorkflowInstanceImpl workflow = WorkflowParser.parseWorkflowInstance(job.getPayload());
				// assertPermission(workflow, READ_PERMISSION);
				return workflow;
			} else {
				throw new NotFoundException("'" + id + "' is a job identifier, but it is not a workflow identifier");
			}
		} catch (WorkflowParsingException e) {
			throw new IllegalStateException("The workflow job payload is malformed");
		} catch (ServiceRegistryException e) {
			throw new IllegalStateException("Error loading workflow job from the service registry");
		} /*
			 * catch (EmployeePackageException e) { throw new
			 * IllegalStateException("Unable to read employeepackage from workflow "
			 * + id, e); }
			 */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowService#getWorkflowInstances(
	 * com.capestartproject.workflow.api.WorkflowQuery)
	 */
	@Override
	public WorkflowSet getWorkflowInstances(WorkflowQuery query) throws WorkflowDatabaseException {
		return index.getWorkflowInstances(query, WRITE_PERMISSION, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * getWorkflowInstancesForAdministrativeRead(com.capestartproject.workflow.
	 * api.WorkflowQuery)
	 */
	@Override
	public WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery query)
			throws WorkflowDatabaseException, UnauthorizedException {
		User user = securityService.getUser();
		if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole()))
			throw new UnauthorizedException(user, getClass().getName() + ".getForAdministrativeRead");

		return index.getWorkflowInstances(query, WRITE_PERMISSION, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowService#countWorkflowInstances(
	 * )
	 */
	@Override
	public long countWorkflowInstances() throws WorkflowDatabaseException {
		return index.countWorkflowInstances(null, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowService#countWorkflowInstances(
	 * com.capestartproject.workflow.api.WorkflowInstance.WorkflowState,
	 * java.lang.String)
	 */
	@Override
	public long countWorkflowInstances(WorkflowState state, String operation) throws WorkflowDatabaseException {
		return index.countWorkflowInstances(state, operation);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#getStatistics()
	 */
	@Override
	public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
		return index.getStatistics();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#stop(long)
	 */
	@Override
	public WorkflowInstance stop(long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		return lock.synchronize(workflowInstanceId, new Function.X<Long, WorkflowInstance>() {
			@Override
			public WorkflowInstance xapply(Long workflowInstanceId) throws Exception {
				WorkflowInstanceImpl instance = getWorkflowById(workflowInstanceId);
				instance.setState(STOPPED);

				// Update the workflow instance
				update(instance);

				removeTempFiles(instance);

				return instance;
			}
		});
	}

	private void removeTempFiles(WorkflowInstance workflowInstance)
			throws WorkflowDatabaseException, UnauthorizedException, NotFoundException {
		logger.info("Removing temporary files for workflow {}", workflowInstance);
		for (EmployeePackageElement elem : workflowInstance.getEmployeePackage().getElements()) {
			try {
				logger.debug("Removing temporary file {} for workflow {}", elem.getURI(), workflowInstance);
				workspace.delete(elem.getURI());
			} catch (IOException e) {
				logger.warn("Unable to delete mediapackage element {}", e.getMessage());
			} catch (NotFoundException e) {
				// File was probably already deleted before...
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#remove(long)
	 */
	@Override
	public void remove(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
			NotFoundException, UnauthorizedException, WorkflowStateException {
		lock.synchronize(workflowInstanceId, new Function.X<Long, Void>() {
			@Override
			protected Void xapply(Long workflowInstanceId) throws Exception {
				WorkflowQuery query = new WorkflowQuery();
				query.withId(Long.toString(workflowInstanceId));
				WorkflowSet workflows = index.getWorkflowInstances(query, READ_PERMISSION, false);
				if (workflows.size() == 1) {
					WorkflowInstance instance = workflows.getItems()[0];

					WorkflowInstance.WorkflowState state = instance.getState();
					if (state != WorkflowState.SUCCEEDED && state != WorkflowState.FAILED
							&& state != WorkflowState.STOPPED)
						throw new WorkflowStateException("Workflow instance with state '" + state
								+ "' cannot be removed. Only states SUCCEEDED, FAILED & STOPPED are allowed");

					// try {
					// assertPermission(instance, WRITE_PERMISSION);
					// } catch (MediaPackageException e) {
					// throw new WorkflowParsingException(e);
					// }

					// First, remove temporary files DO THIS BEFORE REMOVING
					// FROM INDEX
					try {
						removeTempFiles(instance);
					} catch (NotFoundException e) {
						// If the files aren't their anymore, we don't have to
						// cleanup up them :-)
						logger.debug("Temporary files of workflow instance {} seem to be gone already...",
								workflowInstanceId);
					}

					// Second, remove jobs related to a operation which belongs
					// to the workflow instance
					List<WorkflowOperationInstance> operations = instance.getOperations();
					for (WorkflowOperationInstance op : operations) {
						if (op.getId() != null) {
							long workflowOpId = op.getId();
							if (workflowOpId != workflowInstanceId) {
								try {
									serviceRegistry.removeJob(workflowOpId);
								} catch (ServiceRegistryException e) {
									logger.warn("Problems while removing jobs related to workflow operation '{}': {}",
											workflowOpId, e.getMessage());
								} catch (NotFoundException e) {
									logger.debug(
											"No jobs related to the workflow operation '{}' found in the service registry",
											workflowOpId);
								}
							}
						}
					}

					// Third, remove workflow instance job itself
					try {
						serviceRegistry.removeJob(workflowInstanceId);
					} catch (ServiceRegistryException e) {
						logger.warn("Problems while removing workflow instance job '{}': {}", workflowInstanceId,
								e.getMessage());
					} catch (NotFoundException e) {
						logger.info("No workflow instance job '{}' found in the service registry", workflowInstanceId);
					}

					// At last, remove workflow instance from the index
					try {
						index.remove(workflowInstanceId);
					} catch (NotFoundException e) {
						// This should never happen, because we got workflow
						// instance by querying the index...
						logger.warn("Workflow instance could not be removed from index: {}", e);
					}
				} else if (workflows.size() == 0) {
					throw new NotFoundException(
							"Workflow instance with id '" + Long.toString(workflowInstanceId) + "' could not be found");
				} else {
					throw new WorkflowDatabaseException(
							"More than one workflow found with id: " + Long.toString(workflowInstanceId));
				}
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#suspend(long)
	 */
	@Override
	public WorkflowInstance suspend(long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		return lock.synchronize(workflowInstanceId, new Function.X<Long, WorkflowInstance>() {
			@Override
			protected WorkflowInstance xapply(Long workflowInstanceId) throws Exception {
				WorkflowInstanceImpl instance = getWorkflowById(workflowInstanceId);
				instance.setState(PAUSED);
				update(instance);
				return instance;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#resume(long)
	 */
	@Override
	public WorkflowInstance resume(long workflowInstanceId)
			throws NotFoundException, WorkflowException, IllegalStateException, UnauthorizedException {
		return resume(workflowInstanceId, null);
	}

	/*
	 * {@inheritDoc}
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#resume(long,
	 * java.util.Map)
	 */
	@Override
	public WorkflowInstance resume(long workflowInstanceId, Map<String, String> properties)
			throws NotFoundException, WorkflowException, IllegalStateException, UnauthorizedException {
		WorkflowInstance workflowInstance = getWorkflowById(workflowInstanceId);
		if (!WorkflowState.PAUSED.equals(workflowInstance.getState()))
			throw new IllegalStateException("Can not resume a workflow where the current state is not in paused");

		workflowInstance = updateConfiguration(workflowInstance, properties);
		update(workflowInstance);

		WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();

		// Is the workflow done?
		if (currentOperation == null) {
			// Let's make sure we didn't miss any failed operation, since the
			// workflow state could have been
			// switched to paused while processing the error handling workflow
			// extension
			workflowInstance.setState(SUCCEEDED);
			for (WorkflowOperationInstance op : workflowInstance.getOperations()) {
				if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
					if (op.isFailWorkflowOnException()) {
						workflowInstance.setState(FAILED);
						break;
					}
				}
			}

			// Save the resumed workflow to the database
			logger.debug("%s has %s", workflowInstance, workflowInstance.getState());
			update(workflowInstance);
			return workflowInstance;
		}

		// We can resume workflows when they are in either the paused state, or
		// they are being advanced manually passed
		// certain operations. In the latter case, there is no current paused
		// operation.
		if (OperationState.INSTANTIATED.equals(currentOperation.getState())) {
			try {
				// the operation has its own job. Update that too.
				Job operationJob = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
						Arrays.asList(Long.toString(workflowInstanceId)), null, false, null);

				// this method call is publicly visible, so it doesn't
				// necessarily go through the accept method. Set the
				// workflow state manually.
				workflowInstance.setState(RUNNING);
				currentOperation.setId(operationJob.getId());

				// update the workflow and its associated job
				update(workflowInstance);

				// Now set this job to be queued so it can be dispatched
				operationJob.setStatus(Status.QUEUED);
				operationJob.setDispatchable(true);
				serviceRegistry.updateJob(operationJob);

				return workflowInstance;
			} catch (ServiceRegistryException e) {
				throw new WorkflowDatabaseException(e);
			}
		}

		Long operationJobId = workflowInstance.getCurrentOperation().getId();
		if (operationJobId == null)
			throw new IllegalStateException(
					"Can not resume a workflow where the current operation has no associated id");

		// Set the current operation's job to queued, so it gets picked up again
		Job workflowJob;
		try {
			workflowJob = serviceRegistry.getJob(workflowInstanceId);
			workflowJob.setStatus(Status.RUNNING);
			workflowJob.setPayload(WorkflowParser.toXml(workflowInstance));
			serviceRegistry.updateJob(workflowJob);

			Job operationJob = serviceRegistry.getJob(operationJobId);
			operationJob.setStatus(Status.QUEUED);
			operationJob.setDispatchable(true);
			if (properties != null) {
				Properties props = new Properties();
				props.putAll(properties);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				props.store(out, null);
				List<String> newArguments = new ArrayList<String>(operationJob.getArguments());
				newArguments.add(new String(out.toByteArray(), "UTF-8"));
				operationJob.setArguments(newArguments);
			}
			serviceRegistry.updateJob(operationJob);
		} catch (ServiceRegistryException e) {
			throw new WorkflowDatabaseException(e);
		} catch (IOException e) {
			throw new WorkflowParsingException("Unable to parse workflow and/or workflow properties");
		}

		return workflowInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#update(com.
	 * capestartproject.workflow.api.WorkflowInstance)
	 */
	@Override
	public void update(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException {
		updateLock.synchronize(workflowInstance.getId(), new Function.X<Long, Void>() {
			@Override
			protected Void xapply(Long a) throws Exception {
				WorkflowInstance originalWorkflowInstance = null;
				try {
					originalWorkflowInstance = getWorkflowById(workflowInstance.getId());
				} catch (NotFoundException e) {
					// That's fine, it's a new workflow instance
				}

				// Before we persist this, extract the metadata
				final EmployeePackage updatedMediaPackage = workflowInstance.getEmployeePackage();
				// populateEmployeePackageMetadata(updatedMediaPackage);

				// Synchronize the job status with the workflow
				WorkflowState workflowState = workflowInstance.getState();
				String xml;
				try {
					xml = WorkflowParser.toXml(workflowInstance);
				} catch (Exception e) {
					// Can't happen, since we are converting from an in-memory
					// object
					throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
				}

				Job job = null;
				try {
					job = serviceRegistry.getJob(workflowInstance.getId());
					job.setPayload(xml);

					// Synchronize workflow and job state
					switch (workflowState) {
					case FAILED:
						job.setStatus(Status.FAILED);
						break;
					case FAILING:
						break;
					case INSTANTIATED:
						job.setDispatchable(true);
						job.setStatus(Status.QUEUED);
						break;
					case PAUSED:
						job.setStatus(Status.PAUSED);
						break;
					case RUNNING:
						job.setStatus(Status.RUNNING);
						break;
					case STOPPED:
						job.setStatus(Status.DELETED);
						break;
					case SUCCEEDED:
						job.setStatus(Status.FINISHED);
						break;
					default:
						throw new IllegalStateException("Found a workflow state that is not handled");
					}
				} catch (ServiceRegistryException e) {
					logger.error(e, "Unable to read workflow job %s from service registry", workflowInstance.getId());
					throw new WorkflowDatabaseException(e);
				} catch (NotFoundException e) {
					logger.error("Job for workflow %s not found in service registry", workflowInstance.getId());
					throw new WorkflowDatabaseException(e);
				}

				// Update both workflow and workflow job
				try {
					serviceRegistry.updateJob(job);
					index(workflowInstance);
				} catch (ServiceRegistryException e) {
					logger.error(
							"Update of workflow job %s in the service registry failed, service registry and workflow index may be out of sync",
							workflowInstance.getId());
					throw new WorkflowDatabaseException(e);
				} catch (NotFoundException e) {
					logger.error("Job for workflow %s not found in service registry", workflowInstance.getId());
					throw new WorkflowDatabaseException(e);
				} catch (Exception e) {
					logger.error(
							"Update of workflow job %s in the service registry failed, service registry and workflow index may be out of sync",
							job.getId());
					throw new WorkflowException(e);
				}

				if (workflowStatsCollect) {
					workflowsStatistics.updateWorkflow(getBeanStatistics(), getHoldWorkflows());
				}

				try {
					WorkflowInstance clone = WorkflowParser
							.parseWorkflowInstance(WorkflowParser.toXml(workflowInstance));
					fireListeners(originalWorkflowInstance, clone);
				} catch (Exception e) {
					// Can't happen, since we are converting from an in-memory
					// object
					throw new IllegalStateException("In-memory workflow instance could not be serialized", e);
				}
				return null;
			}
		});
	}

	/**
	 * Updates the search index entries for this workflow instance.
	 *
	 * @param workflowInstance
	 *            the workflow
	 * @throws WorkflowDatabaseException
	 *             if there is a problem storing the workflow instance
	 */
	protected void index(final WorkflowInstance workflowInstance) throws WorkflowDatabaseException {
		// Update the search index
		index.update(workflowInstance);
	}

	/**
	 * Gets the currently registered workflow operation handlers.
	 *
	 * @return All currently registered handlers
	 */
	public Set<HandlerRegistration> getRegisteredHandlers() {
		Set<HandlerRegistration> set = new HashSet<HandlerRegistration>();
		ServiceReference[] refs;
		try {
			refs = componentContext.getBundleContext().getServiceReferences(WorkflowOperationHandler.class.getName(),
					null);
		} catch (InvalidSyntaxException e) {
			throw new IllegalStateException(e);
		}
		if (refs != null) {
			for (ServiceReference ref : refs) {
				WorkflowOperationHandler handler = (WorkflowOperationHandler) componentContext.getBundleContext()
						.getService(ref);
				set.add(new HandlerRegistration((String) ref.getProperty(WORKFLOW_OPERATION_PROPERTY), handler));
			}
		} else {
			logger.warn("No registered workflow operation handlers found");
		}
		return set;
	}

	protected WorkflowOperationHandler getWorkflowOperationHandler(String operationId) {
		for (HandlerRegistration reg : getRegisteredHandlers()) {
			if (reg.operationName.equals(operationId))
				return reg.handler;
		}
		return null;
	}

	/**
	 * Callback for workflow operations that were throwing an exception. This
	 * implementation assumes that the operation worker has already adjusted the
	 * current operation's state appropriately.
	 *
	 * @param workflow
	 *            the workflow instance
	 * @param operation
	 *            the current workflow operation
	 * @return the workflow instance
	 * @throws WorkflowParsingException
	 */
	protected WorkflowInstance handleOperationException(WorkflowInstance workflow, WorkflowOperationInstance operation)
			throws WorkflowDatabaseException, WorkflowParsingException, UnauthorizedException {
		WorkflowOperationInstanceImpl currentOperation = (WorkflowOperationInstanceImpl) operation;
		int failedAttempt = currentOperation.getFailedAttempts() + 1;
		currentOperation.setFailedAttempts(failedAttempt);
		currentOperation.addToExecutionHistory(currentOperation.getId());

		if (currentOperation.getMaxAttempts() != -1 && failedAttempt == currentOperation.getMaxAttempts()) {
			handleFailedOperation(workflow, currentOperation);
		} else {
			switch (currentOperation.getRetryStrategy()) {
			case NONE:
				handleFailedOperation(workflow, currentOperation);
				break;
			case RETRY:
				currentOperation.setState(OperationState.RETRY);
				break;
			case HOLD:
				currentOperation.setState(OperationState.RETRY);
				List<WorkflowOperationInstance> operations = workflow.getOperations();
				WorkflowOperationDefinitionImpl errorResolutionDefinition = new WorkflowOperationDefinitionImpl(
						ERROR_RESOLUTION_HANDLER_ID, "Error Resolution Operation", "error", true);
				WorkflowOperationInstanceImpl errorResolutionInstance = new WorkflowOperationInstanceImpl(
						errorResolutionDefinition, currentOperation.getPosition());
				errorResolutionInstance.setExceptionHandlingWorkflow(currentOperation.getExceptionHandlingWorkflow());
				operations.add(currentOperation.getPosition(), errorResolutionInstance);
				workflow.setOperations(operations);
				break;
			default:
				break;
			}
		}
		return workflow;
	}

	/**
	 * Lists the names of each workflow operation. Operation names are availalbe
	 * for use if there is a registered {@link WorkflowOperationHandler} with an
	 * equal {@link WorkflowServiceImpl#WORKFLOW_OPERATION_PROPERTY} property.
	 *
	 * @return The {@link List} of available workflow operation names
	 */
	protected List<String> listAvailableOperationNames() {
		List<String> list = new ArrayList<String>();
		for (HandlerRegistration reg : getRegisteredHandlers()) {
			list.add(reg.operationName);
		}
		return list;
	}


	/**
	 * A tuple of a workflow operation handler and the name of the operation it
	 * handles
	 */
	public static class HandlerRegistration {

		protected WorkflowOperationHandler handler;
		protected String operationName;

		public HandlerRegistration(String operationName, WorkflowOperationHandler handler) {
			if (operationName == null)
				throw new IllegalArgumentException("Operation name cannot be null");
			if (handler == null)
				throw new IllegalArgumentException("Handler cannot be null");
			this.operationName = operationName;
			this.handler = handler;
		}

		public WorkflowOperationHandler getHandler() {
			return handler;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + handler.hashCode();
			result = prime * result + operationName.hashCode();
			return result;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			HandlerRegistration other = (HandlerRegistration) obj;
			if (!handler.equals(other.handler))
				return false;
			if (!operationName.equals(other.operationName))
				return false;
			return true;
		}
	}

	/**
	 * A utility class to run jobs
	 */
	class JobRunner implements Callable<Void> {

		/** The job */
		private Job job = null;

		/** The current job */
		private final Job currentJob;

		/**
		 * Constructs a new job runner
		 *
		 * @param job
		 *            the job to run
		 * @param currentJob
		 *            the current running job
		 */
		JobRunner(Job job, Job currentJob) {
			this.job = job;
			this.currentJob = currentJob;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() throws Exception {
			Organization jobOrganization = organizationDirectoryService.getOrganization(job.getOrganization());
			try {
				serviceRegistry.setCurrentJob(currentJob);
				securityService.setOrganization(jobOrganization);
				User jobUser = userDirectoryService.loadUser(job.getCreator());
				securityService.setUser(jobUser);
				process(job);
			} finally {
				serviceRegistry.setCurrentJob(null);
				securityService.setUser(null);
				securityService.setOrganization(null);
			}
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.emppackage.EmployeePackage, java.util.Map)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition, EmployeePackage employeePackage,
			Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException {
		try {
			return start(workflowDefinition, employeePackage, null, properties);
		} catch (NotFoundException e) {
			// should never happen
			throw new IllegalStateException(
					"a null workflow ID caused a NotFoundException.  This is a programming error.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.emppackage.EmployeePackage, java.lang.Long,
	 * java.util.Map)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition, EmployeePackage sourceEmployeePackage,
			Long parentWorkflowId, Map<String, String> properties)
			throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException {
		try {
			logger.startUnitOfWork();
			if (workflowDefinition == null)
				throw new IllegalArgumentException("workflow definition must not be null");
			if (sourceEmployeePackage == null)
				throw new IllegalArgumentException("employeepackage must not be null");
			for (List<String> errors : EmployeePackageSupport.sanityCheck(sourceEmployeePackage)) {
				throw new IllegalArgumentException(
						"Insane employee package cannot be processed: " + mkString(errors, "; "));
			}
			if (parentWorkflowId != null) {
				try {
					getWorkflowById(parentWorkflowId); // Let NotFoundException
														// bubble up
				} catch (UnauthorizedException e) {
					throw new IllegalArgumentException(
							"Parent workflow " + parentWorkflowId + " not visible to this user");
				}
			}

			// Get the current user
			User currentUser = securityService.getUser();
			if (currentUser == null)
				throw new SecurityException("Current user is unknown");

			// Get the current organization
			Organization organization = securityService.getOrganization();
			if (organization == null)
				throw new SecurityException("Current organization is unknown");

			WorkflowInstance workflowInstance = new WorkflowInstanceImpl(workflowDefinition, sourceEmployeePackage,
					parentWorkflowId, currentUser, organization, properties);
			workflowInstance = updateConfiguration(workflowInstance, properties);

			// Create and configure the workflow instance
			try {
				// Create a new job for this workflow instance
				String workflowDefinitionXml = WorkflowParser.toXml(workflowDefinition);
				String workflowInstanceXml = WorkflowParser.toXml(workflowInstance);
				String employeePackageXml = EmployeePackageParser.getAsXml(sourceEmployeePackage);

				List<String> arguments = new ArrayList<String>();
				arguments.add(workflowDefinitionXml);
				arguments.add(employeePackageXml);
				if (parentWorkflowId != null || properties != null) {
					String parentWorkflowIdString = (parentWorkflowId != null) ? parentWorkflowId.toString()
							: NULL_PARENT_ID;
					arguments.add(parentWorkflowIdString);
				}
				if (properties != null) {
					arguments.add(mapToString(properties));
				}

				Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_WORKFLOW.toString(), arguments,
						workflowInstanceXml, false, null);

				// Have the workflow take on the job's identity
				workflowInstance.setId(job.getId());

				// Add the workflow to the search index and have the job
				// enqueued for dispatch.
				// Update also sets ACL and employeepackage metadata
				update(workflowInstance);

				return workflowInstance;
			} catch (Throwable t) {
				try {
					workflowInstance.setState(FAILED);
					update(workflowInstance);
				} catch (Exception failureToFail) {
					logger.warn(failureToFail, "Unable to update workflow to failed state");
				}
				throw new WorkflowDatabaseException(t);
			}
		} finally {
			logger.endUnitOfWork();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.emppackage.EmployeePackage)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition, EmployeePackage employeePackage)
			throws WorkflowDatabaseException, WorkflowParsingException {
		return start(workflowDefinition, employeePackage, new HashMap<String, String>());
	}

	protected WorkflowInstance updateConfiguration(WorkflowInstance instance, Map<String, String> properties) {
		try {
			if (properties != null) {
				for (Entry<String, String> entry : properties.entrySet()) {
					instance.setConfiguration(entry.getKey(), entry.getValue());
				}
			}
			String xml = replaceVariables(WorkflowParser.toXml(instance), properties);
			WorkflowInstanceImpl workflow = WorkflowParser.parseWorkflowInstance(xml);
			return workflow;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to replace workflow instance variables", e);
		}
	}

	/**
	 * Replaces all occurrences of <code>${.*+}</code> with the property in the
	 * provided map, or if not available in the map, from the bundle context
	 * properties, if available.
	 *
	 * @param source
	 *            The source string
	 * @param properties
	 *            The map of properties to replace
	 * @return The resulting string
	 */
	protected String replaceVariables(String source, Map<String, String> properties) {
		Matcher matcher = PROPERTY_PATTERN.matcher(source);
		StringBuilder result = new StringBuilder();
		int cursor = 0;
		boolean matchFound = matcher.find();
		if (!matchFound)
			return source;
		while (matchFound) {
			int matchStart = matcher.start();
			int matchEnd = matcher.end();
			result.append(source.substring(cursor, matchStart)); // add the
																	// content
																	// before
																	// the match
			String key = source.substring(matchStart + 2, matchEnd - 1);
			String systemProperty = componentContext == null ? null
					: componentContext.getBundleContext().getProperty(key);
			String providedProperty = null;
			if (properties != null) {
				providedProperty = properties.get(key);
			}
			if (isNotBlank(providedProperty)) {
				result.append(providedProperty);
			} else if (isNotBlank(systemProperty)) {
				result.append(systemProperty);
			} else {
				result.append(source.substring(matchStart, matchEnd)); // retain
																		// the
																		// original
																		// matched
																		// value
			}
			cursor = matchEnd;
			matchFound = matcher.find();
			if (!matchFound)
				result.append(source.substring(matchEnd, source.length()));
		}
		return result.toString();
	}

	/**
	 * Does a lookup of available operation handlers for the given workflow
	 * operation.
	 *
	 * @param operation
	 *            the operation definition
	 * @return the handler or <code>null</code>
	 */
	protected WorkflowOperationHandler selectOperationHandler(WorkflowOperationInstance operation) {
		List<WorkflowOperationHandler> handlerList = new ArrayList<WorkflowOperationHandler>();
		for (HandlerRegistration handlerReg : getRegisteredHandlers()) {
			if (handlerReg.operationName != null && handlerReg.operationName.equals(operation.getTemplate())) {
				handlerList.add(handlerReg.handler);
			}
		}
		if (handlerList.size() > 1) {
			throw new IllegalStateException(
					"Multiple operation handlers found for operation '" + operation.getTemplate() + "'");
		} else if (handlerList.size() == 1) {
			return handlerList.get(0);
		}
		logger.warn("No workflow operation handlers found for operation '%s'", operation.getTemplate());
		return null;
	}

	/**
	 * Executes the workflow.
	 *
	 * @param workflow
	 *            the workflow instance
	 * @throws WorkflowException
	 *             if there is a problem processing the workflow
	 */
	protected Job runWorkflow(WorkflowInstance workflow) throws WorkflowException, UnauthorizedException {
		if (!INSTANTIATED.equals(workflow.getState())) {

			// If the workflow is "running", we need to determine if there is an
			// operation being executed or not.
			// When a workflow has been restarted, this might not be the case
			// and the status might not have been
			// updated accordingly.
			if (RUNNING.equals(workflow.getState())) {
				WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();
				if (currentOperation != null) {
					if (currentOperation.getId() != null) {
						try {
							Job operationJob = serviceRegistry.getJob(currentOperation.getId());
							if (Job.Status.RUNNING.equals(operationJob.getStatus())) {
								logger.debug("Not starting workflow %s, it is already in running state", workflow);
								return null;
							} else {
								logger.info("Scheduling next operation of workflow %s", workflow);
								operationJob.setStatus(Status.QUEUED);
								operationJob.setDispatchable(true);
								return serviceRegistry.updateJob(operationJob);
							}
						} catch (Exception e) {
							logger.warn("Error determining status of current workflow operation in {}: {}", workflow,
									e.getMessage());
							return null;
						}
					}
				} else {
					throw new IllegalStateException(
							"Cannot start a workflow '" + workflow + "' with no current operation");
				}
			} else {
				throw new IllegalStateException("Cannot start a workflow in state '" + workflow.getState() + "'");
			}
		}

		// If this is a new workflow, move to the first operation
		workflow.setState(RUNNING);
		update(workflow);

		WorkflowOperationInstance operation = workflow.getCurrentOperation();

		if (operation == null)
			throw new IllegalStateException("Cannot start a workflow without a current operation");

		if (operation.getPosition() != 0)
			throw new IllegalStateException("Current operation expected to be first");

		try {
			logger.info("Scheduling workflow %s for execution", workflow.getId());
			Job job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
					Arrays.asList(Long.toString(workflow.getId())), null, false, null);
			operation.setId(job.getId());
			update(workflow);
			job.setStatus(Status.QUEUED);
			job.setDispatchable(true);
			return serviceRegistry.updateJob(job);
		} catch (ServiceRegistryException e) {
			throw new WorkflowDatabaseException(e);
		} catch (NotFoundException e) {
			// this should be impossible
			throw new IllegalStateException("Unable to find a job that was just created");
		}

	}

	/**
	 * Executes the workflow's current operation.
	 *
	 * @param workflow
	 *            the workflow
	 * @param properties
	 *            the properties that are passed in on resume
	 * @return the processed workflow operation
	 * @throws WorkflowException
	 *             if there is a problem processing the workflow
	 */
	protected WorkflowOperationInstance runWorkflowOperation(WorkflowInstance workflow, Map<String, String> properties)
			throws WorkflowException, UnauthorizedException {
		WorkflowOperationInstance processingOperation = workflow.getCurrentOperation();
		if (processingOperation == null)
			throw new IllegalStateException("Workflow '" + workflow + "' has no operation to run");

		// Keep the current state for later reference, it might have been
		// changed from the outside
		WorkflowState initialState = workflow.getState();

		// Execute the operation handler
		WorkflowOperationHandler operationHandler = selectOperationHandler(processingOperation);
		WorkflowOperationWorker worker = new WorkflowOperationWorker(operationHandler, workflow, properties, this);
		workflow = worker.execute();

		// The workflow has been serialized/deserialized in between, so we need
		// to refresh the reference
		int currentOperationPosition = processingOperation.getPosition();
		processingOperation = workflow.getOperations().get(currentOperationPosition);

		Long currentOperationJobId = processingOperation.getId();
		try {
			updateOperationJob(currentOperationJobId, processingOperation.getState());
		} catch (NotFoundException e) {
			throw new IllegalStateException("Unable to find a job that has already been running");
		} catch (ServiceRegistryException e) {
			throw new WorkflowDatabaseException(e);
		}

		// Move on to the next workflow operation
		WorkflowOperationInstance currentOperation = workflow.getCurrentOperation();

		// Is the workflow done?
		if (currentOperation == null) {

			// If we are in failing mode, we were simply working off an error
			// handling workflow
			if (FAILING.equals(workflow.getState())) {
				workflow.setState(FAILED);
			}

			// Otherwise, let's make sure we didn't miss any failed operation,
			// since the workflow state could have been
			// switched to paused while processing the error handling workflow
			// extension
			else if (!FAILED.equals(workflow.getState())) {
				workflow.setState(SUCCEEDED);
				for (WorkflowOperationInstance op : workflow.getOperations()) {
					if (op.getState().equals(WorkflowOperationInstance.OperationState.FAILED)) {
						if (op.isFailWorkflowOnException()) {
							workflow.setState(FAILED);
							break;
						}
					}
				}
			}

			// Save the updated workflow to the database
			logger.debug("%s has %s", workflow, workflow.getState());
			update(workflow);

		} else {

			// Somebody might have set the workflow to "paused" from the
			// outside, so take a look a the database first
			WorkflowState dbWorkflowState = null;
			try {
				dbWorkflowState = getWorkflowById(workflow.getId()).getState();
			} catch (WorkflowDatabaseException e) {
				throw new IllegalStateException(
						"The workflow with ID " + workflow.getId() + " can not be accessed in the database", e);
			} catch (NotFoundException e) {
				throw new IllegalStateException(
						"The workflow with ID " + workflow.getId() + " can not be found in the database", e);
			} catch (UnauthorizedException e) {
				throw new IllegalStateException("The workflow with ID " + workflow.getId() + " can not be read", e);
			}

			// If somebody changed the workflow state from the outside, that
			// state should take precedence
			if (!dbWorkflowState.equals(initialState)) {
				logger.info("Workflow state for %s was changed to '%s' from the outside", workflow, dbWorkflowState);
				workflow.setState(dbWorkflowState);
			}

			// Save the updated workflow to the database

			Job job = null;
			switch (workflow.getState()) {
			case FAILED:
				update(workflow);
				break;
			case FAILING:
			case RUNNING:
				try {
					job = serviceRegistry.createJob(JOB_TYPE, Operation.START_OPERATION.toString(),
							Arrays.asList(Long.toString(workflow.getId())), null, false, null);
					currentOperation.setId(job.getId());
					update(workflow);
					job.setStatus(Status.QUEUED);
					job.setDispatchable(true);
					serviceRegistry.updateJob(job);
				} catch (ServiceRegistryException e) {
					throw new WorkflowDatabaseException(e);
				} catch (NotFoundException e) {
					// this should be impossible
					throw new IllegalStateException("Unable to find a job that was just created");
				}
				break;
			case PAUSED:
			case STOPPED:
			case SUCCEEDED:
				update(workflow);
				break;
			case INSTANTIATED:
				update(workflow);
				throw new IllegalStateException("Impossible workflow state found during processing");
			default:
				throw new IllegalStateException("Unkown workflow state found during processing");
			}
		}
		return processingOperation;
	}

	/**
	 * Processes the workflow job.
	 *
	 * @param job
	 *            the job
	 * @return the job payload
	 * @throws Exception
	 *             if job processing fails
	 */
	protected String process(Job job) throws Exception {
		List<String> arguments = job.getArguments();
		Operation op = null;
		WorkflowInstance workflowInstance = null;
		WorkflowOperationInstance wfo = null;
		String operation = job.getOperation();
		try {
			try {
				op = Operation.valueOf(operation);
				switch (op) {
				case START_WORKFLOW:
					workflowInstance = WorkflowParser.parseWorkflowInstance(job.getPayload());
					logger.debug("Starting new workflow %s", workflowInstance);
					runWorkflow(workflowInstance);
					break;
				case RESUME:
					workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
					wfo = workflowInstance.getCurrentOperation();
					Map<String, String> properties = null;
					if (arguments.size() > 1) {
						Properties props = new Properties();
						props.load(IOUtils.toInputStream(arguments.get(arguments.size() - 1)));
						properties = new HashMap<String, String>();
						for (Entry<Object, Object> entry : props.entrySet()) {
							properties.put(entry.getKey().toString(), entry.getValue().toString());
						}
					}
					logger.debug("Resuming %s at %s", workflowInstance, workflowInstance.getCurrentOperation());
					workflowInstance.setState(RUNNING);
					update(workflowInstance);
					wfo = runWorkflowOperation(workflowInstance, properties);
					break;
				case START_OPERATION:
					workflowInstance = getWorkflowById(Long.parseLong(arguments.get(0)));
					wfo = workflowInstance.getCurrentOperation();

					if (OperationState.RUNNING.equals(wfo.getState()) || OperationState.PAUSED.equals(wfo.getState())) {
						logger.info("Reset operation state %s %s to INSTANTIATED due to job restart", workflowInstance,
								wfo);
						wfo.setState(OperationState.INSTANTIATED);
					}

					wfo.setExecutionHost(job.getProcessingHost());
					logger.debug("Running %s %s", workflowInstance, wfo);
					wfo = runWorkflowOperation(workflowInstance, null);
					updateOperationJob(job.getId(), wfo.getState());
					break;
				default:
					throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
				}
			} catch (IllegalArgumentException e) {
				throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
			} catch (IndexOutOfBoundsException e) {
				throw new ServiceRegistryException(
						"This argument list for operation '" + op + "' does not meet expectations", e);
			} catch (NotFoundException e) {
				logger.warn(e.getMessage());
				updateOperationJob(job.getId(), OperationState.FAILED);
			}
			return null;
		} catch (Exception e) {
			logger.warn(e, "Exception while accepting job " + job);
			try {
				if (workflowInstance != null) {
					logger.warn("Marking workflow instance %s as failed", workflowInstance);
					workflowInstance.setState(FAILED);
					update(workflowInstance);
				} else {
					logger.warn(e, "Unable to parse workflow instance");
				}
			} catch (WorkflowDatabaseException e1) {
				throw new ServiceRegistryException(e1);
			}
			if (e instanceof ServiceRegistryException)
				throw e;
			throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
		}
	}

	/**
	 * Synchronizes the workflow operation's job with the operation status if
	 * the operation has a job associated with it, which is determined by
	 * looking at the operation's job id.
	 *
	 * @param state
	 *            the operation state
	 * @param jobId
	 *            the associated job
	 * @return the updated job or <code>null</code> if there is no job for this
	 *         operation
	 * @throws ServiceRegistryException
	 *             if the job can't be updated in the service registry
	 * @throws NotFoundException
	 *             if the job can't be found
	 */
	private Job updateOperationJob(Long jobId, OperationState state)
			throws NotFoundException, ServiceRegistryException {
		if (jobId == null)
			return null;
		Job job = serviceRegistry.getJob(jobId);
		switch (state) {
		case FAILED:
		case RETRY:
			job.setStatus(Status.FAILED);
			break;
		case PAUSED:
			job.setStatus(Status.PAUSED);
			job.setOperation(Operation.RESUME.toString());
			break;
		case SKIPPED:
		case SUCCEEDED:
			job.setStatus(Status.FINISHED);
			break;
		default:
			throw new IllegalStateException("Unexpected state '" + state + "' found");
		}
		return serviceRegistry.updateJob(job);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * cleanupWorkflowInstances(int,
	 * com.capestartproject.workflow.api.WorkflowInstance.WorkflowState)
	 */
	@Override
	public void cleanupWorkflowInstances(int lifetime, WorkflowState state)
			throws UnauthorizedException, WorkflowDatabaseException {
		logger.info("Start cleaning up workflow instances older than {} days with status '{}'", lifetime, state);

		int instancesCleaned = 0;
		int cleaningFailed = 0;

		WorkflowQuery query = new WorkflowQuery().withState(state)
				.withDateBefore(DateUtils.addDays(new Date(), -lifetime)).withCount(Integer.MAX_VALUE);
		for (WorkflowInstance workflowInstance : getWorkflowInstances(query).getItems()) {
			try {
				remove(workflowInstance.getId());
				instancesCleaned++;
			} catch (WorkflowDatabaseException e) {
				throw e;
			} catch (UnauthorizedException e) {
				throw e;
			} catch (NotFoundException e) {
				// Since we are in a cleanup operation, we don't have to care
				// about NotFoundExceptions
				logger.debug("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(), e);
			} catch (WorkflowParsingException e) {
				logger.warn("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(), e);
				cleaningFailed++;
			} catch (WorkflowStateException e) {
				logger.warn("Workflow instance '{}' could not be removed: {}", workflowInstance.getId(), e);
				cleaningFailed++;
			}
		}

		if (instancesCleaned == 0 && cleaningFailed == 0) {
			logger.info("No workflow instances found to clean up");
			return;
		}

		if (instancesCleaned > 0)
			logger.info("Cleaned up {} workflow instances", instancesCleaned);
		if (cleaningFailed > 0) {
			logger.warn("Cleaning failed for {} workflow instances", cleaningFailed);
			throw new WorkflowDatabaseException("Unable to clean all workflow instances, see logs!");
		}
	}
}
