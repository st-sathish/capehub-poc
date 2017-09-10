package com.capestartproject.serviceregistry.impl;

import static com.capestartproject.common.job.api.Job.FailureReason.DATA;
import static com.capestartproject.common.job.api.Job.Status.FAILED;
import static com.capestartproject.common.security.api.SecurityConstants.ORGANIZATION_HEADER;
import static com.capestartproject.common.security.api.SecurityConstants.USER_HEADER;
import static com.capestartproject.common.serviceregistry.api.ServiceState.ERROR;
import static com.capestartproject.common.serviceregistry.api.ServiceState.NORMAL;
import static com.capestartproject.common.serviceregistry.api.ServiceState.WARNING;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.spi.PersistenceProvider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.JaxbJob;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.job.api.JobParser;
import com.capestartproject.common.rest.RestConstants;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.TrustedHttpClient;
import com.capestartproject.common.security.api.TrustedHttpClientException;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.HostRegistration;
import com.capestartproject.common.serviceregistry.api.IncidentService;
import com.capestartproject.common.serviceregistry.api.Incidents;
import com.capestartproject.common.serviceregistry.api.JaxbServiceRegistration;
import com.capestartproject.common.serviceregistry.api.JaxbServiceStatistics;
import com.capestartproject.common.serviceregistry.api.ServiceRegistration;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.ServiceStatistics;
import com.capestartproject.common.serviceregistry.api.SystemLoad;
import com.capestartproject.common.systems.CapehubConstans;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.UrlSupport;
import com.capestartproject.common.util.jmx.JmxUtil;
import com.capestartproject.serviceregistry.impl.jmx.HostsStatistics;
import com.capestartproject.serviceregistry.impl.jmx.JobsStatistics;
import com.capestartproject.serviceregistry.impl.jmx.ServicesStatistics;

/** JPA implementation of the {@link ServiceRegistry} */
public class ServiceRegistryJpaImpl implements ServiceRegistry, ManagedService {

  /** Id of the workflow's start operation operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String START_OPERATION = "START_OPERATION";

  /** Id of the workflow's start workflow operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String START_WORKFLOW = "START_WORKFLOW";

  /** Id of the workflow's resume operation, need to match the corresponding enum value in WorkflowServiceImpl */
  public static final String RESUME = "RESUME";

  /** Identifier for the workflow service */
  public static final String TYPE_WORKFLOW = "org.opencastproject.workflow";

  static final Logger logger = LoggerFactory.getLogger(ServiceRegistryJpaImpl.class);

  /** The list of registered JMX beans */
  protected List<ObjectInstance> jmxBeans = new ArrayList<ObjectInstance>();

  /** Hosts statistics JMX type */
  private static final String JMX_HOSTS_STATISTICS_TYPE = "HostsStatistics";

  /** Services statistics JMX type */
  private static final String JMX_SERVICES_STATISTICS_TYPE = "ServicesStatistics";

  /** Jobs statistics JMX type */
  private static final String JMX_JOBS_STATISTICS_TYPE = "JobsStatistics";

  /** The JMX business object for hosts statistics */
  private HostsStatistics hostsStatistics;

  /** The JMX business object for services statistics */
  private ServicesStatistics servicesStatistics;

  /** The JMX business object for jobs statistics */
  private JobsStatistics jobsStatistics;

  /** Current job used to process job in the service registry */
  private static final ThreadLocal<Job> currentJob = new ThreadLocal<Job>();

  /** Configuration key for the maximum load */
  protected static final String OPT_MAXLOAD = "org.opencastproject.server.maxload";

  /** Configuration key for the dispatch interval in milliseconds */
  protected static final String OPT_DISPATCHINTERVAL = "dispatchinterval";

  /** Configuration key for the interval to check whether the hosts in the service registry are still alive [sec] * */
  protected static final String OPT_HEARTBEATINTERVAL = "heartbeat.interval";

  /** Configuration key for the collection of job statistics */
  protected static final String OPT_JOBSTATISTICS = "jobstats.collect";

  /** The http client to use when connecting to remote servers */
  protected TrustedHttpClient client = null;

  /** Minimum delay between job dispatching attempts, in milliseconds */
  static final long MIN_DISPATCH_INTERVAL = 1000;

  /** Default delay between job dispatching attempts, in milliseconds */
  static final long DEFAULT_DISPATCH_INTERVAL = 5000;

  /** Default setting on job statistics collection */
  static final boolean DEFAULT_JOB_STATISTICS = true;

  /** Default value for {@link #maxAttemptsBeforeErrorState} */
  private static final int MAX_FAILURE_BEFORE_ERROR_STATE = 1;

  /** The configuration key for setting {@link #maxAttemptsBeforeErrorState} */
  private static final String MAX_ATTEMPTS_CONFIG_KEY = "max.attempts";

  /** Number of failed jobs on a service before to set it in error state */
  protected int maxAttemptsBeforeErrorState = MAX_FAILURE_BEFORE_ERROR_STATE;

  /** Default delay between checking if hosts are still alive in seconds * */
  static final long DEFAULT_HEART_BEAT = 60;

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider;

  /** This host's base URL */
  protected String hostName;

  /** The base URL for job URLs */
  protected String jobHost;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** Tracks services published locally and adds them to the service registry */
  protected RestServiceTracker tracker = null;

  /** The thread pool to use for dispatching queued jobs and checking on phantom services. */
  protected ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  protected Incidents incidents;

  /** Whether to collect detailed job statistics */
  protected boolean collectJobstats = DEFAULT_JOB_STATISTICS;

  protected Map<String, Object> persistenceProperties;

  /** A static list of statuses that influence how load balancing is calculated */
  protected static final List<Status> JOB_STATUSES_INFLUENCING_LOAD_BALANCING;

  static {
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING = new ArrayList<Status>();
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.DISPATCHING);
    JOB_STATUSES_INFLUENCING_LOAD_BALANCING.add(Status.RUNNING);
  }

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activate service registery");

    logger.debug("activate");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.serviceregistry", persistenceProperties);

    // Clean all undispatchable jobs
    cleanUndispatchableJobs();

    // Find this host's url
		if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(CapehubConstans.SERVER_URL_PROPERTY))) {
      hostName = UrlSupport.DEFAULT_BASE_URL;
    } else {
			hostName = cc.getBundleContext().getProperty(CapehubConstans.SERVER_URL_PROPERTY);
    }

    // Register JMX beans with statistics
    try {
      List<ServiceStatistics> serviceStatistics = getServiceStatistics();
      hostsStatistics = new HostsStatistics(serviceStatistics);
      servicesStatistics = new ServicesStatistics(hostName, serviceStatistics);
      jobsStatistics = new JobsStatistics(hostName);
      jmxBeans.add(JmxUtil.registerMXBean(hostsStatistics, JMX_HOSTS_STATISTICS_TYPE));
      jmxBeans.add(JmxUtil.registerMXBean(servicesStatistics, JMX_SERVICES_STATISTICS_TYPE));
      jmxBeans.add(JmxUtil.registerMXBean(jobsStatistics, JMX_JOBS_STATISTICS_TYPE));
    } catch (ServiceRegistryException e) {
      logger.error("Error registarting JMX statistic beans {}", e);
    }

    // Find the jobs URL
    if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty("org.opencastproject.jobs.url"))) {
      jobHost = hostName;
    } else {
      jobHost = cc.getBundleContext().getProperty("org.opencastproject.jobs.url");
    }

    // Register this host
    try {
      int maxJobs = -1;
      if (cc == null || StringUtils.isBlank(cc.getBundleContext().getProperty(OPT_MAXLOAD))) {
        maxJobs = Runtime.getRuntime().availableProcessors();
      } else {
        try {
          maxJobs = Integer.parseInt(cc.getBundleContext().getProperty(OPT_MAXLOAD));
        } catch (NumberFormatException e) {
          maxJobs = Runtime.getRuntime().availableProcessors();
          logger.warn("Configuration key '{}' is not an integer. Falling back to the number of cores ({})",
                  OPT_MAXLOAD, maxJobs);
        }
      }

      registerHost(hostName, maxJobs);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to register host " + hostName + " in the service registry", e);
    }

    // Track any services from this host that need to be added to the service registry
    if (cc != null) {
      try {
        tracker = new RestServiceTracker(cc.getBundleContext());
        tracker.open(true);
      } catch (InvalidSyntaxException e) {
        logger.error("Invlid filter syntax: {}", e);
        throw new IllegalStateException(e);
      }
    }

    // Schedule the heartbeat with the default interval
    scheduledExecutor.scheduleWithFixedDelay(new JobProducerHeartbeat(), DEFAULT_HEART_BEAT, DEFAULT_HEART_BEAT,
            TimeUnit.SECONDS);

    // Schedule the job dispatching with the default interval
    scheduledExecutor.scheduleWithFixedDelay(new JobDispatcher(), DEFAULT_DISPATCH_INTERVAL, DEFAULT_DISPATCH_INTERVAL,
            TimeUnit.MILLISECONDS);
  }

  public void deactivate() {
    logger.debug("deactivate");

    for (ObjectInstance mbean : jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }

    if (tracker != null) {
      tracker.close();
    }
    try {
      unregisterHost(hostName);
    } catch (ServiceRegistryException e) {
      throw new IllegalStateException("Unable to unregister host " + hostName + " from the service registry", e);
    }
    if (emf != null) {
      emf.close();
    }

    // Stop the job dispatcher
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdownNow();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, null, null, true, getCurrentJob());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, null, true, getCurrentJob());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, java.lang.String)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, true, getCurrentJob());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(java.lang.String, java.lang.String,
   *      java.util.List, String, boolean)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable)
          throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, getCurrentJob());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#createJob(String, String, List, String, boolean, Job)
   */
  @Override
  public Job createJob(String type, String operation, List<String> arguments, String payload, boolean dispatchable,
          Job parentJob) throws ServiceRegistryException {
    return createJob(this.hostName, type, operation, arguments, payload, dispatchable, parentJob);
  }

  /** Creates a job on a remote host. */
  public Job createJob(String host, String serviceType, String operation, List<String> arguments, String payload,
          boolean dispatchable, Job parentJob) throws ServiceRegistryException {
    if (StringUtils.isBlank(host)) {
      throw new IllegalArgumentException("Host can't be null");
    }
    if (StringUtils.isBlank(serviceType)) {
      throw new IllegalArgumentException("Service type can't be null");
    }
    if (StringUtils.isBlank(operation)) {
      throw new IllegalArgumentException("Operation can't be null");
    }

    User currentUser = securityService.getUser();
    Organization currentOrganization = securityService.getOrganization();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      ServiceRegistrationJpaImpl creatingService = getServiceRegistration(em, serviceType, host);
      if (creatingService == null) {
        throw new ServiceRegistryException("No service registration exists for type '" + serviceType + "' on host '"
                + host + "'");
      }
      if (creatingService.getHostRegistration().isMaintenanceMode()) {
        logger.warn("Creating a job from {}, which is currently in maintenance mode.", creatingService.getHost());
      } else if (!creatingService.getHostRegistration().isActive()) {
        logger.warn("Creating a job from {}, which is currently inactive.", creatingService.getHost());
      }
      JobJpaImpl job = new JobJpaImpl(currentUser, currentOrganization, creatingService, operation, arguments, payload,
              dispatchable);

      // Bind the given parent job to the new job
      if (parentJob != null) {

        // Get the JPA instance of the parent job
        JobJpaImpl jpaParentJob;
        if (parentJob instanceof JobJpaImpl)
          jpaParentJob = (JobJpaImpl) parentJob;
        else {
          try {
            jpaParentJob = (JobJpaImpl) getJob(parentJob.getId());
          } catch (NotFoundException e) {
            logger.error("{} not found in the persistence context", parentJob);
            throw new ServiceRegistryException(e);
          }
        }
        job.setParentJob(jpaParentJob);

        // Get the JPA instance of the root job
        JobJpaImpl jpaRootJob;
        if (parentJob.getRootJobId() == -1L) {
          jpaRootJob = jpaParentJob;
        } else {
          try {
            jpaRootJob = (JobJpaImpl) getJob(parentJob.getRootJobId());
          } catch (NotFoundException e) {
            logger.error("job with id {} not found in the persistence context", parentJob.getRootJobId());
            throw new ServiceRegistryException(e);
          }
        }
        job.setRootJob(jpaRootJob);
      }

      // if this job is not dispatchable, it must be handled by the host that has created it
      if (dispatchable) {
        job.setStatus(Status.QUEUED);
      } else {
        job.setProcessingHost(creatingService.getHost());
      }

      em.persist(job);
      tx.commit();
      setJobUri(job);
      return job;
    } catch (RollbackException e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void removeJob(long jobId) throws NotFoundException, ServiceRegistryException {
    if (jobId < 1)
      throw new NotFoundException("Job ID must be greater than zero (0)");

    logger.debug("Start deleting job with ID '{}'", jobId);

    EntityManager em = null;
    EntityTransaction tx = null;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();

      Job job = em.find(JobJpaImpl.class, jobId);
      if (job == null)
        throw new NotFoundException("Job with ID '" + jobId + "' not found");

      deleteChildJobs(jobId);

      tx.begin();
      em.remove(job);
      tx.commit();
      logger.debug("Job with ID '{}' deleted", jobId);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to remove job {}: {}", jobId, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  private void deleteChildJobs(long jobId) throws ServiceRegistryException {
    List<Job> childJobs = getChildJobs(jobId);
    if (childJobs.isEmpty()) {
      logger.debug("No child jobs of job '{}' found to delete.", jobId);
      return;
    }

    logger.debug("Start deleting child jobs of job '{}'", jobId);

    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      for (int i = childJobs.size() - 1; i >= 0; i--) {
        Job job = childJobs.get(i);
        Job jobToDelete = em.find(JobJpaImpl.class, job.getId());
        tx.begin();
        em.remove(jobToDelete);
        tx.commit();
        logger.debug("Job '{}' deleted", jobToDelete.getId());
      }
      logger.debug("Deleted all child jobs of job '{}'", jobId);
    } catch (Exception e) {
      logger.error("Unable to remove child jobs from {}: {}", jobId, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void removeParentlessJobs(int lifetime) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;

    Date d = DateUtils.addDays(new Date(), -lifetime);
    int count = 0;

    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      TypedQuery<JobJpaImpl> query = em.createNamedQuery("Job.withoutParent", JobJpaImpl.class);
      List<JobJpaImpl> jobs = query.getResultList();

      tx.begin();
      for (JobJpaImpl job : jobs) {
        if (job.getDateCreated().after(d))
          continue;

        // DO NOT DELETE workflow instances and operations!
        if (START_OPERATION.equals(job.getOperation()) || START_WORKFLOW.equals(job.getOperation())
                || RESUME.equals(job.getOperation()))
          continue;

        if (job.getStatus().isTerminated()) {
          try {
            removeJob(job.getId());
            logger.debug("Parentless job '{}' removed", job.getId());
            count++;
          } catch (NotFoundException e) {
            logger.debug("Parentless job '{} ' not found in database: {}", job.getId(), e);
          }
        }

      }
      tx.commit();
      if (count > 0)
        logger.info("Successfully removed {} parentless jobs", count);
      else
        logger.info("No parentless jobs found to remove", count);
    } finally {
      if (em != null)
        em.close();
    }
    return;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {
    String maxAttempts = StringUtils.trimToNull((String) properties.get(MAX_ATTEMPTS_CONFIG_KEY));
    if (maxAttempts != null) {
      try {
        maxAttemptsBeforeErrorState = Integer.parseInt(maxAttempts);
        logger.info("Set max attempts before error state to {}", maxAttempts);
      } catch (NumberFormatException e) {
        logger.warn("Can not set max attempts before error state to {}. {} must be an integer", maxAttempts,
                MAX_ATTEMPTS_CONFIG_KEY);
      }
    }

    long dispatchInterval = DEFAULT_DISPATCH_INTERVAL;
    String dispatchIntervalString = StringUtils.trimToNull((String) properties.get(OPT_DISPATCHINTERVAL));
    if (StringUtils.isNotBlank(dispatchIntervalString)) {
      try {
        dispatchInterval = Long.parseLong(dispatchIntervalString);
      } catch (Exception e) {
        logger.warn("Dispatch interval '{}' is malformed, setting to {}", dispatchIntervalString, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      }
      if (dispatchInterval == 0) {
        logger.info("Dispatching disabled");
      } else if (dispatchInterval < MIN_DISPATCH_INTERVAL) {
        logger.warn("Dispatch interval {} ms too low, adjusting to {}", dispatchInterval, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      } else {
        logger.info("Dispatch interval set to {} ms", dispatchInterval);
      }
    }

    long heartbeatInterval = DEFAULT_HEART_BEAT;
    String heartbeatIntervalString = StringUtils.trimToNull((String) properties.get(OPT_HEARTBEATINTERVAL));
    if (StringUtils.isNotBlank(heartbeatIntervalString)) {
      try {
        heartbeatInterval = Long.parseLong(heartbeatIntervalString);
      } catch (Exception e) {
        logger.warn("Heartbeat interval '{}' is malformed, setting to {}", heartbeatIntervalString, DEFAULT_HEART_BEAT);
        heartbeatInterval = DEFAULT_HEART_BEAT;
      }
      if (heartbeatInterval == 0) {
        logger.info("Heartbeat disabled");
      } else if (heartbeatInterval < 0) {
        logger.warn("Heartbeat interval {} minutes too low, adjusting to {}", heartbeatInterval, DEFAULT_HEART_BEAT);
        heartbeatInterval = DEFAULT_HEART_BEAT;
      } else {
        logger.info("Dispatch interval set to {} minutes", heartbeatInterval);
      }
    }

    String jobStatsString = StringUtils.trimToNull((String) properties.get(OPT_JOBSTATISTICS));
    if (StringUtils.isNotBlank(jobStatsString)) {
      try {
        collectJobstats = Boolean.valueOf(jobStatsString);
      } catch (Exception e) {
        logger.warn("Job statistics collection flag '{}' is malformed, setting to {}", jobStatsString,
                DEFAULT_JOB_STATISTICS);
        collectJobstats = DEFAULT_JOB_STATISTICS;
      }
    }

    // Stop the current scheduled executors so we can configure new ones
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown();
      scheduledExecutor = Executors.newScheduledThreadPool(2);
    }

    // Schedule the service heartbeat if the interval is > 0
    if (heartbeatInterval > 0) {
      logger.debug("Starting service heartbeat at a custom interval of {}s", heartbeatInterval);
      scheduledExecutor.scheduleWithFixedDelay(new JobProducerHeartbeat(), heartbeatInterval, heartbeatInterval,
              TimeUnit.SECONDS);
    }

    // Schedule the job dispatching.
    if (dispatchInterval > 0) {
      logger.debug("Starting job dispatching at a custom interval of {}s", DEFAULT_DISPATCH_INTERVAL / 1000);
      scheduledExecutor.scheduleWithFixedDelay(new JobDispatcher(), dispatchInterval, dispatchInterval,
              TimeUnit.MILLISECONDS);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJob(long)
   */
  @Override
  public Job getJob(long id) throws NotFoundException, ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      JobJpaImpl job = em.find(JobJpaImpl.class, id);
      if (job == null) {
        throw new NotFoundException("Job " + id + " not found");
      }
      // JPA's caches can be out of date if external changes (e.g. another node in the cluster) have been made to
      // this row in the database
      em.refresh(job);
      job.getArguments();
      setJobUri(job);
      return job;
    } catch (Exception e) {
      if (e instanceof NotFoundException) {
        throw (NotFoundException) e;
      } else {
        throw new ServiceRegistryException(e);
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCurrentJob()
   */
  @Override
  public Job getCurrentJob() {
    return currentJob.get();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setCurrentJob(Job)
   */
  @Override
  public void setCurrentJob(Job job) {
    currentJob.set(job);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#updateJob(org.opencastproject.job.api.Job)
   */
  @Override
  public Job updateJob(Job job) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      JobJpaImpl oldJob = (JobJpaImpl) getJob(job.getId());
      Job jpaJob = updateInternal(em, job);

      // All WorkflowService Jobs will be ignored
      if (oldJob.getStatus() != job.getStatus() && !"org.opencastproject.workflow".equals(job.getJobType())) {
        updateServiceForFailover(job);
      }

      return jpaJob;
    } catch (PersistenceException e) {
      throw new ServiceRegistryException(e);
    } catch (NotFoundException e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  protected Job setJobUri(Job job) {
    if (job instanceof JaxbJob) {
      try {
        ((JaxbJob) job).setUri(new URI(jobHost + "/services/job/" + job.getId() + ".xml"));
      } catch (URISyntaxException e) {
        logger.warn("Can not set the job URI", e);
      }
    } else {
      logger.warn("Can not set the job URI on a " + job.getClass().getName());
    }
    return job;
  }

  /**
   * Internal method to update a job, throwing unwrapped JPA exceptions.
   *
   * @param em
   *          the current entity manager
   * @param job
   *          the job to update
   * @return the updated job
   * @throws PersistenceException
   *           if there is an exception thrown while persisting the job via JPA
   * @throws IllegalArgumentException
   */
  protected Job updateInternal(EntityManager em, Job job) throws PersistenceException {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      JobJpaImpl fromDb;
      fromDb = em.find(JobJpaImpl.class, job.getId());
      if (fromDb == null) {
        throw new NoResultException();
      }
      update(fromDb, (JaxbJob) job);

      em.merge(fromDb);
      tx.commit();
      ((JaxbJob) job).setVersion(fromDb.getVersion());
      setJobUri(job);
      return job;
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Internal method to update the service registration state, throwing unwrapped JPA exceptions.
   *
   * @param em
   *          the current entity manager
   * @param registration
   *          the service registration to update
   * @return the updated service registration
   * @throws PersistenceException
   *           if there is an exception thrown while persisting the job via JPA
   * @throws IllegalArgumentException
   */
  private ServiceRegistration updateServiceState(EntityManager em, ServiceRegistrationJpaImpl registration)
          throws PersistenceException {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      ServiceRegistrationJpaImpl fromDb;
      fromDb = em.find(ServiceRegistrationJpaImpl.class, registration.getId());
      if (fromDb == null) {
        throw new NoResultException();
      }
      fromDb.setServiceState(registration.getServiceState());
      fromDb.setStateChanged(registration.getStateChanged());
      fromDb.setWarningStateTrigger(registration.getWarningStateTrigger());
      fromDb.setErrorStateTrigger(registration.getErrorStateTrigger());
      tx.commit();
      servicesStatistics.updateService(registration);
      return registration;
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Sets the queue and runtimes and other elements of a persistent job based on a job that's been modified in memory.
   * Times on both the objects must be modified, since the in-memory job must not be stale.
   *
   * @param fromDb
   *          The job from the database
   * @param job
   *          The in-memory job
   */
  private void update(JobJpaImpl fromDb, JaxbJob job) {
    Date now = new Date();
    Status status = job.getStatus();
    fromDb.setPayload(job.getPayload());
    fromDb.setStatus(job.getStatus());
    fromDb.setDispatchable(job.isDispatchable());
    fromDb.setVersion(job.getVersion());
    fromDb.setOperation(job.getOperation());
    fromDb.setArguments(job.getArguments());
    if (job.getDateCreated() == null) {
      job.setDateCreated(now);
      fromDb.setDateCreated(now);
    }
    if (job.getProcessingHost() != null) {
      ServiceRegistrationJpaImpl processingService = (ServiceRegistrationJpaImpl) getServiceRegistration(
              job.getJobType(), job.getProcessingHost());
      fromDb.setProcessorServiceRegistration(processingService);
    }
    if (Status.RUNNING.equals(status)) {
      job.setDateStarted(now);
      job.setQueueTime(now.getTime() - job.getDateCreated().getTime());
      fromDb.setDateStarted(now);
      fromDb.setQueueTime(now.getTime() - job.getDateCreated().getTime());
    } else if (Status.FAILED.equals(status)) {
      // failed jobs may not have even started properly
      fromDb.setDateCompleted(now);
      job.setDateCompleted(now);
      if (job.getDateStarted() != null) {
        job.setRunTime(now.getTime() - job.getDateStarted().getTime());
        fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
      }
    } else if (Status.FINISHED.equals(status)) {
      if (job.getDateStarted() == null) {
        // Some services (e.g. ingest) don't use job dispatching, since they start immediately and handle their own
        // lifecycle. In these cases, if the start date isn't set, use the date created as the start date
        job.setDateStarted(job.getDateCreated());
      }
      job.setDateCompleted(now);
      job.setRunTime(now.getTime() - job.getDateStarted().getTime());
      fromDb.setDateCompleted(now);
      fromDb.setRunTime(now.getTime() - job.getDateStarted().getTime());
    }
  }

  /**
   * Fetches a host registration from persistence.
   *
   * @param em
   *          an active entity manager
   * @param host
   *          the host name
   * @return the host registration, or null if none exists
   */
  protected HostRegistrationJpaImpl fetchHostRegistration(EntityManager em, String host) {
    Query query = em.createNamedQuery("HostRegistration.byHostName");
    query.setParameter("host", host);
    try {
      return (HostRegistrationJpaImpl) query.getSingleResult();
    } catch (NoResultException e) {
      logger.debug("No existing host registration for {}", host);
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerHost(java.lang.String, int)
   */
  @Override
  public void registerHost(String host, int maxJobs) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        hostRegistration = new HostRegistrationJpaImpl(host, maxJobs, true, false);
        em.persist(hostRegistration);
      } else {
        hostRegistration.setMaxJobs(maxJobs);
        hostRegistration.setOnline(true);
        em.merge(hostRegistration);
      }
      logger.info("Registering {} with a maximum load of {}", host, maxJobs);
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unregisterHost(java.lang.String)
   */
  @Override
  public void unregisterHost(String host) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl existingHostRegistration = fetchHostRegistration(em, host);
      if (existingHostRegistration == null) {
        throw new ServiceRegistryException("Host '" + host
                + "' is not currently registered, so it can not be unregistered");
      } else {
        existingHostRegistration.setOnline(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          unRegisterService(serviceRegistration.getServiceType(), serviceRegistration.getHost());
        }
        em.merge(existingHostRegistration);
      }
      logger.info("Unregistering {}", host);
      tx.commit();
      hostsStatistics.updateHost(existingHostRegistration);
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#enableHost(String)
   */
  @Override
  public void enableHost(String host) throws ServiceRegistryException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Find the existing registrations for this host and if it exists, update it
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        throw new NotFoundException("Host '" + host + "' is currently not registered, so it can not be enabled");
      } else {
        hostRegistration.setActive(true);
        em.merge(hostRegistration);
      }
      logger.info("Enabling {}", host);
      tx.commit();
      tx.begin();
      for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
        ServiceRegistrationJpaImpl registration = (ServiceRegistrationJpaImpl) serviceRegistration;
        registration.setActive(true);
        em.merge(registration);
        servicesStatistics.updateService(registration);
      }
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#disableHost(String)
   */
  @Override
  public void disableHost(String host) throws ServiceRegistryException, NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, host);
      if (hostRegistration == null) {
        throw new NotFoundException("Host '" + host + "' is not currently registered, so it can not be disabled");
      } else {
        hostRegistration.setActive(false);
        for (ServiceRegistration serviceRegistration : getServiceRegistrationsByHost(host)) {
          ServiceRegistrationJpaImpl registration = (ServiceRegistrationJpaImpl) serviceRegistration;
          registration.setActive(false);
          em.merge(registration);
          servicesStatistics.updateService(registration);
        }
        em.merge(hostRegistration);
      }
      logger.info("Disabling {}", host);
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path)
          throws ServiceRegistryException {
    return registerService(serviceType, baseUrl, path, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#registerService(java.lang.String, java.lang.String,
   *      java.lang.String, boolean)
   */
  @Override
  public ServiceRegistration registerService(String serviceType, String baseUrl, String path, boolean jobProducer)
          throws ServiceRegistryException {
    cleanRunningJobs(serviceType, baseUrl);
    return setOnlineStatus(serviceType, baseUrl, path, true, jobProducer);
  }

  protected ServiceRegistrationJpaImpl getServiceRegistration(EntityManager em, String serviceType, String host) {
    try {
      Query q = em.createNamedQuery("ServiceRegistration.getRegistration");
      q.setParameter("serviceType", serviceType);
      q.setParameter("host", host);
      return (ServiceRegistrationJpaImpl) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Sets the online status of a service registration.
   *
   * @param serviceType
   *          The job type
   * @param baseUrl
   *          the host URL
   * @param online
   *          whether the service is online or off
   * @param jobProducer
   *          whether this service produces jobs for long running operations
   * @return the service registration
   */
  protected ServiceRegistration setOnlineStatus(String serviceType, String baseUrl, String path, boolean online,
          Boolean jobProducer) throws ServiceRegistryException {
    if (isBlank(serviceType) || isBlank(baseUrl)) {
      throw new IllegalArgumentException("serviceType and baseUrl must not be blank");
    }
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl hostRegistration = fetchHostRegistration(em, baseUrl);
      if (hostRegistration == null) {
        throw new IllegalStateException(
                "A service registration can not be updated when it has no associated host registration");
      }
      ServiceRegistrationJpaImpl registration = getServiceRegistration(em, serviceType, baseUrl);
      if (registration == null) {
        if (isBlank(path)) {
          // we can not create a new registration without a path
          throw new IllegalArgumentException("path must not be blank when registering new services");
        }
        if (jobProducer == null) { // if we are not provided a value, consider it to be false
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, false);

        } else {
          registration = new ServiceRegistrationJpaImpl(hostRegistration, serviceType, path, jobProducer);
        }
        em.persist(registration);
      } else {
        if (StringUtils.isNotBlank(path))
          registration.setPath(path);
        registration.setOnline(online);
        if (jobProducer != null) { // if we are not provided a value, don't update the persistent value
          registration.setJobProducer(jobProducer);
        }
        em.merge(registration);
      }
      tx.commit();
      hostsStatistics.updateHost(hostRegistration);
      servicesStatistics.updateService(registration);
      return registration;
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#unRegisterService(java.lang.String, java.lang.String)
   */
  @Override
  public void unRegisterService(String serviceType, String baseUrl) throws ServiceRegistryException {
    logger.info("Unregistering Service " + serviceType + "@" + baseUrl);
    // TODO: create methods that accept an entity manager, so we can execute multiple queries using the same em and tx
    setOnlineStatus(serviceType, baseUrl, null, false, null);

    cleanRunningJobs(serviceType, baseUrl);
  }

  /** Find all undispatchable jobs and set them to CANCELED. */
  private void cleanUndispatchableJobs() {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query query = em.createNamedQuery("Job.undispatchable.status");
      List<Status> statuses = new ArrayList<Job.Status>();
      statuses.add(Status.INSTANTIATED);
      statuses.add(Status.RUNNING);
      query.setParameter("statuses", statuses);
      @SuppressWarnings("unchecked")
      List<JobJpaImpl> undispatchableJobs = query.getResultList();
      for (JobJpaImpl job : undispatchableJobs) {
        logger.info("Marking undispatchable job {} as canceled", job);
        job.setStatus(Status.CANCELED);
        em.merge(job);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Unable to clean undispatchable jobs! {}", e.getMessage());
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Find all running jobs on this service and set them to RESET or CANCELED.
   *
   * @param serviceType
   *          the service type
   * @param baseUrl
   *          the base url
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  private void cleanRunningJobs(String serviceType, String baseUrl) throws ServiceRegistryException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Query query = em.createNamedQuery("Job.processinghost.status");
      List<Status> statuses = new ArrayList<Job.Status>();
      statuses.add(Status.RUNNING);
      statuses.add(Status.DISPATCHING);
      query.setParameter("statuses", statuses);
      query.setParameter("host", baseUrl);
      query.setParameter("serviceType", serviceType);
      @SuppressWarnings("unchecked")
      List<JobJpaImpl> unregisteredJobs = query.getResultList();
      for (JobJpaImpl job : unregisteredJobs) {
        if (job.isDispatchable()) {
          em.refresh(job);
          // If this job has already been treated
          if (Status.CANCELED.equals(job.getStatus()) || Status.RESTART.equals(job.getStatus()))
            continue;
          if (job.getRootJob() != null && Status.PAUSED.equals(job.getRootJob().getStatus())) {
            JobJpaImpl rootJob = job.getRootJob();
            cancelAllChildren(rootJob, em);
            rootJob.setStatus(Status.RESTART);
            rootJob.setOperation(START_OPERATION);
            em.merge(rootJob);
            continue;
          }

          logger.info("Marking child jobs from job {} as canceled", job);
          cancelAllChildren(job, em);
          logger.info("Rescheduling lost job {}", job);
          job.setStatus(Status.RESTART);
          job.setProcessorServiceRegistration(null);
        } else {
          logger.info("Marking lost job {} as failed", job);
          job.setStatus(Status.FAILED);
        }
        em.merge(job);
      }
      tx.commit();
    } catch (Exception e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Go through all the children recursively to set them in {@link Status#CANCELED} status
   *
   * @param job
   *          the parent job
   * @param em
   *          the entity manager
   */
  private void cancelAllChildren(JobJpaImpl job, EntityManager em) {
    for (JobJpaImpl child : job.getChildJobs()) {
      em.refresh(child);
      if (Status.CANCELED.equals(job.getStatus()))
        continue;
      cancelAllChildren(child, em);
      child.setStatus(Status.CANCELED);
      em.merge(child);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#setMaintenanceStatus(java.lang.String, boolean)
   */
  @Override
  public void setMaintenanceStatus(String baseUrl, boolean maintenance) throws NotFoundException {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      HostRegistrationJpaImpl reg = fetchHostRegistration(em, baseUrl);
      if (reg == null) {
        throw new NotFoundException("Can not set maintenance mode on a host that has not been registered");
      }
      reg.setMaintenanceMode(maintenance);
      em.merge(reg);
      tx.commit();
      hostsStatistics.updateHost(reg);
    } catch (RollbackException e) {
      if (tx != null && tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrations()
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getServiceRegistrations(em);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public Incidents incident() {
    return incidents;
  }

  @SuppressWarnings("unchecked")
  private List<ServiceRegistration> getOnlineServiceRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getAllOnline").getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets all service registrations.
   *
   * @param em
   *          the current entity manager
   * @return the list of service registrations
   */
  @SuppressWarnings("unchecked")
  protected List<ServiceRegistration> getServiceRegistrations(EntityManager em) {
    return em.createNamedQuery("ServiceRegistration.getAll").getResultList();
  }

  /**
   * Gets all host registrations
   *
   * @return the list of host registrations
   */
  @Override
  public List<HostRegistration> getHostRegistrations() {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getHostRegistrations(em);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets all host registrations
   *
   * @param em
   *          the current entity manager
   * @return the list of host registrations
   */
  @SuppressWarnings("unchecked")
  protected List<HostRegistration> getHostRegistrations(EntityManager em) {
    return em.createNamedQuery("HostRegistration.getAll").getResultList();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getChildJobs(long)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Job> getChildJobs(long id) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Job.root.children");
      query.setParameter("id", id);
      List<Job> jobs = query.getResultList();
      if (jobs.size() == 0) {
        jobs = getChildren(em, id);
        Collections.sort(jobs, new Comparator<Job>() {
          @Override
          public int compare(Job job1, Job job2) {
            return job1.getDateCreated().compareTo(job2.getDateCreated());
          }
        });
      }
      for (Job job : jobs) {
        setJobUri(job);
      }
      return jobs;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Job> getChildren(EntityManager em, long id) throws Exception {
    Query query = em.createNamedQuery("Job.children");
    query.setParameter("id", id);
    List<Job> childJobs = query.getResultList();
    List<Job> resultJobs = new ArrayList<Job>(childJobs);
    for (Job childJob : childJobs) {
      resultJobs.addAll(getChildren(em, childJob.getId()));
    }
    return resultJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getJobs(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Job> getJobs(String type, Status status) throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      if (type == null && status == null) {
        query = em.createNamedQuery("Job.all");
      } else if (type == null) {
        query = em.createNamedQuery("Job.status");
        query.setParameter("status", status);
      } else if (status == null) {
        query = em.createNamedQuery("Job.type");
        query.setParameter("serviceType", type);
      } else {
        query = em.createNamedQuery("Job");
        query.setParameter("status", status);
        query.setParameter("serviceType", type);
      }
      List<Job> jobs = query.getResultList();
      for (Job job : jobs) {
        setJobUri(job);
      }
      return jobs;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }

  }

  /**
   * Gets jobs of all types that are in the {@value Status#QUEUED} and {@value Status#RESTART} state.
   *
   * @param em
   *          the entity manager
   * @return the list of jobs waiting for dispatch
   * @throws ServiceRegistryException
   *           if there is a problem communicating with the jobs database
   */
  @SuppressWarnings("unchecked")
  protected List<Job> getDispatchableJobs(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.dispatchable.status");
      List<Status> statuses = new ArrayList<Job.Status>();
      statuses.add(Status.QUEUED);
      statuses.add(Status.RESTART);
      query.setParameter("statuses", statuses);
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected List<Object[]> getAvgOperations(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.avgOperation");
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  @SuppressWarnings("unchecked")
  List<Object[]> getCountPerHostService(EntityManager em) throws ServiceRegistryException {
    Query query = null;
    try {
      query = em.createNamedQuery("Job.countPerHostService");
      return query.getResultList();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, Status status) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query;
      if (serviceType == null && status == null) {
        query = em.createNamedQuery("Job.count.all");
      } else if (serviceType == null) {
        query = em.createNamedQuery("Job.count.nullType");
        query.setParameter("status", status);
      } else if (status == null) {
        query = em.createNamedQuery("Job.count.nullStatus");
        query.setParameter("serviceType", serviceType);
      } else {
        query = em.createNamedQuery("Job.count");
        query.setParameter("status", status);
        query.setParameter("serviceType", serviceType);
      }
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByHost(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByHost(String serviceType, String host, Status status) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Job.countByHost");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("host", host);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#countByOperation(java.lang.String, java.lang.String,
   *      org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long countByOperation(String serviceType, String operation, Status status) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Job.countByOperation");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#count(java.lang.String, java.lang.String,
   *      java.lang.String, org.opencastproject.job.api.Job.Status)
   */
  @Override
  public long count(String serviceType, String host, String operation, Status status) throws ServiceRegistryException {
    if (StringUtils.isBlank(serviceType) || StringUtils.isBlank(host) || StringUtils.isBlank(operation)
            || status == null)
      throw new IllegalArgumentException("service type, host, operation, and status must be provided");
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Job.fullMonty");
      query.setParameter("status", status);
      query.setParameter("serviceType", serviceType);
      query.setParameter("operation", operation);
      Number countResult = (Number) query.getSingleResult();
      return countResult.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getCountOfAbnormalServices()
   */
  @Override
  public long countOfAbnormalServices() throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("ServiceRegistration.countNotNormal");
      Number count = (Number) query.getSingleResult();
      return count.longValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceStatistics()
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public List<ServiceStatistics> getServiceStatistics() throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Map<Long, JaxbServiceStatistics> statsMap = new HashMap<Long, JaxbServiceStatistics>();

      // Make sure we also include the services that have no processing history so far
      List<ServiceRegistrationJpaImpl> services = em.createNamedQuery("ServiceRegistration.getAll").getResultList();
      for (ServiceRegistrationJpaImpl s : services) {
        statsMap.put(s.getId(), new JaxbServiceStatistics(s));
      }

      Query query = em.createNamedQuery("ServiceRegistration.statistics");
      List queryResults = query.getResultList();
      for (Object result : queryResults) {
        Object[] oa = (Object[]) result;
        Number serviceRegistrationId = ((Number) oa[0]);
        if (serviceRegistrationId == null || serviceRegistrationId.longValue() == 0)
          continue;
        Status status = (Status) oa[1];
        Number count = (Number) oa[2];
        Number meanQueueTime = (Number) oa[3];
        Number meanRunTime = (Number) oa[4];

        // The statistics query returns a cartesian product, so we need to iterate over them to build up the objects
        JaxbServiceStatistics stats = statsMap.get(serviceRegistrationId.longValue());
        if (stats == null)
          continue;

        // the status will be null if there are no jobs at all associated with this service registration
        if (status != null) {
          switch (status) {
            case RUNNING:
              stats.setRunningJobs(count.intValue());
              break;
            case QUEUED:
            case DISPATCHING:
              stats.setQueuedJobs(count.intValue());
              break;
            case FINISHED:
              stats.setMeanRunTime(meanRunTime.longValue());
              stats.setMeanQueueTime(meanQueueTime.longValue());
              stats.setFinishedJobs(count.intValue());
              break;
            default:
              break;
          }
        }
      }

      List<ServiceStatistics> stats = new ArrayList<ServiceStatistics>(statsMap.values());
      Collections.sort(stats, new Comparator<ServiceStatistics>() {
        @Override
        public int compare(ServiceStatistics o1, ServiceStatistics o2) {
          ServiceRegistration reg1 = o1.getServiceRegistration();
          ServiceRegistration reg2 = o2.getServiceRegistration();
          int typeComparison = reg1.getServiceType().compareTo(reg2.getServiceType());
          return typeComparison == 0 ? reg1.getHost().compareTo(reg2.getHost()) : typeComparison;
        }
      });

      return stats;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Do not look at this, it will burn your eyes! This is due to JPA's inability to do a left outer join with join
   * conditions.
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByLoad(java.lang.String)
   */
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByLoad(String serviceType) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Map<String, Integer> loadByHost = getHostLoads(em, true);
      List<HostRegistration> hostRegistrations = getHostRegistrations();
      List<ServiceRegistration> serviceRegistrations = getServiceRegistrationsByType(serviceType);
      return getServiceRegistrationsByLoad(serviceType, serviceRegistrations, hostRegistrations, loadByHost);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets a map of hosts to the number of jobs currently loading that host
   *
   * @param em
   *          the entity manager
   * @param activeOnly
   *          if true, the map will include only hosts that are online and have non-maintenance mode services
   * @return the map of hosts to job counts
   */
  @SuppressWarnings("unchecked")
  protected Map<String, Integer> getHostLoads(EntityManager em, boolean activeOnly) {

    // Initialize the list of hosts
    List<ServiceRegistration> services = em.createNamedQuery("ServiceRegistration.getAll").getResultList();
    Map<String, Integer> loadByHost = new HashMap<String, Integer>();
    for (ServiceRegistration s : services) {
      if (!loadByHost.containsKey(s.getHost()))
        loadByHost.put(s.getHost(), 0);
    }

    // Find all jobs that are currently running on any given host
    Query q = em.createNamedQuery("ServiceRegistration.hostload");

    // Accumulate the numbers for relevant job statuses per host
    for (Object result : q.getResultList()) {
      Object[] resultArray = (Object[]) result;
      ServiceRegistrationJpaImpl service = (ServiceRegistrationJpaImpl) resultArray[0];

      // Workflow related jobs are not counting. Workflows are load balanced by the workflow service directly
      if (TYPE_WORKFLOW.equals(service.getServiceType()))
        continue;

      Job.Status status = (Status) resultArray[1];
      int count = ((Number) resultArray[2]).intValue();

      if (activeOnly && (service.isInMaintenanceMode() || !service.isOnline())) {
        continue;
      }

      // Only queued and running jobs are adding to the load, so every other status is discarded
      if (status == null || !JOB_STATUSES_INFLUENCING_LOAD_BALANCING.contains(status)) {
        count = 0;
      }

      // Add the service registration
      if (loadByHost.containsKey(service.getHost())) {
        Integer previousServiceLoad = loadByHost.get(service.getHost());
        loadByHost.put(service.getHost(), previousServiceLoad + count);
      } else {
        loadByHost.put(service.getHost(), count);
      }
    }

    return loadByHost;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByType(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByType(String serviceType) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getByType").setParameter("serviceType", serviceType)
              .getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistrationsByHost(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<ServiceRegistration> getServiceRegistrationsByHost(String host) throws ServiceRegistryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return em.createNamedQuery("ServiceRegistration.getByHost").setParameter("host", host).getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getServiceRegistration(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public ServiceRegistration getServiceRegistration(String serviceType, String host) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getServiceRegistration(em, serviceType, host);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * A custom ServiceTracker that registers all locally published servlets so clients can find the most appropriate
   * service on the network to handle new jobs.
   */
  class RestServiceTracker extends ServiceTracker {
    protected static final String FILTER = "(&(objectClass=javax.servlet.Servlet)("
            + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

    protected BundleContext bundleContext = null;

    RestServiceTracker(BundleContext bundleContext) throws InvalidSyntaxException {
      super(bundleContext, bundleContext.createFilter(FILTER), null);
      this.bundleContext = bundleContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.util.tracker.ServiceTracker#open(boolean)
     */
    @Override
    public void open(boolean trackAllServices) {
      super.open(trackAllServices);
      try {
        ServiceReference[] references = bundleContext.getAllServiceReferences(null, FILTER);
        if (references != null) {
          for (ServiceReference ref : references) {
            addingService(ref);
          }
        }
      } catch (InvalidSyntaxException e) {
        throw new IllegalStateException("The tracker filter '" + FILTER + "' has syntax errors", e);
      }
    }

    @Override
    public Object addingService(ServiceReference reference) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      String servicePath = (String) reference.getProperty(RestConstants.SERVICE_PATH_PROPERTY);
      boolean publishFlag = (Boolean) reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY);
      boolean jobProducer = (Boolean) reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY);

      // Only register services that have the "publish" flag set to "true"
      if (publishFlag) {
        try {
          registerService(serviceType, hostName, servicePath, jobProducer);
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to register job producer of type " + serviceType + " on host " + hostName);
        }
      } else {
        logger.debug("Not registering service " + serviceType + " in service registry by configuration");
      }

      return super.addingService(reference);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      boolean publishFlag = (Boolean) reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY);

      // Services that have the "publish" flag set to "true" have been registered before.
      if (publishFlag) {
        try {
          unRegisterService(serviceType, hostName);
        } catch (ServiceRegistryException e) {
          logger.warn("Unable to unregister job producer of type " + serviceType + " on host " + hostName);
        }
      } else {
        logger.trace("Service " + reference + " was never registered");
      }
      super.removedService(reference, service);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getLoad()
   */
  @Override
  public SystemLoad getLoad() throws ServiceRegistryException {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets the trusted http client.
   *
   * @param client
   *          the trusted http client
   */
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /** OSGi DI. */
  public void setIncidentService(IncidentService incidentService) {
    // Manually resolve the cyclic dependency between the incident service and the service registry
    ((OsgiIncidentService) incidentService).setServiceRegistry(this);
    this.incidents = new Incidents(this, incidentService);
  }

  /**
   * Dispatches the job to the least loaded service that will accept the job, or throws a
   * <code>ServiceUnavailableException</code> if there is no such service.
   *
   * @param em
   *          the current entity manager
   * @param job
   *          the job to dispatch
   * @param services
   *          a list of service registrations
   * @return the host that accepted the dispatched job, or <code>null</code> if no services took the job.
   * @throws ServiceRegistryException
   *           if the service registrations are unavailable
   * @throws ServiceUnavailableException
   *           if no service is available or if all available services refuse to take on more work
   * @throws UndispatchableJobException
   *           if the current job cannot be processed
   */
  protected String dispatchJob(EntityManager em, Job job, List<ServiceRegistration> services)
          throws ServiceRegistryException, ServiceUnavailableException, UndispatchableJobException {

    if (services.size() == 0) {
      logger.debug("No service is currently available to handle jobs of type '" + job.getJobType() + "'");
      throw new ServiceUnavailableException("No service of type " + job.getJobType() + " available");
    }

    // Try the service registrations, after the first one finished, we quit
    JobJpaImpl jpaJob = ((JobJpaImpl) job);
    jpaJob.setStatus(Status.DISPATCHING);

    boolean triedDispatching = false;

    for (ServiceRegistration registration : services) {
      jpaJob.setProcessorServiceRegistration((ServiceRegistrationJpaImpl) registration);

      try {
        updateInternal(em, jpaJob);
      } catch (Exception e) {
        // In theory, we should catch javax.persistence.OptimisticLockException. Unfortunately, eclipselink throws
        // org.eclipse.persistence.exceptions.OptimisticLockException. In order to avoid importing the implementation
        // specific APIs, we just catch Exception.
        logger.debug("Unable to dispatch {}.  This is likely caused by another service registry dispatching the job",
                job);
        throw new UndispatchableJobException("Job " + job.getId() + " is already being dispatched");
      }

      triedDispatching = true;

      String serviceUrl = UrlSupport
              .concat(new String[] { registration.getHost(), registration.getPath(), "dispatch" });
      HttpPost post = new HttpPost(serviceUrl);

      // Add current organization and user so they can be used during execution at the remote end
      post.addHeader(ORGANIZATION_HEADER, securityService.getOrganization().getId());
      post.addHeader(USER_HEADER, securityService.getUser().getUsername());

      try {
        String jobXml = JobParser.toXml(jpaJob);
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("job", jobXml));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
        post.setEntity(entity);
      } catch (IOException e) {
        logger.warn("Job parsing error on job {}", job, e);
        jpaJob.setStatus(Status.FAILED);
        jpaJob.setProcessorServiceRegistration(null);
        updateJob(jpaJob);
        throw new ServiceRegistryException("Can not serialize job " + jpaJob, e);
      }

      // Post the request
      HttpResponse response = null;
      int responseStatusCode;
      try {
        logger.debug("Trying to dispatch job {} of type '{}' to {}", new String[] { Long.toString(jpaJob.getId()),
                jpaJob.getJobType(), registration.getHost() });
        if (!START_WORKFLOW.equals(jpaJob.getOperation()))
          setCurrentJob(jpaJob);
        response = client.execute(post);
        responseStatusCode = response.getStatusLine().getStatusCode();
        if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
          return registration.getHost();
        } else if (responseStatusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
          logger.debug("Service {} is currently refusing to accept jobs of type {}", registration, job.getOperation());
          continue;
        } else if (responseStatusCode == HttpStatus.SC_PRECONDITION_FAILED) {
          jpaJob.setStatus(Status.FAILED);
          updateJob(jpaJob);
          logger.debug("Service {} refused to accept {}", registration, job);
          throw new UndispatchableJobException(IOUtils.toString(response.getEntity().getContent()));
        } else if (responseStatusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
          logger.debug("Service {} is not yet reachable", registration);
          continue;
        } else {
          logger.warn("Service {} failed ({}) accepting {}", new Object[] { registration, responseStatusCode, job });
          continue;
        }
      } catch (UndispatchableJobException e) {
        throw e;
      } catch (Exception e) {
        logger.warn("Unable to dispatch job {}", jpaJob.getId(), e);
      } finally {
        client.close(response);
        setCurrentJob(null);
      }
    }

    // We've tried dispatching to every online service that can handle this type of job, with no luck.
    if (triedDispatching) {
      try {
        jpaJob.setStatus(Status.QUEUED);
        jpaJob.setProcessorServiceRegistration(null);
        updateJob(jpaJob);
      } catch (Exception e) {
        logger.error("Unable to put job back into queue", e);
      }
    }

    logger.debug("Unable to dispatch {}, no service is currently ready to accept the job", job);
    throw new UndispatchableJobException("Job " + job.getId() + " is currently undispatchable");
  }

  /**
   * Update the jobs failure history and the service status with the given information. All these data are then use for
   * the jobs failover strategy. Only the terminated job (with FAILED or FINISHED status) are taken into account.
   *
   * @param job
   *          the current job that failed/succeeded
   * @throws ServiceRegistryException
   * @throws IllegalArgumentException
   */
  private void updateServiceForFailover(Job job) throws IllegalArgumentException, ServiceRegistryException {
    if (job.getStatus() != Status.FAILED && job.getStatus() != Status.FINISHED)
      return;

    JobJpaImpl jpaJob;
    try {
      jpaJob = (JobJpaImpl) getJob(job.getId());
      // Failure reason is transient
      jpaJob.setStatus(job.getStatus(), job.getFailureReason());
    } catch (NotFoundException e) {
      throw new ServiceRegistryException(e);
    }

    // At this point, the only possible states for the current service are NORMAL and WARNING,
    // the services in ERROR state will not be chosen by the dispatcher
    ServiceRegistrationJpaImpl currentService = jpaJob.getProcessorServiceRegistration();
    if (currentService == null)
      return;

    EntityManager em = emf.createEntityManager();
    try {
      em = emf.createEntityManager();

      // Job is finished with a failure
      if (jpaJob.getStatus() == FAILED && !DATA.equals(jpaJob.getFailureReason())) {

        // Services in WARNING or ERROR state triggered by current job
        List<ServiceRegistrationJpaImpl> relatedWarningOrErrorServices = getRelatedWarningErrorServices(jpaJob);

        // Before this job failed there was at least one job failed with this job signature on any service
        if (relatedWarningOrErrorServices.size() > 0) {

          for (ServiceRegistrationJpaImpl relatedService : relatedWarningOrErrorServices) {
            // Skip current service from the list
            if (currentService.equals(relatedService))
              continue;

            // Reset the WARNING job to NORMAL
            if (relatedService.getServiceState() == WARNING) {
              logger.info("State reset to NORMAL for related service {} on host {}", relatedService.getServiceType(),
                      relatedService.getHost());
              relatedService.setServiceState(NORMAL, jpaJob.getSignature());
            }

            // Reset the ERROR job to WARNING
            else if (relatedService.getServiceState() == ERROR) {
              logger.info("State reset to WARNING for related service {} on host {}", relatedService.getServiceType(),
                      relatedService.getHost());
              relatedService.setServiceState(WARNING, relatedService.getWarningStateTrigger());
            }

            updateServiceState(em, relatedService);
          }

        }

        // This is the first job with this signature failing on any service
        else {

          // Set the current service to WARNING state
          if (currentService.getServiceState() == NORMAL) {
            logger.info("State set to WARNING for current service {} on host {}", currentService.getServiceType(),
                    currentService.getHost());
            currentService.setServiceState(WARNING, jpaJob.getSignature());
            updateServiceState(em, currentService);
          }

          // The current service already is in WARNING state and max attempts is reached
          else if (getHistorySize(currentService) >= maxAttemptsBeforeErrorState) {
            logger.info("State set to ERROR for current service {} on host {}", currentService.getServiceType(),
                    currentService.getHost());
            currentService.setServiceState(ERROR, jpaJob.getSignature());
            updateServiceState(em, currentService);
          }
        }

      }

      // Job is finished without failure
      else if (jpaJob.getStatus() == Status.FINISHED) {

        // If the service was in warning state reset to normal state
        if (currentService.getServiceState() == WARNING) {
          logger.info("State reset to NORMAL for current service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          currentService.setServiceState(NORMAL);
          updateServiceState(em, currentService);
        }

        // Services in WARNING state triggered by current job
        List<ServiceRegistrationJpaImpl> relatedWarningServices = getRelatedWarningServices(jpaJob);

        // Sets all related services to error state
        for (ServiceRegistrationJpaImpl relatedService : relatedWarningServices) {
          logger.info("State set to ERROR for related service {} on host {}", currentService.getServiceType(),
                  currentService.getHost());
          relatedService.setServiceState(ERROR, jpaJob.getSignature());
          updateServiceState(em, relatedService);
        }

      }

    } finally {
      if (em != null)
        em.close();
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#sanitize(java.lang.String, java.lang.String)
   */
  @Override
  public void sanitize(String serviceType, String host) throws NotFoundException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      ServiceRegistrationJpaImpl service = getServiceRegistration(em, serviceType, host);
      if (service == null)
        throw new NotFoundException("");
      logger.info("State reset to NORMAL for service {} on host {} through santize method", service.getServiceType(),
              service.getHost());
      service.setServiceState(NORMAL);
      updateServiceState(em, service);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets the failed jobs history for the given service registration
   *
   * @param serviceRegistration
   * @return the failed jobs history size
   * @throws IllegalArgumentException
   *           if parameter is null
   * @throws ServiceRegistryException
   */
  private int getHistorySize(JaxbServiceRegistration serviceRegistration) throws IllegalArgumentException,
          ServiceRegistryException {
    if (serviceRegistration == null)
      throw new IllegalArgumentException("serviceRegistration must not be null!");

    Query query = null;
    EntityManager em = null;
    logger.debug("Try to get the number of jobs who failed on the service {}", serviceRegistration.toString());
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("Job.count.history.failed");
      query.setParameter("serviceType", serviceRegistration.getServiceType());
      query.setParameter("host", serviceRegistration.getHost());
      Number number = (Number) query.getSingleResult();
      return number.intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets the services in WARNING state triggered by this job
   *
   * @param job
   *          the given job to get the related services
   * @return a list of services triggered by the job
   * @throws IllegalArgumentException
   *           if the given job was null
   * @throws ServiceRegistryException
   *           if the there was a problem with the query
   */
  private List<ServiceRegistrationJpaImpl> getRelatedWarningServices(JobJpaImpl job) throws IllegalArgumentException,
          ServiceRegistryException {
    if (job == null)
      throw new IllegalArgumentException("job must not be null!");

    Query query = null;
    EntityManager em = null;
    logger.debug("Try to get the services in WARNING state triggered by this job {} failed", job.getSignature());
    try {
      em = emf.createEntityManager();
      // TODO: modify the query to avoid to go through the list here
      query = em.createNamedQuery("ServiceRegistration.relatedservices.warning");
      query.setParameter("serviceType", job.getJobType());

      List<ServiceRegistrationJpaImpl> jpaServices = new ArrayList<ServiceRegistrationJpaImpl>();

      @SuppressWarnings("unchecked")
      List<ServiceRegistrationJpaImpl> jobResults = query.getResultList();
      for (ServiceRegistrationJpaImpl relatedService : jobResults) {
        if (relatedService.getWarningStateTrigger() == job.getSignature()) {
          jpaServices.add(relatedService);
        }
      }
      return jpaServices;
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Gets the services in WARNING or ERROR state triggered by this job
   *
   * @param job
   *          the given job to get the related services
   * @return a list of services triggered by the job
   * @throws IllegalArgumentException
   *           if the given job was null
   * @throws ServiceRegistryException
   *           if the there was a problem with the query
   */
  private List<ServiceRegistrationJpaImpl> getRelatedWarningErrorServices(JobJpaImpl job)
          throws ServiceRegistryException {
    if (job == null)
      throw new IllegalArgumentException("job must not be null!");

    Query query = null;
    EntityManager em = null;
    logger.debug("Try to get the services in WARNING or ERROR state triggered by this job {} failed",
            job.getSignature());
    try {
      em = emf.createEntityManager();

      // TODO: modify the query to avoid to go through the list here
      query = em.createNamedQuery("ServiceRegistration.relatedservices.warning_error");
      query.setParameter("serviceType", job.getJobType());

      List<ServiceRegistrationJpaImpl> jpaServices = new ArrayList<ServiceRegistrationJpaImpl>();

      @SuppressWarnings("unchecked")
      List<ServiceRegistrationJpaImpl> serviceResults = query.getResultList();
      for (ServiceRegistrationJpaImpl relatedService : serviceResults) {
        if (relatedService.getServiceState() == WARNING
                && relatedService.getWarningStateTrigger() == job.getSignature()) {
          jpaServices.add(relatedService);
        }

        if (relatedService.getServiceState() == ERROR && relatedService.getErrorStateTrigger() == job.getSignature()) {
          jpaServices.add(relatedService);
        }
      }
      return jpaServices;
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a filtered list of service registrations, containing only those that are online, not in maintenance mode,
   * and with a specific service type that are running on a host which is not already maxed out.
   *
   * @param serviceRegistrations
   *          the complete list of service registrations
   * @param hostRegistrations
   *          the complete list of host registrations
   * @param loadByHost
   *          the map of hosts to the number of running jobs
   * @param jobType
   *          the job type for which the services registrations are filtered
   */
  protected List<ServiceRegistration> getServiceRegistrationsWithCapacity(String jobType,
          List<ServiceRegistration> serviceRegistrations, List<HostRegistration> hostRegistrations,
          final Map<String, Integer> loadByHost) {

    List<ServiceRegistration> filteredList = new ArrayList<ServiceRegistration>();

    for (ServiceRegistration service : serviceRegistrations) {

      // Skip services that are not of the requested type
      if (!jobType.equals(service.getServiceType())) {
        logger.trace("Not considering {} because it is of the wrong job type", service);
        continue;
      }

      // Skip services that are in error state
      if (service.getServiceState() == ERROR) {
        logger.trace("Not considering {} because it is in error state", service);
        continue;
      }

      // Skip services that are in maintenance mode
      if (service.isInMaintenanceMode()) {
        logger.trace("Not considering {} because it is in maintenance mode", service);
        continue;
      }

      // Skip services that are marked as offline
      if (!service.isOnline()) {
        logger.trace("Not considering {} because it is currently offline", service);
        continue;
      }

      // Determine the maximum load for this host
      Integer hostLoadMax = null;
      for (HostRegistration host : hostRegistrations) {
        if (host.getBaseUrl().equals(service.getHost())) {
          hostLoadMax = host.getMaxJobs();
          break;
        }
      }
      if (hostLoadMax == null)
        logger.warn("Unable to determine max load for host {}", service.getHost());

      // Determine the current load for this host
      Integer hostLoad = loadByHost.get(service.getHost());
      if (hostLoad == null)
        logger.warn("Unable to determine current load for host {}", service.getHost());

      boolean hasCapacity = hostLoad == null || hostLoadMax == null || hostLoad < hostLoadMax;

      // Is this host suited for processing?
      if (hasCapacity) {
        logger.debug("Adding candidate service {} for processing of jobs of type '{}'", service, jobType);
        filteredList.add(service);
      }

    }

    // Sort the list by capacity
    Collections.sort(filteredList, new LoadComparator(loadByHost));

    return filteredList;
  }

  /**
   * Returns a filtered list of service registrations, containing only those that are online, not in maintenance mode,
   * and with a specific service type, ordered by load.
   *
   * @param serviceRegistrations
   *          the complete list of service registrations
   * @param hostRegistrations
   *          the complete list of host registrations
   * @param loadByHost
   *          the map of hosts to the number of running jobs
   * @param jobType
   *          the job type for which the services registrations are filtered
   */
  protected List<ServiceRegistration> getServiceRegistrationsByLoad(String jobType,
          List<ServiceRegistration> serviceRegistrations, List<HostRegistration> hostRegistrations,
          final Map<String, Integer> loadByHost) {

    List<ServiceRegistration> filteredList = new ArrayList<ServiceRegistration>();

    logger.debug("Finding services to dispatch job of type {}", jobType);

    for (ServiceRegistration service : serviceRegistrations) {

      // Skip services that are not of the requested type
      if (!jobType.equals(service.getServiceType())) {
        logger.trace("Not considering {} because it is of the wrong job type", service);
        continue;
      }

      // Skip services that are in error state
      if (service.getServiceState() == ERROR) {
        logger.trace("Not considering {} because it is in error state", service);
        continue;
      }

      // Skip services that are in maintenance mode
      if (service.isInMaintenanceMode()) {
        logger.trace("Not considering {} because it is in maintenance mode", service);
        continue;
      }

      // Skip services that are marked as offline
      if (!service.isOnline()) {
        logger.trace("Not considering {} because it is currently offline", service);
        continue;
      }

      // We found a candidate service
      logger.debug("Adding candidate service {} for processing of job of type '{}'", service, jobType);
      filteredList.add(service);
    }

    // Sort the list by capacity
    Collections.sort(filteredList, new LoadComparator(loadByHost));

    return filteredList;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceRegistry#getMaxConcurrentJobs()
   */
  @Override
  public int getMaxConcurrentJobs() throws ServiceRegistryException {
    Query query = null;
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      query = em.createNamedQuery("HostRegistration.cores");
      return ((Number) query.getSingleResult()).intValue();
    } catch (Exception e) {
      throw new ServiceRegistryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * This dispatcher implementation will check for jobs in the QUEUED {@link org.opencastproject.job.api.Job.Status}. If
   * new jobs are found, the dispatcher will attempt to dispatch each job to the least loaded service.
   */
  class JobDispatcher implements Runnable {

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      EntityManager em = null;
      try {
        em = emf.createEntityManager();
        List<Job> jobsToDispatch = getDispatchableJobs(em);
        List<String> undispatchableJobTypes = new ArrayList<String>();

        // FIXME: the stats are not currently used and the queries are very
        // expense in database time.
        if (collectJobstats) {
          jobsStatistics.updateAvg(getAvgOperations(em));
          jobsStatistics.updateJobCount(getCountPerHostService(em));
        }

        // Make sure dispatching is happening in an ideal order
        Collections.sort(jobsToDispatch, new DispatchableComparator());

        // Initialize collections used to dispatch the current list of jobs
        Map<String, Integer> hostLoads = getHostLoads(em, true);
        List<ServiceRegistration> services = getServiceRegistrations(em);
        List<HostRegistration> hosts = getHostRegistrations(em);

        for (Job job : jobsToDispatch) {

          // Remember the job type
          String jobType = job.getJobType();

          // Skip jobs that we already know can't be dispatched
          String jobSignature = new StringBuilder(jobType).append('@').append(job.getOperation()).toString();
          if (undispatchableJobTypes.contains(jobSignature)) {
            logger.trace("Skipping dispatching of jobs {} with type '{}' for this round of dispatching", job.getId(),
                    jobType);
            continue;
          }

          // Set the job's user and organization prior to dispatching
          String creator = job.getCreator();
          String creatorOrganization = job.getOrganization();

          // Try to load the organization.
          Organization organization = null;
          try {
            organization = organizationDirectoryService.getOrganization(creatorOrganization);
            securityService.setOrganization(organization);
          } catch (NotFoundException e) {
            logger.debug("Skipping dispatching of job for non-existing organization '{}'", creatorOrganization);
            continue;
          }

          // Try to load the user
          User user = userDirectoryService.loadUser(creator);
          if (user == null) {
            logger.warn("Unable to dispatch job {}: creator '{}' is not available", job.getId(), creator);
            continue;
          }
          securityService.setUser(user);

          // Start dispatching
          try {

            List<ServiceRegistration> candidateServices = null;

            // Depending on whether this running job is trying to reach out to other services or whether this is an
            // attempt to execute the next operation in a workflow, choose either from a limited or from the full list
            // of services
            Job parentJob = null;
            try {
              parentJob = getJob(job.getParentJobId());
            } catch (NotFoundException e) {
              // That's ok
            }

            // When a job A starts a series of child jobs, then those child jobs should only be dispatched at the
            // same time if there is processing capacity available.
            boolean parentHasRunningChildren = false;
            if (parentJob != null) {
              List<Job> childJobs = getChildJobs(parentJob.getId());
              if (childJobs != null) {
                for (Job child : childJobs) {
                  if (Job.Status.RUNNING.equals(child.getStatus())) {
                    parentHasRunningChildren = true;
                    break;
                  }
                }
              }
            }

            // If this is a root job (a new workflow or a new workflow operation), then only dispatch if there is
            // capacity, i. e. the workflow service is ok dispatching the next workflow or the next workflow operation.
            if (parentJob == null || TYPE_WORKFLOW.equals(jobType) || parentHasRunningChildren) {
              logger.trace("Using available capacity only for dispatching of {} to a service of type '{}'", job,
                      jobType);
              candidateServices = getServiceRegistrationsWithCapacity(jobType, services, hosts, hostLoads);
            } else {
              logger.trace("Using full list of services for dispatching of {} to a service of type '{}'", job, jobType);
              candidateServices = getServiceRegistrationsByLoad(jobType, services, hosts, hostLoads);
            }

            // Try to dispatch the job
            String hostAcceptingJob = null;
            try {
              hostAcceptingJob = dispatchJob(em, job, candidateServices);
            } catch (ServiceUnavailableException e) {
              logger.debug("Jobs of type {} currently cannot be dispatched", job.getOperation());
              // Don't mark workflow jobs as undispatchable to not impact worklfow operations
              if (!TYPE_WORKFLOW.equals(jobType))
                undispatchableJobTypes.add(jobSignature);
              continue;
            } catch (UndispatchableJobException e) {
              logger.debug("Job {} currently cannot be dispatched", job.getId());
              continue;
            }

            logger.debug("Job {} dispatched to {}", job.getId(), hostAcceptingJob);
            if (hostLoads.containsKey(hostAcceptingJob)) {
              Integer previousServiceLoad = hostLoads.get(hostAcceptingJob);
              hostLoads.put(hostAcceptingJob, ++previousServiceLoad);
            } else {
              hostLoads.put(hostAcceptingJob, 1);
            }

          } catch (ServiceRegistryException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            logger.error("Error dispatching job " + job, cause);
          } finally {
            securityService.setUser(null);
            securityService.setOrganization(null);
          }
        }
      } catch (Throwable t) {
        logger.warn("Error dispatching jobs", t);
      } finally {
        if (em != null)
          em.close();
      }
    }
  }

  /** A periodic check on each service registration to ensure that it is still alive. */
  class JobProducerHeartbeat implements Runnable {

    /** List of service registrations that have been found unresponsive last time we checked */
    private final List<ServiceRegistration> unresponsive = new ArrayList<ServiceRegistration>();

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

      logger.debug("Checking for unresponsive services");
      List<ServiceRegistration> serviceRegistrations = getOnlineServiceRegistrations();

      for (ServiceRegistration service : serviceRegistrations) {
        hostsStatistics.updateHost(((ServiceRegistrationJpaImpl) service).getHostRegistration());
        servicesStatistics.updateService(service);
        if (!service.isJobProducer())
          continue;
        if (service.isInMaintenanceMode())
          continue;

        // We think this service is online and available. Prove it.
        String[] urlParts = new String[] { service.getHost(), service.getPath(), "dispatch" };
        String serviceUrl = UrlSupport.concat(urlParts);
        HttpHead options = new HttpHead(serviceUrl);
        HttpResponse response = null;
        try {
          try {
            response = client.execute(options);
            if (response != null) {
              switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK:
                  // this service is reachable, continue checking other services
                  logger.trace("Service " + service.toString() + " is responsive: " + response.getStatusLine());
                  if (unresponsive.remove(service)) {
                    logger.info("Service {} is still online", service);
                  } else if (!service.isOnline()) {
                    try {
                      setOnlineStatus(service.getServiceType(), service.getHost(), service.getPath(), true, true);
                      logger.info("Service {} is back online", service);
                    } catch (ServiceRegistryException e) {
                      logger.warn("Error setting online status for {}", service);
                    }
                  }
                  continue;
                default:
                  if (!service.isOnline())
                    continue;
                  logger.warn("Service {} is not working as expected: {}", service.toString(), response.getStatusLine());
              }
            } else {
              logger.warn("Service {} does not respond: {}", service.toString());
            }
          } catch (TrustedHttpClientException e) {
            if (!service.isOnline())
              continue;
            logger.warn("Unable to reach {} : {}", service, e);
          }

          // If we get here, the service did not respond as expected
          try {
            if (unresponsive.contains(service)) {
              unRegisterService(service.getServiceType(), service.getHost());
              unresponsive.remove(service);
              logger.warn("Marking {} as offline", service);
            } else {
              unresponsive.add(service);
              logger.warn("Added {} to the watch list", service);
            }
          } catch (ServiceRegistryException e) {
            logger.warn("Unable to unregister unreachable service: {} : {}", service, e);
          }
        } finally {
          client.close(response);
        }
      }

      logger.debug("Finished checking for unresponsive services");
    }

  }

  /**
   * Comparator that will sort service registrations depending on their capacity, wich is defined by the number of jobs
   * the service's host is already running. The lower that number, the bigger the capacity.
   */
  private static final class LoadComparator implements Comparator<ServiceRegistration> {

    private Map<String, Integer> loadByHost = null;

    /**
     * Creates a new comparator which is using the given map of host names and loads.
     *
     * @param loadByHost
     *          the current work load by host
     */
    public LoadComparator(Map<String, Integer> loadByHost) {
      this.loadByHost = loadByHost;
    }

    @Override
    public int compare(ServiceRegistration serviceA, ServiceRegistration serviceB) {
      String hostA = serviceA.getHost();
      String hostB = serviceB.getHost();
      return loadByHost.get(hostA) - loadByHost.get(hostB);
    }

  }

  /**
   * Comparator that will sort jobs according to their status. Those that were restarted are on top, those that are
   * queued are next.
   */
  private static final class DispatchableComparator implements Comparator<Job> {

    @Override
    public int compare(Job jobA, Job jobB) {

      // Jobs that are in "restart" mode should be handled first
      if (Job.Status.RESTART.equals(jobA.getStatus()) && !Job.Status.RESTART.equals(jobB.getStatus())) {
        return 1;
      } else if (Job.Status.RESTART.equals(jobB.getStatus()) && !Job.Status.RESTART.equals(jobA.getStatus())) {
        return -1;
      }

      // Regular jobs should be processed prior to workflow and workflow operation jobs
      if (TYPE_WORKFLOW.equals(jobA.getJobType()) && !TYPE_WORKFLOW.equals(jobB.getJobType())) {
        return -1;
      } else if (TYPE_WORKFLOW.equals(jobB.getJobType()) && !TYPE_WORKFLOW.equals(jobA.getJobType())) {
        return 1;
      }

      // undecided
      return 0;
    }

  }

}
