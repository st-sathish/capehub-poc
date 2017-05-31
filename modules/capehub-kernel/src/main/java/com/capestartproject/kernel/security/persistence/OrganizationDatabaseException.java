package com.capestartproject.kernel.security.persistence;

/**
 * Presents exception that occurs while storing/retrieving organizations from persistence storage.
 */
public class OrganizationDatabaseException extends RuntimeException {

  /**
   * UUID
   */
  private static final long serialVersionUID = 1291868652443428386L;

  /**
   * Create exception.
   */
  public OrganizationDatabaseException() {
  }

  /**
   * Create exception with a message.
   *
   * @param message
   */
  public OrganizationDatabaseException(String message) {
    super(message);
  }

  /**
   * Create exception with a cause.
   *
   * @param cause
   */
  public OrganizationDatabaseException(Throwable cause) {
    super(cause);
  }

  /**
   * Create exception with a message and a cause.
   *
   * @param message
   * @param cause
   */
  public OrganizationDatabaseException(String message, Throwable cause) {
    super(message, cause);
  }

}
