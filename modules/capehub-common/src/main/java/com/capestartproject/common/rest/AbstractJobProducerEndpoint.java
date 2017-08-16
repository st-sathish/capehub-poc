package com.capestartproject.common.rest;

import java.io.IOException;

import javax.ws.rs.FormParam;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.JobParser;
import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.serviceregistry.api.UndispatchableJobException;

/**
 * Base implementation for job producer REST endpoints.
 */
public abstract class AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AbstractJobProducerEndpoint.class);

  /**
   * @see org.opencastproject.job.api.JobProducer#acceptJob(org.opencastproject.job.api.Job)
   */
  @POST
  @Path("/dispatch")
  public Response dispatchJob(@FormParam("job") String jobXml) throws ServiceRegistryException {
    final JobProducer service = getService();
    if (service == null)
      throw new WebApplicationException(Status.SERVICE_UNAVAILABLE);

    final Job job;
    try {
      job = JobParser.parseJob(jobXml);
    } catch (IOException e) {
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    // See if the service is ready to accept anything
    String operation = job.getOperation();
    if (!service.isReadyToAcceptJobs(operation)) {
      logger.debug("Service {} is not ready to accept jobs with operation {}", new Object[] { service, job, operation });
      return Response.status(Status.SERVICE_UNAVAILABLE).build();
    }

    // See if the service has strong feelings about this particular job
    try {
      if (!service.isReadyToAccept(job)) {
        logger.debug("Service {} temporarily refused to accept job {}", service, job);
        return Response.status(Status.SERVICE_UNAVAILABLE).build();
      }
    } catch (UndispatchableJobException e) {
      logger.warn("Service {} permanently refused to accept job {}", service, job);
      return Response.status(Status.PRECONDITION_FAILED).build();
    }

    service.acceptJob(job);
    return Response.noContent().build();

  }

  @HEAD
  @Path("/dispatch")
  public Response checkHeartbeat() {
    return Response.ok().build();
  }

  /**
   * Returns the job producer that is backing this REST endpoint.
   *
   * @return the job producer
   */
  public abstract JobProducer getService();

  /**
   * Return the service registry.
   */
  public abstract ServiceRegistry getServiceRegistry();

}
