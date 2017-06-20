package com.capestartproject.kernel.bundleinfo;

import static com.capestartproject.common.util.EqualsUtil.eq;
import static com.capestartproject.common.util.data.Option.option;

import org.osgi.framework.Bundle;

import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.data.Option;

/** Functions on {@link BundleInfo}. */
public final class BundleInfos {
  public static final String MANIFEST_BUILD_NUMBER = "Build-Number";
	public static final String MANIFEST_DB_VERSION = "Ch-Db-Version";

  private BundleInfos() {
  }

  /** Check if version and build numbers are equal. */
  public static boolean versionEq(BundleInfo a, BundleInfo b) {
    return eq(a.getBundleVersion(), b.getBundleVersion()) && eq(a.getBuildNumber(), b.getBuildNumber());
  }

  /** {@link BundleInfos#versionEq(BundleInfo, BundleInfo)} as a function. */
  public static final Function2<BundleInfo, BundleInfo, Boolean> versionEq = new Function2<BundleInfo, BundleInfo, Boolean>() {
    @Override
    public Boolean apply(BundleInfo a, BundleInfo b) {
      return versionEq(a, b);
    }
  };

  public static final Function<BundleInfo, String> getBundleVersion = new Function<BundleInfo, String>() {
    @Override
    public String apply(BundleInfo bundleInfo) {
      return bundleInfo.getBundleVersion();
    }
  };

  public static final Function<BundleInfo, Option<String>> getBuildNumber = new Function<BundleInfo, Option<String>>() {
    @Override
    public Option<String> apply(BundleInfo bundleInfo) {
      return bundleInfo.getBuildNumber();
    }
  };

  /** Extract the build number of a bundle. */
	public static Option<String> getBuildNumber(Bundle bundle) {
		return option(bundle.getHeaders().get(MANIFEST_BUILD_NUMBER).toString());
  }
}
