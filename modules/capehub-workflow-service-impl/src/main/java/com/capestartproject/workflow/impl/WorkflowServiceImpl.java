package com.capestartproject.workflow.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import javax.management.ObjectInstance;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

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
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.workflow.api.WorkflowDatabaseException;
import com.capestartproject.workflow.api.WorkflowDefinition;
import com.capestartproject.workflow.api.WorkflowException;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowInstance.WorkflowState;
import com.capestartproject.workflow.api.WorkflowListener;
import com.capestartproject.workflow.api.WorkflowOperationDefinition;
import com.capestartproject.workflow.api.WorkflowOperationDefinitionImpl;
import com.capestartproject.workflow.api.WorkflowOperationHandler;
import com.capestartproject.workflow.api.WorkflowOperationInstance;
import com.capestartproject.workflow.api.WorkflowOperationInstance.OperationState;
import com.capestartproject.workflow.api.WorkflowOperationInstanceImpl;
import com.capestartproject.workflow.api.WorkflowOperationResult;
import com.capestartproject.workflow.api.WorkflowParsingException;
import com.capestartproject.workflow.api.WorkflowQuery;
import com.capestartproject.workflow.api.WorkflowService;
import com.capestartproject.workflow.api.WorkflowSet;
import com.capestartproject.workflow.api.WorkflowStateException;
import com.capestartproject.workflow.api.WorkflowStatistics;
import com.capestartproject.workflow.impl.jmx.WorkflowsStatistics;

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

  /** The collection of workflow definitions */
  // protected Map<String, WorkflowDefinition> workflowDefinitions = new HashMap<String, WorkflowDefinition>();

  /** The list of workflow listeners */
  private final List<WorkflowListener> listeners = new CopyOnWriteArrayList<WorkflowListener>();

  /** The thread pool to use for firing listeners and handling dispatched jobs */
  protected ThreadPoolExecutor executorService;

  /** The workspace */
	// protected Workspace workspace = null;

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
	 * Constructs a new workflow service impl, with a priority-sorted map of
	 * metadata services
	 */
  public WorkflowServiceImpl() {

  }

  /**
   * Activate this service implementation via the OSGI service component runtime.
   *
   * @param componentContext
   *          the component context
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
	 * @see com.capestartproject.workflow.api.WorkflowService#addWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
	 */
  @Override
  public void addWorkflowListener(WorkflowListener listener) {
    listeners.add(listener);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowService#removeWorkflowListener(org.opencastproject.workflow.api.WorkflowListener)
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
   * Tests the workflow definition for its runnability. This method is a helper for
   * {@link #isRunnable(WorkflowDefinition)} that is suited for recursive calling.
   *
   * @param workflowDefinition
   *          the definition to test
   * @param availableOperations
   *          list of currently available operation handlers
   * @param checkedWorkflows
   *          list of checked workflows, used to avoid circular checking
   * @return <code>true</code> if all bits and pieces used for executing <code>workflowDefinition</code> are in place
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

		return null;
	}

  	/**
	 * {@inheritDoc}
	 *
	 * @see org.opencastproject.workflow.api.WorkflowService#listAvailableWorkflowDefinitions()
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
	 */
	@Override
	public void updated(Dictionary properties) throws ConfigurationException {
		// TODO Auto-generated method stub

	}

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
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * registerWorkflowDefinition(com.capestartproject.workflow.api.
	 * WorkflowDefinition)
	 */
	@Override
	public void registerWorkflowDefinition(WorkflowDefinition workflow) throws WorkflowDatabaseException {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * getWorkflowDefinitionById(java.lang.String)
	 */
	@Override
	public WorkflowDefinition getWorkflowDefinitionById(String id) throws WorkflowDatabaseException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.workflow.api.WorkflowService#getWorkflowById(long)
	 */
	@Override
	public WorkflowInstance getWorkflowById(long workflowId)
			throws WorkflowDatabaseException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#
	 * getWorkflowInstancesForAdministrativeRead(com.capestartproject.workflow.
	 * api.WorkflowQuery)
	 */
	@Override
	public WorkflowSet getWorkflowInstancesForAdministrativeRead(WorkflowQuery q)
			throws WorkflowDatabaseException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.employeepackage.EmployeePackage,
	 * java.util.Map)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition,
			Map<String, String> properties) throws WorkflowDatabaseException, WorkflowParsingException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.employeepackage.EmployeePackage,
	 * java.lang.Long, java.util.Map)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition,
			Long parentWorkflowId, Map<String, String> properties)
			throws WorkflowDatabaseException, WorkflowParsingException, NotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#start(com.
	 * capestartproject.workflow.api.WorkflowDefinition,
	 * com.capestartproject.common.employeepackage.EmployeePackage)
	 */
	@Override
	public WorkflowInstance start(WorkflowDefinition workflowDefinition)
			throws WorkflowDatabaseException, WorkflowParsingException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#getStatistics()
	 */
	@Override
	public WorkflowStatistics getStatistics() throws WorkflowDatabaseException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#stop(long)
	 */
	@Override
	public WorkflowInstance stop(long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#remove(long)
	 */
	@Override
	public void remove(long workflowInstanceId) throws WorkflowDatabaseException, WorkflowParsingException,
			NotFoundException, UnauthorizedException, WorkflowStateException {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#suspend(long)
	 */
	@Override
	public WorkflowInstance suspend(long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#resume(long)
	 */
	@Override
	public WorkflowInstance resume(long workflowInstanceId)
			throws NotFoundException, WorkflowException, IllegalStateException, UnauthorizedException {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.workflow.api.WorkflowService#update(com.
	 * capestartproject.workflow.api.WorkflowInstance)
	 */
	@Override
	public void update(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException {

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
			throws WorkflowDatabaseException, UnauthorizedException {
		// TODO Auto-generated method stub

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
				// process(job);
			} finally {
				serviceRegistry.setCurrentJob(null);
				securityService.setUser(null);
				securityService.setOrganization(null);
			}
			return null;
		}
	}
}
