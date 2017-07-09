package com.capestartproject.kernel.bundleinfo;

import static com.capestartproject.common.util.persistence.PersistenceEnvs.persistenceEnvironment;

import java.util.Map;

import javax.persistence.spi.PersistenceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.util.persistence.PersistenceEnv;

/** OSGi bound bundle info database. */
public class OsgiBundleInfoDb extends AbstractBundleInfoDb {
  private static final Logger logger = LoggerFactory.getLogger(OsgiBundleInfoDb.class);

	public static final String PERSISTENCE_UNIT = "com.capestartproject.kernel";
	private PersistenceEnv penv;

	private Map<String, Object> persistenceProperties;
	private PersistenceProvider persistenceProvider;

  /** OSGi DI */
	public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
		this.persistenceProperties = persistenceProperties;
	}

	/** OSGi DI */
	public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

  /** OSGi callback */
  public void activate() {
		penv = persistenceEnvironment(persistenceProvider, PERSISTENCE_UNIT, persistenceProperties);
		logger.info("Activated persistence environment {}", penv);
  }

  public void deactivate() {
    logger.info("Closing persistence environment");
    penv.close();
  }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.kernel.bundleinfo.AbstractBundleInfoDb#
	 * getPersistenceEnv()
	 */
	@Override
	protected PersistenceEnv getPersistenceEnv() {
		// TODO Auto-generated method stub
		return penv;
	}
}
