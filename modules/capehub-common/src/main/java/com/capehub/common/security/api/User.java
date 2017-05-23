
package com.capehub.common.security.api;

import java.util.Set;

/**
 * Represent a user in Capehub
 */
public interface User {

  /**
   * Gets this user's unique account name.
   *
   * @return the account name
   */
  String getUsername();

  /**
   * Gets this user's password, if available.
   *
   * @return the password
   */
  String getPassword();

  /**
   * Returns <code>true</code> if this user object can be used to log into Matterhorn.
   *
   * @return <code>true</code> if this user can login
   */
  boolean canLogin();

  /**
   * Returns the user's organization identifier.
   *
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Gets the user's roles. For anonymous users, this will return {@link Anonymous}.
   *
   * @return the user's roles
   */
  Set<Role> getRoles();

  /**
   * Returns whether the user is in a specific role.
   *
   * @param role
   *          the role to check
   * @return whether the role is one of this user's roles
   */
  boolean hasRole(String role);

}
