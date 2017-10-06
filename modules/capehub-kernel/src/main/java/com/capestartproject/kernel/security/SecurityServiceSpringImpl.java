package com.capestartproject.kernel.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.capestartproject.common.security.api.JaxbOrganization;
import com.capestartproject.common.security.api.JaxbRole;
import com.capestartproject.common.security.api.JaxbUser;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.util.SecurityUtil;

/**
 * A Spring Security implementation of {@link SecurityService}.
 */
public class SecurityServiceSpringImpl implements SecurityService {

  /** Holds delegates users for new threads that have been spawned from authenticated threads */
  private static final ThreadLocal<User> delegatedUserHolder = new ThreadLocal<User>();

  /** Holds organization responsible for the current thread */
  private static final ThreadLocal<Organization> organization = new ThreadLocal<Organization>();

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.security.api.SecurityService#getOrganization()
	 */
  @Override
  public Organization getOrganization() {
    return SecurityServiceSpringImpl.organization.get();
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.security.api.SecurityService#setOrganization(Organization)
	 */
  @Override
  public void setOrganization(Organization organization) {
    SecurityServiceSpringImpl.organization.set(organization);
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.security.api.SecurityService#getUser()
	 */
  @Override
  public User getUser() throws IllegalStateException {
    Organization org = getOrganization();
    if (org == null)
      throw new IllegalStateException("No organization is set in security context");

    User delegatedUser = delegatedUserHolder.get();
    if (delegatedUser != null) {
      return delegatedUser;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(org);
    if (auth == null) {
      return SecurityUtil.createAnonymousUser(jaxbOrganization);
    } else {
      Object principal = auth.getPrincipal();
      if (principal == null) {
        return SecurityUtil.createAnonymousUser(jaxbOrganization);
      }
      if (principal instanceof UserDetails) {
        UserDetails userDetails = (UserDetails) principal;

        Set<JaxbRole> roles = new HashSet<JaxbRole>();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities != null && authorities.size() > 0) {
          for (GrantedAuthority ga : authorities) {
            roles.add(new JaxbRole(ga.getAuthority(), jaxbOrganization));
          }
        }
        return new JaxbUser(userDetails.getUsername(), jaxbOrganization, roles);
      } else {
        return SecurityUtil.createAnonymousUser(jaxbOrganization);
      }
    }
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.security.api.SecurityService#setUser(User)
	 */
  @Override
  public void setUser(User user) {
    delegatedUserHolder.set(user);
  }

}
