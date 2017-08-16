package com.capestartproject.workflow.api;

import static com.capestartproject.common.util.data.Option.option;
import static com.capestartproject.common.util.data.functions.Misc.chuck;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

import com.capestartproject.common.emppackage.EmployeePackage;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.JobBarrier;
import com.capestartproject.common.job.api.JobContext;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.workflow.api.WorkflowOperationResult.Action;

/**
 * Abstract base implementation for an operation handler, which implements a
 * simple start operation that returns a {@link WorkflowOperationResult} with
 * the current employeepackage and {@link Action#CONTINUE}.
 */
public abstract class AbstractWorkflowOperationHandler implements WorkflowOperationHandler {

  /** The ID of this operation handler */
  protected String id = null;

  /** The description of what this handler actually does */
  protected String description = null;

  /** The configuration options for this operation handler */
  protected SortedMap<String, String> options = new TreeMap<String, String>();

  /** Optional service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   *
   * @param cc
   *          The component's context, containing the properties used for configuration
   */
  protected void activate(ComponentContext cc) {
    this.id = (String) cc.getProperties().get(WorkflowService.WORKFLOW_OPERATION_PROPERTY);
    this.description = (String) cc.getProperties().get(Constants.SERVICE_DESCRIPTION);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
	 *      JobContext)
	 */
  @Override
  public abstract WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException;

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#skip(org.opencastproject.workflow.api.WorkflowInstance,
	 *      JobContext)
	 */
  @Override
  public WorkflowOperationResult skip(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    return createResult(Action.SKIP);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#destroy(org.opencastproject.workflow.api.WorkflowInstance,
	 *      JobContext)
	 */
  @Override
  public void destroy(WorkflowInstance workflowInstance, JobContext context) throws WorkflowOperationException {
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
	 */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return options;
  }

  /**
   * Adds a configuration option to the list of possible configuration options.
   *
   * @param name
   *          the option name
   * @param description
   *          the option description
   */
  public void addConfigurationOption(String name, String description) {
    options.put(name, description);
  }

  /**
   * Removes the configuration option from the list of possible configuration options.
   *
   * @param name
   *          the option name
   */
  public void removeConfigurationOption(String name) {
    options.remove(name);
  }

  /**
   * Converts a comma separated string into a set of values. Useful for converting operation configuration strings into
   * multi-valued sets.
   *
   * @param elements
   *          The comma space separated string
   * @return the set of values
   */
  protected List<String> asList(String elements) {
    elements = StringUtils.trimToNull(elements);
    List<String> list = new ArrayList<String>();
    if (elements != null) {
      for (String s : StringUtils.split(elements, ",")) {
        if (StringUtils.trimToNull(s) != null) {
          list.add(s.trim());
        }
      }
    }
    return list;
  }

  /** {@link #asList(String)} as a function. */
  protected Function<String, List<String>> asList = new Function<String, List<String>>() {
    @Override public List<String> apply(String s) {
      return asList(s);
    }
  };

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#getId()
	 */
  @Override
  public String getId() {
    return id;
  }

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.workflow.api.WorkflowOperationHandler#getDescription()
	 */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   *
   * @param action
   *          the action to take
   * @return the result
   */
  protected WorkflowOperationResult createResult(Action action) {
    return createResult(null, null, action, 0);
  }

  /**
   * Creates a result for the execution of this workflow operation handler.
   * <p>
   * Since there is no way for the workflow service to determine the queuing time (e. g. waiting on services), it needs
   * to be provided by the handler.
   *
   * @param action
   *          the action to take
   * @param timeInQueue
   *          the amount of time this handle spent waiting for services
   * @return the result
   */
  protected WorkflowOperationResult createResult(Action action, long timeInQueue) {
    return createResult(null, null, action, timeInQueue);
  }

  	/**
	 * Creates a result for the execution of this workflow operation handler.
	 *
	 * @param employeePackage
	 *            the modified employeepackage
	 * @param action
	 *            the action to take
	 * @return the result
	 */
	protected WorkflowOperationResult createResult(EmployeePackage employeePackage, Action action) {
		return createResult(employeePackage, null, action, 0);
	}

	/**
	 * Creates a result for the execution of this workflow operation handler.
	 * <p>
	 * Since there is no way for the workflow service to determine the queuing
	 * time (e. g. waiting on services), it needs to be provided by the handler.
	 *
	 * @param employeePackage
	 *            the modified employeepackage
	 * @param action
	 *            the action to take
	 * @param timeInQueue
	 *            the amount of time this handle spent waiting for services
	 * @return the result
	 */
	protected WorkflowOperationResult createResult(EmployeePackage employeePackage, Action action, long timeInQueue) {
		return createResult(employeePackage, null, action, timeInQueue);
  }

  	/**
	 * Creates a result for the execution of this workflow operation handler.
	 * <p>
	 * Since there is no way for the workflow service to determine the queuing
	 * time (e. g. waiting on services), it needs to be provided by the handler.
	 *
	 * @param employeepackage
	 *            the modified employeepackage
	 * @param properties
	 *            the properties to add to the workflow instance
	 * @param action
	 *            the action to take
	 * @param timeInQueue
	 *            the amount of time this handle spent waiting for services
	 * @return the result
	 */
	protected WorkflowOperationResult createResult(EmployeePackage employeepackage, Map<String, String> properties,
          Action action, long timeInQueue) {
		return new WorkflowOperationResultImpl(employeepackage, properties, action, timeInQueue);
  }

  /**
   * Sets the service registry. This method is here as a convenience for developers that need the registry to do job
   * waiting.
   *
   * @param serviceRegistry
   *          the service registry
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Waits until all of the jobs have reached either one of these statuses:
   * <ul>
   * <li>{@link Job.Status#FINISHED}</li>
   * <li>{@link Job.Status#FAILED}</li>
   * <li>{@link Job.Status#DELETED}</li>
   * </ul>
   * After that, the method returns with the actual outcomes of the jobs.
   *
   * @param jobs
   *          the jobs
   * @return the jobs and their outcomes
   * @throws IllegalStateException
   *           if the service registry has not been set
   * @throws IllegalArgumentException
   *           if the jobs collecion is either <code>null</code> or empty
   */
  protected JobBarrier.Result waitForStatus(Job... jobs) throws IllegalStateException, IllegalArgumentException {
    return waitForStatus(0, jobs);
  }

  /**
   * Waits until all of the jobs have reached either one of these statuses:
   * <ul>
   * <li>{@link Job.Status#FINISHED}</li>
   * <li>{@link Job.Status#FAILED}</li>
   * <li>{@link Job.Status#DELETED}</li>
   * </ul>
   * After that, the method returns with the actual outcomes of the jobs.
   *
   * @param timeout
   *          the maximum amount of time in miliseconds to wait
   * @param jobs
   *          the jobs
   * @return the jobs and their outcomes
   * @throws IllegalStateException
   *           if the service registry has not been set
   * @throws IllegalArgumentException
   *           if the jobs collecion is either <code>null</code> or empty
   */
  protected JobBarrier.Result waitForStatus(long timeout, Job... jobs) throws IllegalStateException,
          IllegalArgumentException {
    if (serviceRegistry == null)
      throw new IllegalStateException("Can't wait for job status without providing a service registry first");
    JobBarrier barrier = new JobBarrier(serviceRegistry, jobs);
    return barrier.waitForJobs(timeout);
  }

  /** Get a configuration option. */
  protected Option<String> getCfg(WorkflowInstance wi, String key) {
    return option(wi.getCurrentOperation().getConfiguration(key));
  }

  /**
   * Create an error function.
   * <p/>
   * Example usage: <code>getCfg(wi, "key").getOrElse(this.&lt;String&gt;cfgKeyMissing("key"))</code>
   *
   * @see #getCfg(WorkflowInstance, String)
   */
  protected <A> Function0<A> cfgKeyMissing(final String key) {
    return new Function0<A>() {
      @Override public A apply() {
        return chuck(new WorkflowOperationException(key + " is missing or malformed"));
      }
    };
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : super.hashCode();
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WorkflowOperationHandler) {
      if (id != null)
        return id.equals(((WorkflowOperationHandler) obj).getId());
      else
        return this == obj;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return getId();
  }
}
