package com.capestartproject.common.serviceregistry.api;

/**
 * Interface representing a host.
 */
public interface HostRegistration {

  /**
   * @return the baseUrl for this host
   */
  String getBaseUrl();

  /**
   * @param baseUrl
   *          the baseUrl to set
   */
  void setBaseUrl(String baseUrl);

  /**
   * @return the maxJobs
   */
  int getMaxJobs();

  /**
   * @param maxJobs
   *          the maxJobs to set
   */
  void setMaxJobs(int maxJobs);

  /**
   * @return whether this host is active
   */
  boolean isActive();

  /**
   * @param active
   *          the active status to set
   */
  void setActive(boolean active);

  /**
   * @return whether this host is online
   */
  boolean isOnline();

  /**
   * @param online
   *          the online status to set
   */
  void setOnline(boolean online);

  /**
   * @return the maintenanceMode
   */
  boolean isMaintenanceMode();

  /**
   * @param maintenanceMode
   *          the maintenanceMode to set
   */
  void setMaintenanceMode(boolean maintenanceMode);

}
