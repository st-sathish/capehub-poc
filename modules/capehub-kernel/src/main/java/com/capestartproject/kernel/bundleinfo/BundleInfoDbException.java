package com.capestartproject.kernel.bundleinfo;

public class BundleInfoDbException extends RuntimeException {
  public BundleInfoDbException(String s) {
    super(s);
  }

  public BundleInfoDbException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public BundleInfoDbException(Throwable throwable) {
    super(throwable);
  }
}
