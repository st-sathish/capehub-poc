package com.capestartproject.common.util.persistence;

import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Lazy;

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

import static com.capestartproject.common.util.data.Collections.map;

/**
 * Builder for persistence environments.
 * Useful in OSGi bound services where required properties are injected by the OSGi environment.
 */
public final class PersistenceEnvBuilder {
  private Map<String, Object> persistenceProperties = map();
  private PersistenceProvider persistenceProvider;
  private String persistenceUnit;
  private Lazy<PersistenceEnv> penv = Lazy.lazy(new Function0<PersistenceEnv>() {
    @Override public PersistenceEnv apply() {
      if (persistenceProvider == null) {
        throw new IllegalStateException("Persistence provider has not been set yet");
      }
      if (persistenceUnit == null) {
        throw new IllegalStateException("Persistence unit has not been set yet");
      }
      return PersistenceEnvs.persistenceEnvironment(persistenceProvider, persistenceUnit, persistenceProperties);
    }
  });

  public PersistenceEnvBuilder() {
  }

  public PersistenceEnvBuilder(String persistenceUnit) {
    this.persistenceUnit = persistenceUnit;
  }

  /** Set the mandatory name of the persistence unit. */
  public void setPersistenceUnit(String name) {
    this.persistenceUnit = name;
  }

  /** Set the optional persistence properties. */
  public void setPersistenceProperties(Map<String, Object> properties) {
    this.persistenceProperties = properties;
  }

  /** Set the mandatory persistence provider. */
  public void setPersistenceProvider(PersistenceProvider provider) {
    this.persistenceProvider = provider;
  }

  /** Builds the persistence env. Always returns the same environment so it may be safely called multiple times. */
  public PersistenceEnv buildOrGet() {
    return penv.value();
  }
}
