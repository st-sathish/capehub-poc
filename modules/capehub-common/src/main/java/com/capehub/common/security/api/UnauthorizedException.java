
package com.capehub.common.security.api;

/**
 * An exception that indicates that a subject attempted to take an action for which it was not authorized.
 */
public class UnauthorizedException extends Exception {

  /** The java.io.serialization uid */
  private static final long serialVersionUID = 7717178990322180202L;

  /**
   * Constructs an UnauthorizedException using the specified message.
   *
   * @param message
   *          the message describing the reason for this exception
   */
  public UnauthorizedException(String message) {
    super(message);
  }

  /**
   * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
   *
   * @param user
   *          the current user
   * @param action
   *          the action attempted
   */
  public UnauthorizedException(User user, String action) {
    super(formatMessage(user, action, null));
  }

  /**
   * Constructs an UnauthorizedException for the specified user's attempt to take a specified action.
   *
   * @param user
   *          the current user
   * @param action
   *          the action attempted
   * @param acl
   *          the access control list that prevented the action
   */
  public UnauthorizedException(User user, String action, AccessControlList acl) {
    super(formatMessage(user, action, acl));
  }

  private static String formatMessage(User user, String action, AccessControlList acl) {
    StringBuilder sb = new StringBuilder();
    sb.append(user.toString());
    sb.append(" can not take action ");
    sb.append("'");
    sb.append(action);
    sb.append("'");
    if (acl != null) {
      sb.append(" according to ");
      sb.append(acl);
    }
    return sb.toString();
  }
}
