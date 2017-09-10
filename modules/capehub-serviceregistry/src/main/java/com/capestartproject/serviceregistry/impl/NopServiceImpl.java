package com.capestartproject.serviceregistry.impl;

import static com.capestartproject.common.util.data.functions.Misc.chuck;

import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.job.api.OsgiAbstractJobProducer;
import com.capestartproject.common.serviceregistry.api.NopService;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.util.Log;

/**
 * No operation service.
 * <p/>
 * This dummy service just exists for creating jobs for testing purposes.
 */
public final class NopServiceImpl extends OsgiAbstractJobProducer implements NopService {
  private static final Log log = Log.mk(NopServiceImpl.class);

  public static final String PAYLOAD = "NopServicePayload";

  public NopServiceImpl() {
    super("org.opencastproject.nop");
  }

  @Override protected String process(Job job) throws Exception {
    log.info("Processing job %d", job.getId());
    return PAYLOAD;
  }

  @Override public Job nop() {
    try {
      return getServiceRegistry().createJob(getJobType(), "nop");
    } catch (ServiceRegistryException e) {
      return chuck(e);
    }
  }
}
