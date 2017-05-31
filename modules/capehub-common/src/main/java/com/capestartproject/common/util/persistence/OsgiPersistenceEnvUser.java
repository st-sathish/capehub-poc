package com.capestartproject.common.util.persistence;

import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

/**
 * Definition of a contract for the use of a {@link PersistenceEnv} in an OSGi environment.
 * <p/>
 * Use in conjunction with {@link PersistenceEnvBuilder}.
 */
public interface OsgiPersistenceEnvUser {
  /** OSGi callback to set persistence properties. */
  void setPersistenceProperties(Map<String, Object> persistenceProperties);

  /** OSGi callback to set persistence provider. */
  void setPersistenceProvider(PersistenceProvider persistenceProvider);

  /**
   * Return the persistence environment.
   * <p/>
   * Create the persistence environment in the activate method like so:
   * <pre>
   *   penv = PersistenceEnvs.persistenceEnvironment(persistenceProvider, "my.persistence.context", persistenceProperties);
   * </pre>
   * Or better use the {@link PersistenceEnvBuilder}.
   */
  PersistenceEnv getPenv();

  /** Close the persistence environment {@link PersistenceEnv#close()}. Call from the deactivate method. */
  void closePenv();
}
