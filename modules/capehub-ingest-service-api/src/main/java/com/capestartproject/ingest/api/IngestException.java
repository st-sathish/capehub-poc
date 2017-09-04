package com.capestartproject.ingest.api;

/**
 * Exception throws due to a problem ingesting employee xml package or metadata
 * files.
 */
public class IngestException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = -321218799805646569L;

  /**
   * Constructs an ingest exception
   *
   * @param message
   *          the failure message
   */
  public IngestException(String message) {
    super(message);
  }

  /**
   * Constructs an ingest exception
   *
   * @param cause
   *          the original cause
   */
  public IngestException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs an ingest exception
   *
   * @param message
   *          the failure message
   * @param cause
   *          the original cause
   */
  public IngestException(String message, Throwable t) {
    super(message, t);
  }

}
