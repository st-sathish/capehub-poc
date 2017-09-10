package com.capestartproject.serviceregistry.impl;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.serviceregistry.api.JaxbServiceRegistration;
import com.capestartproject.common.serviceregistry.api.ServiceState;

/**
 * A record of a service that creates and manages receipts.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "service", namespace = "http://serviceregistry.capestartproject.com")
@XmlRootElement(name = "service", namespace = "http://serviceregistry.capestartproject.com")
@Entity(name = "ServiceRegistration")
@Access(AccessType.PROPERTY)
@Table(name = "ch_service_registration", uniqueConstraints = @UniqueConstraint(columnNames = { "host_registration",
        "service_type" }))
@NamedQueries({
        @NamedQuery(name = "ServiceRegistration.statistics", query = "SELECT job.processorServiceRegistrationId as serviceRegistration, job.status, "
                + "count(job.status) as numJobs, "
                + "avg(job.queueTime) as meanQueue, "
                + "avg(job.runTime) as meanRun FROM Job job group by job.processorServiceRegistrationId, job.status"),
        @NamedQuery(name = "ServiceRegistration.hostload", query = "SELECT job.processorServiceRegistration as serviceRegistration, job.status, count(job.status) as numJobs "
                + "FROM Job job "
                + "WHERE job.processorServiceRegistration.online=true and job.processorServiceRegistration.active=true and job.processorServiceRegistration.hostRegistration.maintenanceMode=false "
                + "GROUP BY job.processorServiceRegistration, job.status"),
        @NamedQuery(name = "ServiceRegistration.getRegistration", query = "SELECT r from ServiceRegistration r "
                + "where r.hostRegistration.baseUrl = :host and r.serviceType = :serviceType"),
        @NamedQuery(name = "ServiceRegistration.getAll", query = "SELECT rh FROM ServiceRegistration rh WHERE rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getAllOnline", query = "SELECT rh FROM ServiceRegistration rh WHERE rh.hostRegistration.online=true AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getByHost", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.hostRegistration.baseUrl=:host AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.getByType", query = "SELECT rh FROM ServiceRegistration rh "
                + "where rh.serviceType=:serviceType AND rh.hostRegistration.active = true"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning_error", query = "SELECT rh FROM ServiceRegistration rh "
				+ "WHERE rh.serviceType = :serviceType AND (rh.serviceState = com.capestartproject.common.serviceregistry.api.ServiceState.WARNING OR "
				+ "rh.serviceState = com.capestartproject.common.serviceregistry.api.ServiceState.ERROR)"),
        @NamedQuery(name = "ServiceRegistration.relatedservices.warning", query = "SELECT rh FROM ServiceRegistration rh "
				+ "WHERE rh.serviceType = :serviceType AND rh.serviceState = com.capestartproject.common.serviceregistry.api.ServiceState.WARNING"),
        @NamedQuery(name = "ServiceRegistration.countNotNormal", query = "SELECT count(rh) FROM ServiceRegistration rh "
				+ "WHERE rh.serviceState <> com.capestartproject.common.serviceregistry.api.ServiceState.NORMAL AND rh.hostRegistration.active = true") })
public class ServiceRegistrationJpaImpl extends JaxbServiceRegistration {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrationJpaImpl.class);

  /** The primary key */
  private Long id;

  /** The host that provides this service */
  private HostRegistrationJpaImpl hostRegistration;

  /**
   * Creates a new service registration which is online
   */
  public ServiceRegistrationJpaImpl() {
    super();
  }

  /**
   * Creates a new service registration which is online
   *
   * @param hostRegistration
   *          the host registration
   * @param serviceType
   *          the type of job this service handles
   * @param path
   *          the URL path on this host to the service endpoint
   */
  public ServiceRegistrationJpaImpl(HostRegistrationJpaImpl hostRegistration, String serviceType, String path) {
    super(serviceType, hostRegistration.getBaseUrl(), path);
    this.hostRegistration = hostRegistration;
  }

  /**
   * Creates a new service registration which is online and not in maintenance mode.
   *
   * @param processingHost
   *          the host
   * @param serviceId
   *          the job type
   * @param jobProducer
   */
  public ServiceRegistrationJpaImpl(HostRegistrationJpaImpl hostRegistration, String serviceType, String path,
          boolean jobProducer) {
    super(serviceType, hostRegistration.getBaseUrl(), path, jobProducer);
    this.hostRegistration = hostRegistration;
  }

  /**
   * Gets the primary key for this service registration.
   *
   * @return the primary key
   */
  @Id
  @Column(name = "id")
  @GeneratedValue
  public Long getId() {
    return id;
  }

  @Column(name = "online_from")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement
  @Override
  public Date getOnlineFrom() {
    return super.getOnlineFrom();
  }

  /**
   * Sets the primary key identifier.
   *
   * @param id
   *          the identifier
   */
  public void setId(Long id) {
    this.id = id;
  }

  /** The length was chosen this short because MySQL complains when trying to create an index larger than this */
  @Column(name = "service_type", nullable = false, length = 255)
  @XmlElement(name = "type")
  @Override
  public String getServiceType() {
    return super.getServiceType();
  }

  @Lob
  @Column(name = "path", nullable = false, length = 255)
  @XmlElement(name = "path")
  @Override
  public String getPath() {
    return super.getPath();
  }

  @Column(name = "service_state")
  @XmlElement(name = "service_state")
  @Override
  public ServiceState getServiceState() {
    return super.getServiceState();
  }

  @Column(name = "state_changed")
  @Temporal(TemporalType.TIMESTAMP)
  @XmlElement(name = "state_changed")
  @Override
  public Date getStateChanged() {
    return super.getStateChanged();
  }

  @Column(name = "warning_state_trigger")
  @XmlElement(name = "warning_state_trigger")
  @Override
  public int getWarningStateTrigger() {
    return warningStateTrigger;
  }

  public void setWarningStateTrigger(int jobSignature) {
    this.warningStateTrigger = jobSignature;
  }

  @Column(name = "error_state_trigger")
  @XmlElement(name = "error_state_trigger")
  @Override
  public int getErrorStateTrigger() {
    return errorStateTrigger;
  }

  public void setErrorStateTrigger(int jobSignature) {
    this.errorStateTrigger = jobSignature;
  }

  @Column(name = "active", nullable = false)
  @XmlElement(name = "active")
  @Override
  public boolean isActive() {
    return super.isActive();
  }

  @Column(name = "online", nullable = false)
  @XmlElement(name = "online")
  @Override
  public boolean isOnline() {
    return super.isOnline();
  }

  @Column(name = "job_producer", nullable = false)
  @XmlElement(name = "jobproducer")
  @Override
  public boolean isJobProducer() {
    return super.isJobProducer();
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.serviceregistry.api.ServiceRegistration#isInMaintenanceMode()
	 */
  @Transient
  @Override
  public boolean isInMaintenanceMode() {
    return super.maintenanceMode;
  }

  /**
   * Gets the associated {@link HostRegistrationJpaImpl}
   *
   * @return the host registration
   */
  @ManyToOne
  @JoinColumn(name = "host_registration")
  public HostRegistrationJpaImpl getHostRegistration() {
    return hostRegistration;
  }

  /**
   * @param hostRegistration
   *          the hostRegistration to set
   */
  public void setHostRegistration(HostRegistrationJpaImpl hostRegistration) {
    this.hostRegistration = hostRegistration;
  }

  @PostLoad
  public void postLoad() {
    if (hostRegistration == null) {
      logger.warn("host registration is null");
    } else {
      super.host = hostRegistration.getBaseUrl();
      super.maintenanceMode = hostRegistration.isMaintenanceMode();
      if (!hostRegistration.isOnline())
        super.online = false;
      if (!hostRegistration.isActive())
        super.active = false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return serviceType + "@" + host;
  }

}
