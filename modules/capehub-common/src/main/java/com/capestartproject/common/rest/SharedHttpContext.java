package com.capestartproject.common.rest;

/**
 * Constant definition for the shared <code>HTTP</code> context.
 */
public interface SharedHttpContext {

  /**
   * The shared context key as used throughout OSGi.
   */
  String CONTEXT_ID = "httpContext.id";

  /**
   * The context key for marking shared contexts.
   */
  String SHARED = "httpContext.shared";

  /**
   * The key for the servlet alias.
   */
  String ALIAS = "alias";

  /**
   * Key for the servlet name.
   */
  String SERVLET_NAME = "servlet-name";

  /**
   * The key for defining a pattern for request filters.
   */
  String PATTERN = "urlPatterns";

  /**
   * Prefix for servlet init keys.
   */
  String INIT_PREFIX = "init.";

  /**
   * Property to define the ranking of a service in the filter chain
   */
  String SERVICE_RANKING = "service.ranking";

}
