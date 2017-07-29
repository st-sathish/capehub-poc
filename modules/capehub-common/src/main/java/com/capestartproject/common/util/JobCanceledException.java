package com.capestartproject.common.util;

import com.capestartproject.common.job.api.Job;

/**
 * This exception is thrown by the
 * {@link com.capestartproject.common.job.api.JobBarrier} and indicates that the
 * job has been canceled.
 */
public class JobCanceledException extends RuntimeException {

  /** Serial version uid */
  private static final long serialVersionUID = -326050100245540889L;

  public JobCanceledException(Job job) {
    super(job + " has been canceled during service shutdown and will be rescheduled");
  }

}
