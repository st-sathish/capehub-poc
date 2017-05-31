
package com.capestartproject.common.security.api;

/**
 * Represent a role in capehub
 */
public interface Role {

  /**
   * Gets the role name
   *
   * @return the role name
   */
  String getName();

  /**
   * Gets the role description
   *
   * @return the role description
   */
  String getDescription();

  /**
   * Returns the role's organization identifier.
   *
   * @return the organization
   */
  Organization getOrganization();

}
