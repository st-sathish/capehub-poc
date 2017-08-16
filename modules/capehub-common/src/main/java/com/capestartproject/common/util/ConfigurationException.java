package com.capestartproject.common.util;

/**
 * This exception is thrown on various occasions where the system detects a state of malconfiguration.
 */
public class ConfigurationException extends RuntimeException {

  /** Serial Version UID */
  private static final long serialVersionUID = -3960378289149011212L;

  /**
   * Creates a new configuration exception.
   *
   * @param message
   *          the exception message
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates a new configuration exception with the given message and cause of the malconfiguration.
   *
   * @param message
   *          the message
   * @param cause
   *          the exception cause
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
