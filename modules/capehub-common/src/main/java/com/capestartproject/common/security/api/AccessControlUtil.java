package com.capestartproject.common.security.api;

import static com.capestartproject.common.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static com.capestartproject.common.util.EqualsUtil.bothNotNull;
import static com.capestartproject.common.util.EqualsUtil.eqListUnsorted;
import static com.capestartproject.common.util.data.Either.left;
import static com.capestartproject.common.util.data.Either.right;
import static com.capestartproject.common.util.data.Monadics.mlist;

import com.capestartproject.common.util.data.Either;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.data.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides common functions helpful in dealing with {@link AccessControlList}s.
 */
public final class AccessControlUtil {

  /** Disallow construction of this utility class */
  private AccessControlUtil() {
  }

  /**
   * Determines whether the {@link AccessControlList} permits a user to perform an action.
   *
   * There are three ways a user can be allowed to perform an action:
   * <ol>
   * <li>They have the superuser role</li>
   * <li>They have their local organization's admin role</li>
   * <li>They have a role listed in the series ACL, with write permission</li>
   * </ol>
   *
   * @param acl
   *          the {@link AccessControlList}
   * @param user
   *          the user
   * @param org
   *          the organization
   * @param action
   *          the action to perform
   * @return whether this action should be allowed
   * @throws IllegalArgumentException
   *           if any of the arguments are null
   */
  public static boolean isAuthorized(AccessControlList acl, User user, Organization org, String action) {
    if (action == null || user == null || acl == null)
      throw new IllegalArgumentException();

    // Check for the global and local admin role
    if (user.hasRole(GLOBAL_ADMIN_ROLE) || user.hasRole(org.getAdminRole()))
      return true;

    Set<Role> userRoles = user.getRoles();
    for (AccessControlEntry entry : acl.getEntries()) {
      if (!action.equals(entry.getAction()))
        continue;

      String aceRole = entry.getRole();
      for (Role role : userRoles) {
        if (!role.getName().equals(aceRole))
          continue;

        return entry.isAllow();
      }
    }
    return false;
  }

  /**
   * Constructor function for ACLs.
   *
   * @see #entry(String, String, boolean)
   * @see #entries(String, org.opencastproject.util.data.Tuple[])
   */
  public static AccessControlList acl(Either<AccessControlEntry, List<AccessControlEntry>>... entries) {
    // sequence entries
    final List<AccessControlEntry> seq = mlist(entries)
            .foldl(new ArrayList<AccessControlEntry>(),
                    new Function2<List<AccessControlEntry>, Either<AccessControlEntry, List<AccessControlEntry>>, List<AccessControlEntry>>() {
                      @Override
                      public List<AccessControlEntry> apply(List<AccessControlEntry> sum,
                              Either<AccessControlEntry, List<AccessControlEntry>> current) {
                        if (current.isLeft())
                          sum.add(current.left().value());
                        else
                          sum.addAll(current.right().value());
                        return sum;
                      }
                    });
    return new AccessControlList(seq);
  }

  /** Create a single access control entry. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entry(String role, String action, boolean allow) {
    return left(new AccessControlEntry(role, action, allow));
  }

  /** Create a list of access control entries for a given role. */
  public static Either<AccessControlEntry, List<AccessControlEntry>> entries(final String role,
          Tuple<String, Boolean>... actions) {
    final List<AccessControlEntry> entries = mlist(actions).map(
            new Function<Tuple<String, Boolean>, AccessControlEntry>() {
              @Override
              public AccessControlEntry apply(Tuple<String, Boolean> action) {
                return new AccessControlEntry(role, action.getA(), action.getB());
              }
            }).value();
    return right(entries);
  }

  /**
   * Define equality on AccessControlLists. Two AccessControlLists are considered equal if they contain the exact same
   * entries no matter in which order.
   * <p/>
   * This has not been implemented in terms of #equals and #hashCode because the list of entries is not immutable and
   * therefore not suitable to be put in a set.
   */
  public static boolean equals(AccessControlList a, AccessControlList b) {
    return bothNotNull(a, b) && eqListUnsorted(a.getEntries(), b.getEntries());
  }
}
