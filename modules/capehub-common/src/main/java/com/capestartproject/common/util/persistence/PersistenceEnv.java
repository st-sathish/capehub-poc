
package com.capestartproject.common.util.persistence;

import com.capestartproject.common.util.data.Function;

import javax.persistence.EntityManager;

/** Persistence environment to perform a transaction. */
public abstract class PersistenceEnv {
  /** Run code inside a transaction. */
  public abstract <A> A tx(Function<EntityManager, A> transactional);

  /** {@link #tx(org.opencastproject.util.data.Function)} as a function. */
  public <A> Function<Function<EntityManager, A>, A> tx() {
    return new Function<Function<EntityManager, A>, A>() {
      @Override
      public A apply(Function<EntityManager, A> transactional) {
        return tx(transactional);
      }
    };
  }

  /** Close the environment and free all associated resources. */
  public abstract void close();
}
