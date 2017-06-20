package com.capestartproject.kernel.bundleinfo;

import static com.capestartproject.common.util.data.Option.none;

import com.capestartproject.common.util.data.Option;

public class BundleInfoImpl implements BundleInfo {
  private final String host;
  private final String bundleSymbolicName;
  private final long bundleId;
  private final String bundleVersion;
  private final Option<String> buildNumber;
  private final Option<String> dbSchemaVersion;
  private final BundleVersion version;

  public BundleInfoImpl(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
          Option<String> buildNumber, Option<String> dbSchemaVersion) {
    this.host = host;
    this.bundleSymbolicName = bundleSymbolicName;
    this.bundleId = bundleId;
    this.bundleVersion = bundleVersion;
    this.buildNumber = buildNumber;
    this.dbSchemaVersion = dbSchemaVersion;
    this.version = new BundleVersion(bundleVersion, buildNumber);
  }

  public static BundleInfo bundleInfo(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
			Option<String> buildNumber) {
    return new BundleInfoImpl(host, bundleSymbolicName, bundleId, bundleVersion, buildNumber, none(""));
  }

  public static BundleInfo bundleInfo(String host, String bundleSymbolicName, long bundleId, String bundleVersion,
          Option<String> buildNumber, Option<String> dbSchemaVersion) {
    return new BundleInfoImpl(host, bundleSymbolicName, bundleId, bundleVersion, buildNumber, dbSchemaVersion);
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getBundleSymbolicName() {
    return bundleSymbolicName;
  }

  @Override
  public long getBundleId() {
    return bundleId;
  }

  @Override
  public String getBundleVersion() {
    return bundleVersion;
  }

  @Override
  public Option<String> getBuildNumber() {
    return buildNumber;
  }

  @Override
  public BundleVersion getVersion() {
    return version;
  }
}
