package com.capestartproject.kernel.userdirectory;

import com.capestartproject.common.security.api.JaxbOrganization;
import com.capestartproject.common.security.api.JaxbRole;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.RoleProvider;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.UserProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The organization role provider returning the admin and anonymous role from the current organization.
 */
public class OrganizationRoleProvider implements RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OrganizationRoleProvider.class);

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @see com.capestartproject.common.security.api.RoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    Organization organization = securityService.getOrganization();
    List<Role> roles = new ArrayList<Role>();
    roles.add(new JaxbRole(organization.getAdminRole(), JaxbOrganization.fromOrganization(organization), ""));
    roles.add(new JaxbRole(organization.getAnonymousRole(), JaxbOrganization.fromOrganization(organization), ""));
    return roles.iterator();
  }

  /**
   * @see com.capestartproject.common.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    return Collections.emptyList();
  }

  /**
   * @see com.capestartproject.common.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * @see com.capestartproject.common.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    HashSet<Role> foundRoles = new HashSet<Role>();
    for (Iterator<Role> it = getRoles(); it.hasNext();) {
      Role role = it.next();
      if (like(role.getName(), query) || like(role.getDescription(), query))
        foundRoles.add(role);
    }
    return offsetLimitCollection(offset, limit, foundRoles).iterator();
  }

  private <T> HashSet<T> offsetLimitCollection(int offset, int limit, HashSet<T> entries) {
    HashSet<T> result = new HashSet<T>();
    int i = 0;
    for (T entry : entries) {
      if (limit != 0 && result.size() >= limit)
        break;
      if (i >= offset)
        result.add(entry);
      i++;
    }
    return result;
  }

  private boolean like(String string, final String query) {
    String regex = query.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(string).matches();
  }

}
