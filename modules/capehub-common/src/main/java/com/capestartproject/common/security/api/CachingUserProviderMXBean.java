package com.capestartproject.common.security.api;

/**
 * A JMX interface for caching user providers.
 */
public interface CachingUserProviderMXBean {

  /**
   * Gets the ratio of cache hits to total requests.
   *
   * @return the hit ratio
   */
  float getCacheHitRatio();

}
