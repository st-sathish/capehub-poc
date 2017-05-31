package com.capestartproject.common.security.api;

import java.util.Iterator;

/**
 * A marker interface for the federation of all {@link RoleProvider}s.
 */
public interface RoleDirectoryService {

  /**
   * Gets all known roles.
   *
   * @return the roles
   */
  Iterator<Role> getRoles();

  /**
   * Return the found role's as an iterator.
   *
   * @param query
   *          the query. Use the wildcards "_" to match any single character and "%" to match an arbitrary number of
   *          characters (including zero characters).
   * @param offset
   *          the offset.
   * @param limit
   *          the limit. 0 means no limit
   * @return an iterator of role's
   * @throws IllegalArgumentException
   *           if the query is <code>null</code>
   */
  Iterator<Role> findRoles(String query, int offset, int limit);

}
