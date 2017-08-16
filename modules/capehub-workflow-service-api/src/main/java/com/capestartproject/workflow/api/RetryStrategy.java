package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * List of possible retry strategies in case of operation hold state
 */
public enum RetryStrategy {

  /** Failed without retry strategy */
  NONE,

  /** Restart the operation */
  RETRY,

  /** Keep the operation in hold state */
  HOLD;

  public static class Adapter extends XmlAdapter<String, RetryStrategy> {

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public String marshal(RetryStrategy retryStrategy) {
      return retryStrategy == null ? null : retryStrategy.toString().toLowerCase();
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public RetryStrategy unmarshal(String val) {
      return val == null ? null : RetryStrategy.valueOf(val.toUpperCase());
    }

  }

}
