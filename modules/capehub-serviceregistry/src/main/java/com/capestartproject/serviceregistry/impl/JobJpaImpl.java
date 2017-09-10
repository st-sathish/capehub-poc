package com.capestartproject.serviceregistry.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.JaxbJob;
import com.capestartproject.common.job.api.JaxbJobContext;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.User;

/** A long running, asynchronously executed job. This concrete implementations adds JPA annotations to {@link JaxbJob}. */
@Entity(name = "Job")
@Access(AccessType.PROPERTY)
@Table(name = "ch_job")
@NamedQueries({
        // Job queries
        @NamedQuery(name = "Job", query = "SELECT j FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType "
                + "order by j.dateCreated"),
        @NamedQuery(name = "Job.type", query = "SELECT j FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType order by j.dateCreated"),
        @NamedQuery(name = "Job.status", query = "SELECT j FROM Job j "
                + "where j.status = :status order by j.dateCreated"),
        @NamedQuery(name = "Job.all", query = "SELECT j FROM Job j order by j.dateCreated"),
        @NamedQuery(name = "Job.dispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = true and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.undispatchable.status", query = "SELECT j FROM Job j where j.dispatchable = false and "
                + "j.status in :statuses order by j.dateCreated"),
        @NamedQuery(name = "Job.processinghost.status", query = "SELECT j FROM Job j "
                + "where j.status in :statuses and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.processorServiceRegistration.hostRegistration.baseUrl = :host order by j.dateCreated"),
        @NamedQuery(name = "Job.root.children", query = "SELECT j FROM Job j WHERE j.rootJob.id = :id ORDER BY j.dateCreated"),
        @NamedQuery(name = "Job.children", query = "SELECT j FROM Job j WHERE j.parentJob.id = :id ORDER BY j.dateCreated"),
        @NamedQuery(name = "Job.withoutParent", query = "SELECT j FROM Job j WHERE j.parentJob IS NULL"),
        @NamedQuery(name = "Job.avgOperation", query = "SELECT j.operation, AVG(j.runTime), AVG(j.queueTime) FROM Job j GROUP BY j.operation"),

        // Job count queries
        @NamedQuery(name = "Job.count", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.count.all", query = "SELECT COUNT(j) FROM Job j"),
        @NamedQuery(name = "Job.count.nullType", query = "SELECT COUNT(j) FROM Job j " + "where j.status = :status"),
        @NamedQuery(name = "Job.count.nullStatus", query = "SELECT COUNT(j) FROM Job j "
                + "where j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.countByHost", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host"),
        @NamedQuery(name = "Job.countByOperation", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.operation = :operation and "
                + "j.creatorServiceRegistration.serviceType = :serviceType"),
        @NamedQuery(name = "Job.fullMonty", query = "SELECT COUNT(j) FROM Job j "
                + "where j.status = :status and j.operation = :operation "
                + "and j.processorServiceRegistration is not null and "
                + "j.processorServiceRegistration.serviceType = :serviceType and "
                + "j.creatorServiceRegistration.hostRegistration.baseUrl = :host"),
        @NamedQuery(name = "Job.count.history.failed", query = "SELECT COUNT(j) FROM Job j "
				+ "WHERE j.status = com.capestartproject.common.job.api.Job$Status.FAILED AND j.processorServiceRegistration IS NOT NULL "
                + "AND j.processorServiceRegistration.serviceType = :serviceType AND j.processorServiceRegistration.hostRegistration.baseUrl = :host "
                + "AND j.dateCompleted >= j.processorServiceRegistration.stateChanged"),
        @NamedQuery(name = "Job.countPerHostService", query = "SELECT h.baseUrl, s.serviceType, j.status, count(j) "
                + "FROM Job j, ServiceRegistration s, HostRegistration h "
                + "WHERE ((j.processorServiceRegistration IS NOT NULL AND j.processorServiceRegistration = s) "
                + "OR (j.creatorServiceRegistration IS NOT NULL AND j.creatorServiceRegistration = s)) "
                + "AND s.hostRegistration = h GROUP BY h.baseUrl, s.serviceType, j.status") })
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "job", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "job", namespace = "http://job.capestartproject.com")
public class JobJpaImpl extends JaxbJob {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JobJpaImpl.class);

  /** The service that produced this job */
  protected ServiceRegistrationJpaImpl creatorServiceRegistration;

  /** The service that is processing, or processed, this job */
  protected ServiceRegistrationJpaImpl processorServiceRegistration;

  protected long processorServiceRegistrationId;

  protected List<JobPropertyJpaImpl> properties;

  /** The job context, to be created after loading by JPA */
  protected JaxbJobContext context = null;

  protected JobJpaImpl rootJob = null;

  protected JobJpaImpl parentJob = null;

  protected List<JobJpaImpl> childJobs = null;

  @OneToMany(mappedBy = "warningStateTrigger")
  private List<ServiceRegistrationJpaImpl> servicesRegistration;

  /** Default constructor needed by jaxb and jpa */
  public JobJpaImpl() {
    super();
  }

  public JobJpaImpl(Job job) {
    super(job);
  }

  /** Constructor with everything needed for a newly instantiated job. */
  public JobJpaImpl(User user, Organization organization, ServiceRegistrationJpaImpl creatorServiceRegistration,
          String operation, List<String> arguments, String payload, boolean dispatchable) {
    this();
    this.creator = user.getUsername();
    this.organization = organization.getId();
    this.operation = operation;
    this.context = new JaxbJobContext();
    this.childJobs = new ArrayList<JobJpaImpl>();
    if (arguments != null) {
      this.arguments = new ArrayList<String>(arguments);
    }
    setPayload(payload);
    setDateCreated(new Date());
    setCreatedHost(creatorServiceRegistration.getHost());
    setJobType(creatorServiceRegistration.getServiceType());
    setDispatchable(dispatchable);
    setStatus(Status.INSTANTIATED);
    this.creatorServiceRegistration = creatorServiceRegistration;
  }

  public JobJpaImpl(User user, Organization organization, ServiceRegistrationJpaImpl creatorServiceRegistration,
          String operation, List<String> arguments, String payload, boolean dispatchable, JobJpaImpl rootJob,
          JobJpaImpl parentJob) {
    this(user, organization, creatorServiceRegistration, operation, arguments, payload, dispatchable);
    super.setRootJobId(rootJob.getId());
    super.setParentJobId(parentJob.getId());
    this.rootJob = rootJob;
    this.parentJob = parentJob;
  }

  @Id
  @GeneratedValue
  @Column(name = "id")
  @XmlAttribute
  @Override
  public long getId() {
    return id;
  }

  @Override
  @Lob
  @Column(name = "creator", nullable = false, length = 65535)
  @XmlElement(name = "creator")
  public String getCreator() {
    return creator;
  }

  @Override
  @Lob
  @Column(name = "organization", nullable = false, length = 128)
  @XmlElement(name = "organization")
  public String getOrganization() {
    return organization;
  }

  @Transient
  @XmlElement(name = "url")
  @Override
  public URI getUri() {
    return super.getUri();
  }

  @OneToMany(mappedBy = "parentJob", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REFRESH,
          CascadeType.MERGE })
  public List<JobJpaImpl> getChildJobs() {
    return childJobs;
  }

  public void setChildJobs(List<JobJpaImpl> jobs) {
    this.childJobs = jobs;
  }

  @Column(name = "instance_version")
  @Version
  @XmlAttribute
  @Override
  public long getVersion() {
    return super.getVersion();
  }

  @Column(name = "status")
  @XmlAttribute
  @Override
  public Status getStatus() {
    return status;
  }

  @Transient
  @XmlTransient
  @Override
  public FailureReason getFailureReason() {
    return failureReason;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.Job#getJobType()
   */
  @Transient
  @XmlAttribute(name = "type")
  @Override
  public String getJobType() {
    return jobType;
  }

  @Lob
  @Column(name = "operation", length = 65535)
  @XmlAttribute
  @Override
  public String getOperation() {
    return operation;
  }

  @Lob
  @Column(name = "argument", length = 2147483647)
  @OrderColumn(name = "argument_index")
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "mh_job_argument", joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
  @XmlElement(name = "arg")
  @XmlElementWrapper(name = "args")
  @Override
  public List<String> getArguments() {
    return arguments;
  }

  @Transient
  @XmlElement
  @Override
  public String getProcessingHost() {
    return processingHost;
  }

  @Transient
  @XmlElement
  @Override
  public String getCreatedHost() {
    return createdHost;
  }

  @Column(name = "date_completed")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  @Column(name = "date_created")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateCreated() {
    return dateCreated;
  }

  @Column(name = "date_started")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /** @return the queueTime */
  @Column(name = "queue_time")
  @XmlElement
  @Override
  public Long getQueueTime() {
    return queueTime;
  }

  /** @return the runTime */
  @Column(name = "run_time")
  @XmlElement
  @Override
  public Long getRunTime() {
    return runTime;
  }

  @Lob
  @Column(name = "payload", length = 16777215)
  @XmlElement
  @Override
  public String getPayload() {
    return super.getPayload();
  }

  @Override
  public void setPayload(String payload) {
    super.setPayload(payload);
  }

  @Column(name = "dispatchable")
  @XmlAttribute
  @Override
  public boolean isDispatchable() {
    return super.dispatchable;
  }

  @Override
  public void setDispatchable(boolean dispatchable) {
    super.setDispatchable(dispatchable);
  }

  /** @return the serviceRegistration where this job was created */
  @ManyToOne
  @JoinColumn(name = "creator_service")
  public ServiceRegistrationJpaImpl getCreatorServiceRegistration() {
    return creatorServiceRegistration;
  }

  /**
   * @param serviceRegistration
   *          the serviceRegistration to set
   */
  public void setCreatorServiceRegistration(ServiceRegistrationJpaImpl serviceRegistration) {
    this.creatorServiceRegistration = serviceRegistration;
    if (creatorServiceRegistration == null) {
      super.setCreatedHost(null);
    } else {
      super.setCreatedHost(creatorServiceRegistration.getHost());
    }
  }

  /** @return the processorServiceRegistration */
  @ManyToOne
  @JoinColumn(name = "processor_service")
  public ServiceRegistrationJpaImpl getProcessorServiceRegistration() {
    return processorServiceRegistration;
  }

  /**
   * @param processorServiceRegistration
   *          the processorServiceRegistration to set
   */
  public void setProcessorServiceRegistration(ServiceRegistrationJpaImpl processorServiceRegistration) {
    this.processorServiceRegistration = processorServiceRegistration;
    if (processorServiceRegistration == null) {
      super.setProcessingHost(null);
    } else {
      super.setProcessingHost(processorServiceRegistration.getHost());
    }
  }

  /**
   * Returns the identifier of the processor service
   * <p>
   * Use this method instead of {@link #getProcessorServiceRegistration()} when you only need/want the identifier of the
   * service and not the service registration object.
   *
   * @return the processor service identifier
   */
  @Column(name = "processor_service", insertable = false, updatable = false)
  public long getProcessorServiceRegistrationId() {
    return processorServiceRegistrationId;
  }

  /**
   * This method MUST NOT be used - it's for JPA only!
   */
  protected void setProcessorServiceRegistrationId(long id) {
    this.processorServiceRegistrationId = id;
  }

  @PreUpdate
  public void preUpdate() {
    if (properties != null)
      properties.clear();
    else
      properties = new ArrayList<JobPropertyJpaImpl>();
    for (Map.Entry<String, String> entry : context.getProperties().entrySet()) {
      properties.add(new JobPropertyJpaImpl(rootJob, entry.getKey(), entry.getValue()));
    }
  }

  @PostLoad
  public void postLoad() {
    if (payload != null) {
      payload.getBytes(); // force the clob to load
    }
    if (creatorServiceRegistration == null) {
      logger.warn("creator service registration for job '{}' is null", id);
    } else {
      super.createdHost = creatorServiceRegistration.getHost();
      super.jobType = creatorServiceRegistration.getServiceType();
    }
    if (processorServiceRegistration == null) {
      logger.debug("processor service registration for job '{}' is null", id);
    } else {
      super.processingHost = processorServiceRegistration.getHost();
      super.jobType = processorServiceRegistration.getServiceType();
    }
    context = new JaxbJobContext();
    if (rootJob != null) {
      context.setId(rootJob.getId());
    }
    if (properties != null) {
      for (JobPropertyJpaImpl property : properties) {
        context.getProperties().put(property.getName(), property.getValue());
      }
    }
  }

  @Transient
  @Override
  public JaxbJobContext getContext() {
    return context;
  }

  /** @return the properties */
  @Transient
  // TODO: remove to re-enable job context properties
  public List<JobPropertyJpaImpl> getProperties() {
    return properties;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties(List<JobPropertyJpaImpl> properties) {
    this.properties = properties;
  }

  /** @return the parentJob */
  @JoinColumn(name = "parent", referencedColumnName = "id", nullable = true)
  public JobJpaImpl getParentJob() {
    return parentJob;
  }

  /**
   * @param parentJob
   *          the parentJob to set
   */
  public void setParentJob(JobJpaImpl parentJob) {
    if (parentJob == null)
      return;

    super.setParentJobId(parentJob.getId());
    this.parentJob = parentJob;
  }

  /** @return the rootJob */
  /**
   * @return the rootJob
   */
  @OneToOne(fetch = FetchType.LAZY, targetEntity = JobJpaImpl.class, optional = true)
  @JoinColumn(name = "root", referencedColumnName = "id", nullable = true)
  public JobJpaImpl getRootJob() {
    return rootJob;
  }

  /**
   * @param rootJob
   *          the rootJob to set
   */
  public void setRootJob(JobJpaImpl rootJob) {
    if (rootJob == null)
      return;

    super.setRootJobId(rootJob.getId());
    this.rootJob = rootJob;
  }

  /** @return the servicesRegistration */
  public List<ServiceRegistrationJpaImpl> getServicesRegistration() {
    return servicesRegistration;
  }

  /**
   * @param servicesRegistration
   *          the servicesRegistration to set
   */
  public void setServicesRegistration(List<ServiceRegistrationJpaImpl> servicesRegistration) {
    this.servicesRegistration = servicesRegistration;
  }

}
