package com.capestartproject.workingfilerepository.jmx;

/**
 * JMX Bean interface exposing working file repository storage information
 */
public interface WorkingFileRepositoryMXBean {

  /**
   * Gets the free working file repository space in bytes
   *
   * @return free space in bytes
   */
  long getFreeSpace();

  /**
   * Gets the used working file repository space in bytes
   *
   * @return used space in bytes
   */
  long getUsedSpace();

  /**
   * Gets the total available working file repository space in bytes
   *
   * @return total available space in bytes
   */
  long getTotalSpace();

}
