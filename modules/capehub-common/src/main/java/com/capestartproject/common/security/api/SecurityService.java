
package com.capestartproject.common.security.api;

/**
 * Provides access to the current user's username and roles, if any.
 */
public interface SecurityService {

  /**
   * Gets the current user, or the local organization's anonymous user if the user has not been authenticated.
   *
   * @return the user
   * @throws IllegalStateException
   *           if no organization is set in the security context
   */
  User getUser() throws IllegalStateException;

  /**
   * Gets the organization associated with the current thread context.
   *
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Sets the organization for the calling thread.
   *
   * @param organization
   *          the organization
   */
  void setOrganization(Organization organization);

  /**
   * Sets the current thread's user context to another user. This is useful when spawning new threads that must contain
   * the parent thread's user context.
   *
   * @param user
   *          the user to set for the current user context
   */
  void setUser(User user);

}
