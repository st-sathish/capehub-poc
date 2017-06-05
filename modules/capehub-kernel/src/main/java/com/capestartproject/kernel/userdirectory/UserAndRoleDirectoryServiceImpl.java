package com.capestartproject.kernel.userdirectory;

import static com.capestartproject.common.security.api.UserProvider.ALL_ORGANIZATIONS;
import static com.capestartproject.common.util.data.Tuple.tuple;

import com.capestartproject.common.security.api.JaxbOrganization;
import com.capestartproject.common.security.api.JaxbRole;
import com.capestartproject.common.security.api.JaxbUser;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.RoleDirectoryService;
import com.capestartproject.common.security.api.RoleProvider;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.security.api.UserProvider;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Tuple;

import com.google.common.collect.MapMaker;

import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Federates user and role providers, and exposes a spring UserDetailsService so user lookups can be used by spring
 * security.
 */
public class UserAndRoleDirectoryServiceImpl implements UserDirectoryService, UserDetailsService, RoleDirectoryService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserAndRoleDirectoryServiceImpl.class);

  /** A non-obvious password to allow a Spring User to be instantiated for CAS authenticated users having no password */
  private static final String DEFAULT_PASSWORD = "4b3e4b30-718c-11e2-bcfd-0800200c9a66";

  /** The list of user providers */
  protected List<UserProvider> userProviders = new ArrayList<UserProvider>();

  /** The list of role providers */
  protected List<RoleProvider> roleProviders = new ArrayList<RoleProvider>();

  /** The security service */
  protected SecurityService securityService = null;

  /** A token to store in the miss cache */
  private Object nullToken = new Object();

  /** A cache of users, which lightens the load on the LDAP server */
  private ConcurrentMap<Tuple<String, String>, Object> cache = new MapMaker().maximumSize(1000)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .makeComputingMap(new com.google.common.base.Function<Tuple<String, String>, Object>() {
            @Override
            public Object apply(Tuple<String, String> orgUser) {
              User user = loadUser.apply(orgUser);
              return user == null ? nullToken : user;
            }
          });

  /**
   * Adds a user provider.
   *
   * @param userProvider
   *          the user provider to add
   */
  protected synchronized void addUserProvider(UserProvider userProvider) {
    logger.debug("Adding {} to the list of user providers", userProvider);
    userProviders.add(userProvider);
  }

  /**
   * Remove a user provider.
   *
   * @param userProvider
   *          the user provider to remove
   */
  protected synchronized void removeUserProvider(UserProvider userProvider) {
    logger.debug("Removing {} from the list of user providers", userProvider);
    roleProviders.remove(userProvider);
  }

  /**
   * Adds a role provider.
   *
   * @param roleProvider
   *          the role provider to add
   */
  protected synchronized void addRoleProvider(RoleProvider roleProvider) {
    logger.debug("Adding {} to the list of role providers", roleProvider);
    roleProviders.add(roleProvider);
  }

  /**
   * Remove a role provider.
   *
   * @param roleProvider
   *          the role provider to remove
   */
  protected synchronized void removeRoleProvider(RoleProvider roleProvider) {
    logger.debug("Removing {} from the list of role providers", roleProvider);
    roleProviders.remove(roleProvider);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserDirectoryService#getUsers()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> getUsers() {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }

    // Find all users from the user providers
    Set<User> users = new HashSet<User>();
    for (UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;

      users.addAll(IteratorUtils.toList(userProvider.getUsers()));
    }
    return users.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleDirectoryService#getRoles()
   */
  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Role> getRoles() {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }
    Set<Role> roles = new HashSet<Role>();
    for (RoleProvider roleProvider : roleProviders) {
      String providerOrgId = roleProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      roles.addAll(IteratorUtils.toList(roleProvider.getRoles()));
    }
    return roles.iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.UserDirectoryService#loadUser(java.lang.String)
   */
  @Override
  public User loadUser(String userName) throws IllegalStateException {
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }

    Object user = cache.get(tuple(org.getId(), userName));
    if (user == nullToken) {
      logger.debug("Returning null user from Cache");
      return null;
    } else {
      logger.debug("Returning user " + userName + " from cache");
      return (User) user;
    }
  }

  /** Load a user of an organization. */
  private final Function<Tuple<String, String>, User> loadUser = new Function<Tuple<String, String>, User>() {
    @Override
    public User apply(Tuple<String, String> orgUser) {
      // Collect all of the roles known from each of the user providers for this user
      User user = null;
      for (UserProvider userProvider : userProviders) {
        String providerOrgId = userProvider.getOrganization();
        if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !orgUser.getA().equals(providerOrgId)) {
          continue;
        }
        User providerUser = userProvider.loadUser(orgUser.getB());
        if (providerUser == null) {
          continue;
        }
        if (user == null) {
          user = providerUser;
        } else {
          user = mergeUsers(user, providerUser);
        }
      }
      return user;
    }
  };

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
   */
  @Override
  public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException,
          org.springframework.dao.DataAccessException {
    User user = loadUser(userName);
    if (user == null) {
      throw new UsernameNotFoundException(userName);
    } else {
      Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
      for (Role role : user.getRoles()) {
        authorities.add(new SimpleGrantedAuthority(role.getName()));
      }

      // Add additional roles from role providers
      for (RoleProvider roleProvider : roleProviders) {
        List<Role> rolesForUser = roleProvider.getRolesForUser(userName);
        for (Role role : rolesForUser)
          authorities.add(new SimpleGrantedAuthority(role.getName()));
      }

      authorities.add(new SimpleGrantedAuthority(securityService.getOrganization().getAnonymousRole()));
      // need a non null password to instantiate org.springframework.security.core.userdetails.User
      // but CAS authenticated users have no password
      String password = user.getPassword() == null ? DEFAULT_PASSWORD : user.getPassword();
      return new org.springframework.security.core.userdetails.User(user.getUsername(), password, user.canLogin(),
              true, true, true, authorities);
    }
  }

  /**
   * Merges two representations of a user, as returned by two different user providers. The set or roles from the
   * provided users will be merged into one set.
   *
   * @param user1
   *          the first user to merge
   * @param user2
   *          the second user to merge
   * @return a user with a merged set of roles
   */
  protected User mergeUsers(User user1, User user2) {
    HashSet<JaxbRole> mergedRoles = new HashSet<JaxbRole>();
    for (Role role : user1.getRoles()) {
      mergedRoles.add(JaxbRole.fromRole(role));
    }
    for (Role role : user2.getRoles()) {
      mergedRoles.add(JaxbRole.fromRole(role));
    }
    JaxbOrganization organization = JaxbOrganization.fromOrganization(user1.getOrganization());
    String password = user1.getPassword() == null ? user2.getPassword() : user1.getPassword();
    return new JaxbUser(user1.getUsername(), password, organization, mergedRoles);
  }

  /**
   * Sets the security service
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<User> findUsers(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }

    // Find all users from the user providers
    HashSet<User> users = new HashSet<User>();
    for (UserProvider userProvider : userProviders) {
      String providerOrgId = userProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      users.addAll(IteratorUtils.toList(userProvider.findUsers(query, 0, 0)));
    }
    return offsetLimitCollection(offset, limit, users).iterator();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    Organization org = securityService.getOrganization();
    if (org == null) {
      throw new IllegalStateException("No organization is set");
    }

    // Find all roles from the role providers
    HashSet<Role> roles = new HashSet<Role>();
    for (RoleProvider roleProvider : roleProviders) {
      String providerOrgId = roleProvider.getOrganization();
      if (!ALL_ORGANIZATIONS.equals(providerOrgId) && !org.getId().equals(providerOrgId))
        continue;
      roles.addAll(IteratorUtils.toList(roleProvider.findRoles(query, 0, 0)));
    }
    return offsetLimitCollection(offset, limit, roles).iterator();
  }

  @Override
  public void invalidate(String userName) {
    Organization org = securityService.getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set");

    cache.remove(tuple(org.getId(), userName));
    logger.trace("Invalidated user {} from user directories", userName);
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

}
