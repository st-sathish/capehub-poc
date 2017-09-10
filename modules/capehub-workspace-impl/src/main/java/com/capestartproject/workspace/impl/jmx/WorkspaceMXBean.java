package com.capestartproject.workspace.impl.jmx;

/**
 * JMX Bean interface exposing workspace storage information
 */
public interface WorkspaceMXBean {

  /**
   * Gets the free workspace space in bytes
   *
   * @return free space in bytes
   */
  long getFreeSpace();

  /**
   * Gets the used workspace space in bytes
   *
   * @return used space in bytes
   */
  long getUsedSpace();

  /**
   * Gets the total available workspace space in bytes
   *
   * @return total available space in bytes
   */
  long getTotalSpace();

}
