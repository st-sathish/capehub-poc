package com.capestartproject.common.security.api;

import java.util.Iterator;

/**
 * Provides access to users and roles.
 */
public interface UserProvider {

  /** The constant indicating that a provider should be consulted for all organizations */
  String ALL_ORGANIZATIONS = "*";

  /**
   * Gets all known users.
   *
   * @return the users
   */
  Iterator<User> getUsers();

  /**
   * Loads a user by username, or returns null if this user is not known to this provider.
   *
   * @param userName
   *          the username
   * @return the user
   */
  User loadUser(String userName);

  /**
   * Returns the identifier for the organization that is associated with this user provider. If equal to
   * {@link #ALL_ORGANIZATIONS}, this provider will always be consulted, regardless of the organization.
   *
   * @return the defining organization
   */
  String getOrganization();

  /**
   * Return the found user's as an iterator.
   *
   * @param query
   *          the query. Use the wildcards "_" to match any single character and "%" to match an arbitrary number of
   *          characters (including zero characters).
   * @param offset
   *          the offset
   * @param limit
   *          the limit. 0 means no limit
   * @return an iterator of user's
   * @throws IllegalArgumentException
   *           if the query is <code>null</code>
   */
  Iterator<User> findUsers(String query, int offset, int limit);

}
