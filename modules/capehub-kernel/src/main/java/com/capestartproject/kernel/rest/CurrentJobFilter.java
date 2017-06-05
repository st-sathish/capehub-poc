package com.capestartproject.kernel.rest;

import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Inspects request current job header and sets the current job for the request.
 */
public class CurrentJobFilter implements Filter {

  public static final String CURRENT_JOB_HEADER = "X-Opencast-Matterhorn-Current-Job-Id";

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CurrentJobFilter.class);

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /**
   * Sets the service registry.
   *
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    try {
      setCurrentJob(httpRequest, httpResponse);
      chain.doFilter(httpRequest, httpResponse);
    } finally {
      serviceRegistry.setCurrentJob(null);
    }
  }

  /**
   * Sets the current job on the new thread
   *
   * @param httpRequest
   *          the HTTP request
   * @param httpResponse
   *          the HTTP response
   * @throws IOException
   *           if the error response was not able to be sent
   */
  private void setCurrentJob(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
    String currentJobId = httpRequest.getHeader(CURRENT_JOB_HEADER);
    try {
      if (StringUtils.isNotBlank(currentJobId)) {
        Job currentJob = serviceRegistry.getJob(Long.parseLong(currentJobId));
        serviceRegistry.setCurrentJob(currentJob);
      }
    } catch (Exception e) {
      logger.error("Was not able to set the current job id {} to the service registry", currentJobId);
      httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Was not able to set the current job id {} to the service registry" + currentJobId);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
  }

}
