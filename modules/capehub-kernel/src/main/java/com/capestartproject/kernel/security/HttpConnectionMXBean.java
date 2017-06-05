package com.capestartproject.kernel.security;

/**
 * An MxBean that exposes the number of open http connections to a JXM agent
 */
public interface HttpConnectionMXBean {
  /** Gets the number of open http connections */
  int getOpenConnections();
}
