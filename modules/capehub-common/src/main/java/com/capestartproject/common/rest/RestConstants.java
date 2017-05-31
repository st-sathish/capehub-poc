
package com.capestartproject.common.rest;

/**
 * Constant definitions used to define and consume Capehub REST services.
 */
public interface RestConstants {

  /** The service property indicating the type of service. This is an arbitrary ID, not necessarily a java interface. */
  String SERVICE_TYPE_PROPERTY = "capehub.service.type";

  /** The service property indicating the URL path that the service is attempting to claim */
  String SERVICE_PATH_PROPERTY = "capehub.service.path";

  /** The service property indicating whether the service should be published in the service registry */
  String SERVICE_PUBLISH_PROPERTY = "capehub.service.publish";

  /** The service property indicating that this service should be registered in the remote service registry */
  String SERVICE_JOBPRODUCER_PROPERTY = "capehub.service.jobproducer";

  /** The ID by which this http context is known by the extended http service */
  String HTTP_CONTEXT_ID = "capehub.httpcontext";

  /** The OSGI service filter that returns all registered services published as REST endpoints */
  String SERVICES_FILTER = "(&(!(objectClass=javax.servlet.Servlet))(" + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

  /** The bundle header used to find the static resource URL alias */
  String HTTP_ALIAS = "Http-Alias";

  /** The bundle header used to find the static resource classpath */
  String HTTP_CLASSPATH = "Http-Classpath";

  /** The bundle header used to find the static resource welcome file */
  String HTTP_WELCOME = "Http-Welcome";

  /**
   * The amount of time in seconds to wait for a session to be inactive before deallocating it. Applied to all sessions
   * with the last filter in the chain.
   **/
  int MAX_INACTIVE_INTERVAL = 1800;
}
