package com.capestartproject.ingest.impl.jmx;

/**
 * JMX Bean interface exposing ingest statistics.
 */
public interface IngestStatisticsMXBean {

  /**
   * Gets the number of successful ingest operations
   *
   * @return the number of ingest operations
   */
  int getSuccessfulIngestOperations();

  /**
   * Gets the number of failed ingest operations
   *
   * @return the number of ingest operations
   */
  int getFailedIngestOperations();

  /**
   * Gets the total number of ingested bytes
   *
   * @return the number of bytes
   */
  long getTotalBytes();

  /**
   * Gets the total number of ingested bytes in the last minute
   *
   * @return the number of bytes
   */
  long getBytesInLastMinute();

  /**
   * Gets the total number of ingested bytes in the last five minutes
   *
   * @return the number of bytes
   */
  long getBytesInLastFiveMinutes();

  /**
   * Gets the total number of ingested bytes in the last fifteen minutes
   *
   * @return the number of bytes
   */
  long getBytesInLastFifteenMinutes();

}
