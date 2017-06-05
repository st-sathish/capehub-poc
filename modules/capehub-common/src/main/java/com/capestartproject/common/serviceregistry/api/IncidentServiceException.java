package com.capestartproject.common.serviceregistry.api;

/**
 * Exception that is thrown during incident service lookups.
 */
public class IncidentServiceException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = 4632653975890321521L;

  /**
   * Creates a new incident service exception.
   *
   * @param message
   *          the error message
   * @param t
   *          the exception causing the error
   */
  public IncidentServiceException(String message, Throwable t) {
    super(message, t);
  }

  /**
   * Creates a new incident service exception.
   *
   * @param message
   *          the error message
   */
  public IncidentServiceException(String message) {
    super(message);
  }

  /**
   * Creates a new incident service exception.
   *
   * @param t
   *          the exception causing the error
   */
  public IncidentServiceException(Throwable t) {
    super(t);
  }

}
