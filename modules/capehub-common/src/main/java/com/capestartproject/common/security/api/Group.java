package com.capestartproject.common.security.api;

import java.util.Set;

/**
 * Represent a group in capehub
 */
public interface Group {

  /** Prefix of every generated group role */
  String ROLE_PREFIX = "ROLE_GROUP_";

  /**
   * Gets the group identifier.
   *
   * @return the group identifier
   */
  String getGroupId();

  /**
   * Gets the group name.
   *
   * @return the group name
   */
  String getName();

  /**
   * Gets the user's organization.
   *
   * @return the organization
   */
  Organization getOrganization();

  /**
   * Gets the role description.
   *
   * @return the description
   */
  String getDescription();

  /**
   * Gets the group role.
   *
   * @return the group role
   */
  String getRole();

  /**
   * Gets the group members
   *
   * @return the group members
   */
  Set<String> getMembers();

  /**
   * Gets the group's roles.
   *
   * @return the group's roles
   */
  Set<Role> getRoles();

}
