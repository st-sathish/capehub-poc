
package com.capestartproject.common.security.api;

import java.util.Map;

public interface Organization {

  /**
   * @return the id
   */
  String getId();

  /**
   * Returns the name for the local anonymous role.
   *
   * @return the anonymous role name
   */
  String getAnonymousRole();

  /**
   * Returns the name for the local admin role.
   *
   * @return the admin role name
   */
  String getAdminRole();

  /**
   * @return the name
   */
  String getName();

  /**
   * Returns the organizational properties
   *
   * @return the properties
   */
  Map<String, String> getProperties();

  /**
   * Returns the server names and the corresponding ports that have been registered with this organization.
   *
   * @return the servers
   */
  Map<String, Integer> getServers();

}
