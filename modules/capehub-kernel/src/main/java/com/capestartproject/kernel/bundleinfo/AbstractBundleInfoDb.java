package com.capestartproject.kernel.bundleinfo;

import static com.capestartproject.common.util.data.Monadics.mlist;
import static com.capestartproject.common.util.persistence.Queries.persist;

import java.util.List;

import javax.persistence.EntityManager;

import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.persistence.PersistenceEnv;

public abstract class AbstractBundleInfoDb implements BundleInfoDb {
  protected abstract PersistenceEnv getPersistenceEnv();

  @Override
  public void store(final BundleInfo info) {
    tx(persist(BundleInfoJpa.create(info)));
  }

  @Override
  public void delete(String host, long bundleId) {
    tx(BundleInfoJpa.delete(host, bundleId));
  }

  @Override
  public void clear(String host) {
    tx(BundleInfoJpa.deleteByHost(host));
  }

  @Override
  public void clearAll() throws BundleInfoDbException {
    tx(BundleInfoJpa.deleteAll);
  }

  @Override
  public List<BundleInfo> getBundles() {
    return mlist(tx(BundleInfoJpa.findAll)).map(BundleInfoJpa.toBundleInfo).value();
  }

  @Override
  public List<BundleInfo> getBundles(String... prefixes) throws BundleInfoDbException {
    return mlist(tx(BundleInfoJpa.findAll(prefixes))).map(BundleInfoJpa.toBundleInfo).value();
  }

  private <A> A tx(Function<EntityManager, A> f) {
    return getPersistenceEnv().<A> tx().rethrow(exTransformer).apply(f);
  }

  private static final Function<Exception, BundleInfoDbException> exTransformer = new Function<Exception, BundleInfoDbException>() {
    @Override
    public BundleInfoDbException apply(Exception e) {
      return new BundleInfoDbException(e);
    }
  };
}
