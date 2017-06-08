package com.capestartproject.common.serviceregistry.api;

import com.capestartproject.common.job.api.Job;

/**
 * No operation service.
 * <p/>
 * This dummy service just exists for creating jobs for testing purposes.
 */
public interface NopService {
  Job nop();
}
