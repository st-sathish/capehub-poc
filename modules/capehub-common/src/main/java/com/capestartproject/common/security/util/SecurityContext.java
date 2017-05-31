package com.capestartproject.common.security.util;

import static com.capestartproject.common.util.EqualsUtil.ne;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.util.data.Function0;

/**
 * This class handles all the boilerplate of setting up and tearing down a security context. It also makes it possible
 * to pass around contexts so that clients need not deal with services, users, passwords etc.
 */
public class SecurityContext {
  private final SecurityService sec;
  private final User user;
  private final Organization org;

  public SecurityContext(SecurityService sec, Organization org, User user) {
    if (ne(org, user.getOrganization())) {
      throw new IllegalArgumentException("User is not a member of organization " + org.getId());
    }
    this.sec = sec;
    this.user = user;
    this.org = org;
  }

  /** Run function <code>f</code> within the context. */
  public <A> A runInContext(Function0<A> f) {
    final Organization prevOrg = sec.getOrganization();
    // workaround: if no organization is bound to the current thread sec.getUser() will throw a NPE
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
}
