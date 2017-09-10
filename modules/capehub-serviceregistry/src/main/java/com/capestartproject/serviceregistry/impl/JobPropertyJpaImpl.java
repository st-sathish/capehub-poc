package com.capestartproject.serviceregistry.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * A JPA annotated version of the JaxbJobContext.
 *
 */
@Entity(name = "JobContext")
@Table(name = "ch_job_context")
public class JobPropertyJpaImpl {

  @Id
  @Column(name = "id")
  protected JobJpaImpl rootJob;

  @Id
  @Column(name = "name", length = 255)
  protected String name;

  @Lob
  @Column(name = "value", length = 65535)
  protected String value;

  /**
   * Default constructor needed by JPA
   */
  public JobPropertyJpaImpl() {
  }

  /**
   * Creates a new job context property.
   *
   * @param job
   *          the root job
   * @param name
   *          the property name
   * @param value
   *          the property value
   */
  public JobPropertyJpaImpl(JobJpaImpl job, String name, String value) {
    this.rootJob = job;
    this.name = name;
    this.value = value;
  }

  /**
   * @return the job
   */
  public JobJpaImpl getJob() {
    return rootJob;
  }

  /**
   * @param job
   *          the job to set
   */
  public void setJob(JobJpaImpl job) {
    this.rootJob = job;
  }

  /**
   * @return the key
   */
  public String getName() {
    return name;
  }

  /**
   * @param name
   *          the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * @param value
   *          the value to set
   */
  public void setValue(String value) {
    this.value = value;
  }

}
