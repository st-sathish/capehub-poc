package com.capestartproject.common.security.util;

import static com.capestartproject.common.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static com.capestartproject.common.security.api.SecurityConstants.GLOBAL_ANONYMOUS_USERNAME;
import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.Option.option;
import static com.capestartproject.common.util.data.Option.some;
import static com.capestartproject.common.util.data.Tuple.tuple;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;

import com.capestartproject.common.security.api.JaxbOrganization;
import com.capestartproject.common.security.api.JaxbRole;
import com.capestartproject.common.security.api.JaxbUser;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.data.Tuple;

/** Capehub security helpers. */
public final class SecurityUtil {
  private SecurityUtil() {
  }

  /** The name of the key used to store the name of the system user in the global config. */
  public static final String PROPERTY_KEY_SYS_USER = "com.capehub.security.digest.user";

  /**
   * Run function <code>f</code> in the context described by the given organization and user.
   *
   * @return the function's outcome.
   */
  public static <A> A runAs(SecurityService sec, Organization org, User user, Function0<A> f) {
    final Organization prevOrg = sec.getOrganization();
    final User prevUser = prevOrg != null ? sec.getUser() : null;
    sec.setOrganization(org);
    sec.setUser(user);
    try {
      return f.apply();
    } finally {
      sec.setOrganization(prevOrg);
      sec.setUser(prevUser);
    }
  }

  /**
   * Create a system user for the given organization with global and organization local admin role. Get the
   * <code>systemUserName</code> from the global config where it is stored under {@link #PROPERTY_KEY_SYS_USER}. In an
   * OSGi environment this is typically done calling
   * <code>componentContext.getBundleContext().getProperty(PROPERTY_KEY_SYS_USER)</code>.
   *
   * @see #createSystemUser(org.osgi.service.component.ComponentContext, org.opencastproject.security.api.Organization)
   */
  public static User createSystemUser(String systemUserName, Organization org) {
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
    return new JaxbUser(systemUserName, jaxbOrganization, new JaxbRole(GLOBAL_ADMIN_ROLE, jaxbOrganization),
            new JaxbRole(org.getAdminRole(), jaxbOrganization));
  }

  /**
   * Create the global anonymous user with the given organization.
   *
   * @param org
   *          the organization
   * @return the global anonymous user
   */
  public static User createAnonymousUser(Organization org) {
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
    return new JaxbUser(GLOBAL_ANONYMOUS_USERNAME, jaxbOrganization, new JaxbRole(jaxbOrganization.getAnonymousRole(),
            jaxbOrganization));
  }

  /**
   * Create a system user for the given organization with global admin role. The system user name is fetched from the
   * global OSGi config.
   *
   * @see #createSystemUser(String, org.opencastproject.security.api.Organization)
   */
  public static User createSystemUser(ComponentContext cc, Organization org) {
    final String systemUserName = cc.getBundleContext().getProperty(PROPERTY_KEY_SYS_USER);
    return createSystemUser(systemUserName, org);
  }

  /** Get the organization <code>orgId</code>. */
  public static Option<Organization> getOrganization(OrganizationDirectoryService orgDir, String orgId) {
    try {
      return some(orgDir.getOrganization(orgId));
    } catch (NotFoundException e) {
      return none();
    }
  }

  /** Get a user of a certain organization by its ID. */
  public static Option<User> getUserOfOrganization(SecurityService sec, OrganizationDirectoryService orgDir,
          String orgId, UserDirectoryService userDir, String userId) {
    final Organization prevOrg = sec.getOrganization();
    try {
      final Organization org = orgDir.getOrganization(orgId);
      sec.setOrganization(org);
      return option(userDir.loadUser(userId));
    } catch (NotFoundException e) {
      return none();
    } finally {
      sec.setOrganization(prevOrg);
    }
  }

  /**
   * Get a user and an organization. Only returns something if both elements can be determined.
   */
  public static Option<Tuple<User, Organization>> getUserAndOrganization(SecurityService sec,
          OrganizationDirectoryService orgDir, String orgId, UserDirectoryService userDir, String userId) {
    final Organization prevOrg = sec.getOrganization();
    try {
      final Organization org = orgDir.getOrganization(orgId);
      sec.setOrganization(org);
      return option(userDir.loadUser(userId)).fmap(new Function<User, Tuple<User, Organization>>() {
        @Override
        public Tuple<User, Organization> apply(User user) {
          return tuple(user, org);
        }
      });
    } catch (NotFoundException e) {
      return none();
    } finally {
      sec.setOrganization(prevOrg);
    }
  }

  /** Extract hostname and port number from a URL. */
  public static Tuple<String, Integer> hostAndPort(URL url) {
    return tuple(StringUtils.strip(url.getHost(), "/"), url.getPort());
  }
}
