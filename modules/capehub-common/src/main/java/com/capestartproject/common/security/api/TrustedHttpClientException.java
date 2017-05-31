package com.capestartproject.common.security.api;

import java.io.IOException;

/**
 * An Exception indicating that the trusted http communication failed.
 */
public class TrustedHttpClientException extends IOException {
  public TrustedHttpClientException(Throwable t) {
    super(t);
  }

  private static final long serialVersionUID = -2818213486414023509L;
}
