package com.capestartproject.kernel.bundleinfo;

import static com.capestartproject.common.util.EqualsUtil.eq;
import static com.capestartproject.common.util.EqualsUtil.hash;

import com.capestartproject.common.util.data.Option;

public final class BundleVersion {
  private final String bundleVersion;
  private final Option<String> buildNumber;

  public BundleVersion(String bundleVersion, Option<String> buildNumber) {
    this.bundleVersion = bundleVersion;
    this.buildNumber = buildNumber;
  }

  public static BundleVersion version(String bundleVersion, Option<String> buildNumber) {
    return new BundleVersion(bundleVersion, buildNumber);
  }

  public String getBundleVersion() {
    return bundleVersion;
  }

  public Option<String> getBuildNumber() {
    return buildNumber;
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof BundleVersion && eqFields((BundleVersion) that));
  }

  private boolean eqFields(BundleVersion that) {
    return eq(this.bundleVersion, that.bundleVersion) && eq(this.buildNumber, that.buildNumber);
  }

  @Override public int hashCode() {
    return hash(bundleVersion, buildNumber);
  }
}
