package com.capestartproject.common.util.persistence;

import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Option;
import org.osgi.service.component.ComponentContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;
import java.util.Map;

import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.functions.Misc.chuck;
import static com.capestartproject.common.util.persistence.PersistenceUtil.createEntityManager;
import static com.capestartproject.common.util.persistence.PersistenceUtil.newEntityManagerFactory;
import static com.capestartproject.common.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

/** Persistence environment factory. */
public final class PersistenceEnvs {
  private PersistenceEnvs() {
  }

  private interface Transactional {
    /** Run a function in a transactional context. */
    <A> A tx(Function<EntityManager, A> transactional);
  }

  private static final ThreadLocal<Option<Transactional>> emStore = new ThreadLocal<Option<Transactional>>() {
    @Override protected Option<Transactional> initialValue() {
      return none();
    }
  };

  /**
   * Create a new, concurrently usable persistence environment which uses JPA local transactions.
   * <p/>
   * Transaction propagation is supported on a per thread basis.
   */
  public static PersistenceEnv persistenceEnvironment(final EntityManagerFactory emf) {
    final Transactional startTx = new Transactional() {
      @Override
      public <A> A tx(Function<EntityManager, A> transactional) {
        for (final EntityManager em : createEntityManager(emf)) {
          final EntityTransaction tx = em.getTransaction();
          try {
            tx.begin();
            emStore.set(Option.<Transactional>some(new Transactional() {
              @Override public <A> A tx(Function<EntityManager, A> transactional) {
                return transactional.apply(em);
              }
            }));
            A ret = transactional.apply(em);
            tx.commit();
            return ret;
          } catch (Exception e) {
            if (tx.isActive()) {
              tx.rollback();
            }
            // propagate exception
            return chuck(e);
          } finally {
            if (em.isOpen())
              em.close();
            emStore.remove();
          }
        }
        return chuck(new IllegalStateException("EntityManager is already closed"));
      }
    };
    return new PersistenceEnv() {
      Transactional currentTx() {
        return emStore.get().getOrElse(startTx);
      }

      @Override public <A> A tx(Function<EntityManager, A> transactional) {
        return currentTx().tx(transactional);
      }

      @Override public void close() {
        emf.close();
      }
    };
  }

  /**
   * Shortcut for <code>persistenceEnvironment(newEntityManagerFactory(cc, emName))</code>.
   *
   * @see PersistenceUtil#newEntityManagerFactory(org.osgi.service.component.ComponentContext, String)
   */
  public static PersistenceEnv persistenceEnvironment(ComponentContext cc, String emName) {
    return persistenceEnvironment(newEntityManagerFactory(cc, emName));
  }

  /**
   * Shortcut for <code>newPersistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps))</code>.
   *
   * @see PersistenceUtil#newEntityManagerFactory(org.osgi.service.component.ComponentContext, String, java.util.Map)
   */
  public static PersistenceEnv persistenceEnvironment(ComponentContext cc, String emName, Map persistenceProps) {
    return persistenceEnvironment(newEntityManagerFactory(cc, emName, persistenceProps));
  }

  /** Create a new persistence environment. This method is the preferred way of creating a persistence environment. */
  public static PersistenceEnv persistenceEnvironment(PersistenceProvider persistenceProvider, String emName,
                                                      Map persistenceProps) {
    return persistenceEnvironment(persistenceProvider.createEntityManagerFactory(emName, persistenceProps));
  }

  /**
   * Create a new persistence environment based on an entity manager factory backed by an in-memory H2 database for
   * testing purposes.
   *
   * @param emName
   *         name of the persistence unit (see META-INF/persistence.xml)
   */
  public static PersistenceEnv testPersistenceEnv(String emName) {
    return persistenceEnvironment(newTestEntityManagerFactory(emName));
  }
}
