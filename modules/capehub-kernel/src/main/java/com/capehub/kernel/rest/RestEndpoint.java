
package com.capehub.kernel.rest;

/**
 * Endpoints that wish to receive a notification after they have been registered by the
 * {@link com.capehub.kernel.rest.RestPublisher} should implement this interface.
 */
public interface RestEndpoint {
  /** Called after successful endpoint publication. The endpoint is now reachable via HTTP. */
  void endpointPublished();
}
