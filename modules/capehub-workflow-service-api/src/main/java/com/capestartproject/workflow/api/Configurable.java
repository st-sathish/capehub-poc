package com.capestartproject.workflow.api;

import java.util.Set;

/**
 * A configurable entity.
 *
 */
public interface Configurable {
  /**
   * Returns the value of property <code>name</code> or <code>null</code> if no such property has been set.
   *
   * @param key
   *          the configuration key
   * @return the configuration value
   */
  String getConfiguration(String key);

  /**
   * Sets the configuration with name <code>key</code> to value <code>value</code>, or adds it if it doesn't already
   * exist.
   *
   * @param key
   *          the configuration key
   * @param value
   *          the configuration value
   */
  void setConfiguration(String key, String value);

  /**
   * Gets the configuration keys that are currently set for this configurable entity.
   *
   * @return the configuration keys
   */
  Set<String> getConfigurationKeys();

  /**
   * Removes the <code>key</code> configuration.
   *
   * @param key
   *          the configuration key
   */
  void removeConfiguration(String key);

}
