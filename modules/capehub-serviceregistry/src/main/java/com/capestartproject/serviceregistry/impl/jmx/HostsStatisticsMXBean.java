package com.capestartproject.serviceregistry.impl.jmx;

/**
 * JMX Bean interface exposing hosts statistics.
 */
public interface HostsStatisticsMXBean {

  /**
   * Gets a list of all hosts
   *
   * @return an array including all hosts
   */
  String[] getAll();

  /**
   * Gets a list of online hosts
   *
   * @return an array including online hosts
   */
  String[] getOnline();

  /**
   * Gets a list of offline hosts
   *
   * @return an array including offline hosts
   */
  String[] getOffline();

  /**
   * Gets a list of hosts in maintenance mode
   *
   * @return an array including hosts in maintenance mode
   */
  String[] getInMaintenance();

  /**
   * Gets the total number of hosts
   *
   * @return the number of hosts
   */
  int getTotalCount();

  /**
   * Gets the number of online hosts
   *
   * @return the number of online hosts
   */
  int getOnlineCount();

  /**
   * Gets the number of offline hosts
   *
   * @return the number of offline hosts
   */
  int getOfflineCount();

  /**
   * Gets the number of hosts in maintenance mode
   *
   * @return the number of hosts in maintenance mode
   */
  int getInMaintenanceCount();

}
