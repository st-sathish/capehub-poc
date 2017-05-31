package com.capestartproject.kernel.bundleinfo;

import com.capestartproject.common.util.data.Option;

public interface BundleInfo {
  /** The host where the bundle lives. */
  String getHost();

  /** The bundle's OSGi symbolic name. */
  String getBundleSymbolicName();

  /** The bundle id in the OSGi container. */
  long getBundleId();

  /** The full bundle version which is a tuple of {@link #getBundleVersion()} and {@link #getBuildNumber()}. */
  BundleVersion getVersion();

  /** The OSGi bundle version. */
  String getBundleVersion();

  /** Get the build number, e.g. the git hash. */
  Option<String> getBuildNumber();
}
