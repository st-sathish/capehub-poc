package com.capestartproject.common.serviceregistry.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Statistics for a service registration.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "statistic", namespace = "http://serviceregistry.capestartproject.com")
@XmlRootElement(name = "statistic", namespace = "http://serviceregistry.capestartproject.com")
public class JaxbServiceStatistics implements ServiceStatistics {

  /** The service registration **/
  @XmlElement
  protected JaxbServiceRegistration serviceRegistration;

  /** The mean run time for jobs **/
  @XmlAttribute(name = "meanruntime")
  protected long meanRunTime;

  /** The mean queue time for jobs **/
  @XmlAttribute(name = "meanqueuetime")
  protected long meanQueueTime;

  /** The number of finished jobs **/
  @XmlAttribute(name = "finished")
  protected int finishedJobs;

  /** The number of currently running jobs **/
  @XmlAttribute(name = "running")
  protected int runningJobs;

  /** The number of currently queued jobs **/
  @XmlAttribute(name = "queued")
  protected int queuedJobs;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbServiceStatistics() {
  }

  /**
   * Constructs a new service statistics instance without statistics.
   *
   * @param serviceRegistration
   *          the service registration
   */
  public JaxbServiceStatistics(JaxbServiceRegistration serviceRegistration) {
    super();
    this.serviceRegistration = serviceRegistration;
  }

  /**
   * Constructs a new service statistics instance with statistics.
   *
   * @param serviceRegistration
   *          the service registration
   * @param meanRunTime
   * @param meanQueueTime
   * @param runningJobs
   * @param queuedJobs
   */
  public JaxbServiceStatistics(JaxbServiceRegistration serviceRegistration, long meanRunTime, long meanQueueTime,
          int runningJobs, int queuedJobs, int finishedJobs) {
    this(serviceRegistration);
    this.meanRunTime = meanRunTime;
    this.meanQueueTime = meanQueueTime;
    this.runningJobs = runningJobs;
    this.finishedJobs = finishedJobs;
    this.queuedJobs = queuedJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getMeanQueueTime()
   */
  @Override
  public long getMeanQueueTime() {
    return meanQueueTime;
  }

  /**
   * Sets the mean queue time.
   *
   * @param meanQueueTime
   *          the mean queue time
   */
  public void setMeanQueueTime(long meanQueueTime) {
    this.meanQueueTime = meanQueueTime;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getMeanRunTime()
   */
  @Override
  public long getMeanRunTime() {
    return meanRunTime;
  }

  /**
   * Sets the mean run time.
   *
   * @param meanRunTime
   *          the mean run time.
   */
  public void setMeanRunTime(long meanRunTime) {
    this.meanRunTime = meanRunTime;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getFinishedJobs()
   */
  @Override
  public int getFinishedJobs() {
    return finishedJobs;
  }

  /**
   * Sets the number of finished jobs
   *
   * @param finishedJobs
   *          the number of finished jobs
   */
  public void setFinishedJobs(int finishedJobs) {
    this.finishedJobs = finishedJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getQueuedJobs()
   */
  @Override
  public int getQueuedJobs() {
    return queuedJobs;
  }

  /**
   * Sets the number of queued jobs
   *
   * @param queuedJobs
   *          the number of queued jobs
   */
  public void setQueuedJobs(int queuedJobs) {
    this.queuedJobs = queuedJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getRunningJobs()
   */
  @Override
  public int getRunningJobs() {
    return runningJobs;
  }

  /**
   * Sets the number of running jobs
   *
   * @param runningJobs
   *          the number of running jobs
   */
  public void setRunningJobs(int runningJobs) {
    this.runningJobs = runningJobs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.serviceregistry.api.ServiceStatistics#getServiceRegistration()
   */
  @Override
  public ServiceRegistration getServiceRegistration() {
    return serviceRegistration;
  }

}
