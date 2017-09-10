package com.capestartproject.common.rest;

import static java.lang.String.format;

import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;

/**
 * Refined implementation of
 * {@link com.capestartproject.common.rest.AbstractJobProducerEndpoint} suitable
 * for use in an OSGi environment.
 * <p/>
 * OSGi dependency injection methods are provided to reduce the amount of
 * boilerplate code needed per service implementation.
 * <p/>
 * Declare as type variable the
 * {@linkplain AbstractJobProducerEndpoint#getService() job producing service}
 * on which the endpoint depends.
 * <p/>
 * <strong>Example:</strong> The endpoint for the WorkflowService can be
 * declared like this:
 * 
 * <pre>
 *   public final class WorkflowServiceEndpoint extends OsgiAbstractJobProducerEndpoint&lt;WorkflowServiceImpl&gt;
 * </pre>
 * <p/>
 * <strong>Implementation note:</strong> Type variable <code>A</code>
 * <em>cannot</em> have upper bound
 * {@link com.capestartproject.common.job.api.JobProducer}. Even though this may
 * seem reasonable it will cause trouble with OSGi dependency injection. The
 * dependency will most likely be declared on the service's <em>interface</em>
 * and <em>not</em> the concrete implementation. But only the concrete
 * implementation is a <code>JobProducer</code>. With <code>A</code> having an
 * upper bound of <code>JobProducer</code> the signature of
 * {@link #setService(Object)} will be fixed to that bound:
 * <code>setService(JobProducer)</code>. Now the service cannot be injected
 * anymore.
 */
public abstract class OsgiAbstractJobProducerEndpoint<A> extends AbstractJobProducerEndpoint {
  private A service;
  private ServiceRegistry serviceRegistry;

  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer) {
      return (JobProducer) service;
    } else {
      throw new RuntimeException(format("Service %s is expected to be of type JobProducer", service));
    }
  }

  public void setService(A service) {
    this.service = service;
  }

  public A getSvc() {
    return service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
