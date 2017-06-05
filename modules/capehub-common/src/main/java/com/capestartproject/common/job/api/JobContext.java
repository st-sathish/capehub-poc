package com.capestartproject.common.job.api;

import java.util.Map;

/**
 * Runtime contextual information to be passed around with jobs.
 */
public interface JobContext {

  // /**
  // * Returns the identifier of the parent job or <code>null</code> if there is no parent job.
  // *
  // * @return the parent job identifier
  // */
  // Long getParentJobId();
  //
  // /**
  // * Returns the user that is associated with the job.
  // *
  // * @return the user id
  // */
  // String getUserId();

  /**
   * Gets the job context identifier.
   *
   * @return the job context identifier
   */
  Long getId();

  /**
   * Returns any additional contextual data.
   *
   * @return the contextual data
   */
  Map<String, String> getProperties();

}
