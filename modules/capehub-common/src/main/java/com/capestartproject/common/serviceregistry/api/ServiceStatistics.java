package com.capestartproject.common.serviceregistry.api;

/**
 * Provides statistics for a service registration
 */
public interface ServiceStatistics {
  /** The service for which these statistics apply **/
  ServiceRegistration getServiceRegistration();

  /** The number of milliseconds a job takes, on average, to run **/
  long getMeanRunTime();

  /** The number of milliseconds a job sits in a queue, on average **/
  long getMeanQueueTime();

  /** The number of jobs that this service has successfully finished**/
  int getFinishedJobs();

  /** The number of job that this service is currently running **/
  int getRunningJobs();

  /** The number of job that are currently waiting to be run by this service **/
  int getQueuedJobs();
}
