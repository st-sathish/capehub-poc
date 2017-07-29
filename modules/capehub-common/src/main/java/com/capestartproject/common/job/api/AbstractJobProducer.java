package com.capestartproject.common.job.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.Incident.Severity;
import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.Incidents;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.UndispatchableJobException;
import com.capestartproject.common.util.JobCanceledException;
import com.capestartproject.common.util.NotFoundException;

/**
 * This class serves as a convenience for services that implement the {@link JobProducer} api to deal with handling long
 * running, asynchronous operations.
 */
public abstract class AbstractJobProducer implements JobProducer {

  /** The logger */
  static final Logger logger = LoggerFactory.getLogger(AbstractJobProducer.class);

  /** The types of job that this producer can handle */
  protected String jobType = null;

  /** To enable threading when dispatching jobs */
  protected ExecutorService executor = Executors.newCachedThreadPool();

  /**
   * Creates a new abstract job producer for jobs of the given type.
   *
   * @param jobType
   *         the job type
   */
  public AbstractJobProducer(String jobType) {
    this.jobType = jobType;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#getJobType()
   */
  @Override
  public String getJobType() {
    return jobType;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#countJobs(org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countJobs(Status status) throws ServiceRegistryException {
    if (status == null)
      throw new IllegalArgumentException("Status must not be null");
    return getServiceRegistry().count(getJobType(), status);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job)
   */
  @Override
  public void acceptJob(Job job) throws ServiceRegistryException {
    try {
      job.setStatus(Job.Status.RUNNING);
      getServiceRegistry().updateJob(job);
    } catch (NotFoundException e) {
      throw new IllegalStateException(e);
    }
    executor.submit(new JobRunner(job, getServiceRegistry().getCurrentJob()));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#isReadyToAcceptJobs(String)
   */
  @Override
  public boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.JobProducer#isReadyToAccept(org.opencastproject.job.api.Job)
   */
  @Override
  public boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException {
    return true;
  }

  /** Shorthand for {@link #getServiceRegistry()}.incident() */
  public Incidents incident() {
    return getServiceRegistry().incident();
  }

  /**
   * Returns a reference to the service registry.
   *
   * @return the service registry
   */
  protected abstract ServiceRegistry getServiceRegistry();

  /**
   * Returns a reference to the security service
   *
   * @return the security service
   */
  protected abstract SecurityService getSecurityService();

  /**
   * Returns a reference to the user directory service
   *
   * @return the user directory service
   */
  protected abstract UserDirectoryService getUserDirectoryService();

  /**
   * Returns a reference to the organization directory service.
   *
   * @return the organization directory service
   */
  protected abstract OrganizationDirectoryService getOrganizationDirectoryService();

  /**
   * Asks the overriding class to process the arguments using the given operation. The result will be added to the
   * associated job as the payload.
   *
   * @param job
   *         the job to process
   * @return the operation result
   * @throws Exception
   */
  protected abstract String process(Job job) throws Exception;

  /** A utility class to run jobs */
  class JobRunner implements Callable<Void> {

    /** The job to dispatch */
    private final Job job;

    /** The current job */
    private final Job currentJob;

    /**
     * Constructs a new job runner
     *
     * @param job
     *         the job to run
     * @param currentJob
     *         the current running job
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
      SecurityService securityService = getSecurityService();
      try {
        Organization organization = getOrganizationDirectoryService().getOrganization(job.getOrganization());
        getServiceRegistry().setCurrentJob(currentJob);
        securityService.setOrganization(organization);
        User user = getUserDirectoryService().loadUser(job.getCreator());
        securityService.setUser(user);
        String payload = process(job);
        if (job.getStatus() == Status.FAILED) {
          logger.warn("Error handling operation '{}' of job {}", job.getOperation(), job.getId());
          return null;
        }
        job.setPayload(payload);
        job.setStatus(Status.FINISHED);
      } catch (JobCanceledException e) {
        logger.info(e.getMessage());
      } catch (Throwable e) {
        job.setStatus(Status.FAILED);
        getServiceRegistry().incident().unhandledException(job, Severity.FAILURE, e);
        logger.error("Error handling operation '{}': {}", job.getOperation(), e);
        if (e instanceof ServiceRegistryException)
          throw (ServiceRegistryException) e;
      } finally {
        try {
          getServiceRegistry().updateJob(job);
        } catch (NotFoundException e) {
          throw new ServiceRegistryException(e);
        } finally {
          getServiceRegistry().setCurrentJob(null);
          securityService.setUser(null);
          securityService.setOrganization(null);
        }
      }
      return null;
    }
  }

}
