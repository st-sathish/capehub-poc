package com.capestartproject.kernel.http.impl;

import com.capestartproject.kernel.http.api.HttpClient;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/** Creates HttpClients that can be used for making requests such as GET, POST etc.*/
public class HttpClientFactory implements ManagedService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);


  /**
   * Callback from the OSGi container once this service is started. This is where we register our shell commands.
   *
   * @param ctx
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    logger.debug("Starting up");
  }

  /**
   * Deactivates the service
   */
  public void deactivate() {
    logger.debug("Shutting down");
  }

  /** Updates the properties for this service. */
  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {

  }

  /** Creates a new HttpClient to make requests.*/
  public HttpClient makeHttpClient() {
    return new HttpClientImpl();
  }
}
