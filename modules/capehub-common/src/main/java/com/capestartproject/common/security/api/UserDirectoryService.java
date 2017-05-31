package com.capestartproject.common.security.api;

import java.util.Iterator;

/**
 * A marker interface for federation of all {@link UserProvider}s.
 */
public interface UserDirectoryService {

  /**
   * Gets all known users.
   *
   * @return the users
   */
  Iterator<User> getUsers();

  /**
   * Loads a user by username, or returns null if this user is not known to the thread's current organization.
   *
   * @param userName
   *          the username
   * @return the user
   * @throws IllegalStateException
   *           if no organization is set for the current thread
   */
  User loadUser(String userName);

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

  /**
   * Discards any cached value for given user name.
   *
   * @param userName
   *          the user name
   */
  void invalidate(String userName);

}
