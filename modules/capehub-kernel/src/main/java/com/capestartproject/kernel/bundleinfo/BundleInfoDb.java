package com.capestartproject.kernel.bundleinfo;

import java.util.List;

/** Persistence for bundle information. */
public interface BundleInfoDb {
  /** Store a bundle info object. */
  void store(BundleInfo info) throws BundleInfoDbException;

  /** Delete a bundle. */
  void delete(String host, long bundleId) throws BundleInfoDbException;

  /** Clear the database for a certain host. */
  void clear(String host) throws BundleInfoDbException;

  /** Clear the complete database. */
  void clearAll() throws BundleInfoDbException;

  /** Return a list of all running bundles. */
  List<BundleInfo> getBundles() throws BundleInfoDbException;

  /** Return a list of all running bundles whose symbolic names start with one of the given prefixes. */
  List<BundleInfo> getBundles(String... prefixes) throws BundleInfoDbException;
}
