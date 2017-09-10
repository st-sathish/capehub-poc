package com.capestartproject.serviceregistry.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.JaxbHostRegistration;

/**
 * A record of a host providing Capehub services.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "host", namespace = "http://serviceregistry.capestartproject.com")
@XmlRootElement(name = "host", namespace = "http://serviceregistry.capestartproject.com")
@Entity(name = "HostRegistration")
@Table(name = "ch_host_registration", uniqueConstraints = @UniqueConstraint(columnNames = "host"))
@NamedQueries({
        @NamedQuery(name = "HostRegistration.cores", query = "SELECT sum(hr.maxJobs) FROM HostRegistration hr where hr.active = true"),
        @NamedQuery(name = "HostRegistration.byHostName", query = "SELECT hr from HostRegistration hr where hr.baseUrl = :host"),
        @NamedQuery(name = "HostRegistration.getAll", query = "SELECT hr FROM HostRegistration hr where hr.active = true") })
public class HostRegistrationJpaImpl extends JaxbHostRegistration {

  /** The primary key identifying this host */
  private Long id;

  /**
   * Creates a new host registration which is online
   */
  public HostRegistrationJpaImpl() {
    super();
  }

  public HostRegistrationJpaImpl(String baseUrl, int maxJobs, boolean online, boolean maintenance) {
    super(baseUrl, maxJobs, online, maintenance);
  }

  @Id
  @Column(name = "id")
  @GeneratedValue
  public Long getId() {
    return id;
  }

  @Override
  @Column(name = "host", nullable = false, length = 255)
  @XmlElement(name = "base_url")
  public String getBaseUrl() {
    return super.getBaseUrl();
  }

  @Override
  @Column(name = "max_jobs", nullable = false)
  @XmlElement(name = "max_jobs")
  public int getMaxJobs() {
    return super.getMaxJobs();
  }

  @Override
  @Column(name = "online", nullable = false)
  @XmlElement(name = "online")
  public boolean isOnline() {
    return super.isOnline();
  }

  @Override
  @Column(name = "active", nullable = false)
  @XmlElement(name = "active")
  public boolean isActive() {
    return super.isActive();
  }

  @Column(name = "maintenance", nullable = false)
  @XmlElement(name = "maintenance")
  @Override
  public boolean isMaintenanceMode() {
    return super.isMaintenanceMode();
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

}
