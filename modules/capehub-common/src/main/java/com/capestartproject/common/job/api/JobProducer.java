package com.capestartproject.common.job.api;

import com.capestartproject.common.job.api.Job.Status;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.UndispatchableJobException;

/**
 * A service that creates jobs for long-running operations.
 */
public interface JobProducer {

  /**
   * The type of jobs that this producer creates.
   *
   * @return the job type
   */
  String getJobType();

  /**
   * Get the number of jobs in a current status on all nodes.
   *
   * @return Number of jobs in this state
   * @throws ServiceRegistryException
   *           if an error occurs while communicating with the backing data source
   */
  long countJobs(Status status) throws ServiceRegistryException;

  /**
   * Asks the job producer to handle the given job using the provided operation and list of arguments. The
   * implementation of this method <b>must</b> be asynchronous if the processing takes more than a few seconds.
   *
   * @param job
   *          the job being dispatched
   * @throws ServiceRegistryException
   *           if the producer was unable to start work as requested
   */
  void acceptJob(Job job) throws ServiceRegistryException;

  /**
   * Whether new jobs can be accepted in general.
   *
   * @param operation
   *          operation
   * @throws ServiceRegistryException
   *           if the producer was unable to start work as requested
   * @return whether the service is ready to accept jobs
   */
  boolean isReadyToAcceptJobs(String operation) throws ServiceRegistryException;

  /**
   * Whether the job can be accepted.
   *
   * @param job
   *          the job being dispatched
   * @throws ServiceRegistryException
   *           if the producer was unable to start work as requested
   * @throws UndispatchableJobException
   *           if the job will never be accepted because it is unacceptable
   * @return whether the service is ready to accept the job
   */
  boolean isReadyToAccept(Job job) throws ServiceRegistryException, UndispatchableJobException;

}
