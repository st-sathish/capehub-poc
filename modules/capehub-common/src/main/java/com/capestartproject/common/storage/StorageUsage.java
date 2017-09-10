package com.capestartproject.common.storage;

import com.capestartproject.common.util.data.Option;

/**
 * Provides access to storage usage information
 */
public interface StorageUsage {

  /**
   * Gets the total space of storage in Bytes
   *
   * @return Number of all bytes in storage
   */
  Option<Long> getTotalSpace();

  /**
   * Gets the available space of storage in Bytes This is free storage that is not reserved
   *
   * @return Number of available bytes in storage
   */
  Option<Long> getUsableSpace();

  /**
   * Gets the used space of storage in Bytes
   *
   * @return Number of used bytes in storage
   */
  Option<Long> getUsedSpace();

}
