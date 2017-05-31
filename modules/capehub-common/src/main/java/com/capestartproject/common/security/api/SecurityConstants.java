
package com.capestartproject.common.security.api;

/**
 * Common security constant definitions.
 */
public interface SecurityConstants {

  /** Header name for the digest authorization */
  String AUTHORIZATION_HEADER = "X-Capehub-Authorization";

  /** Header name for the current organization */
  String ORGANIZATION_HEADER = "X-Capehub-Organization";

  /** Header name for the current user */
  String USER_HEADER = "X-Capehub-User";

  /** Name of the Capehub admin role */
  String GLOBAL_ADMIN_ROLE = "CAPEHUB_ADMINISTRATOR";

  /** Name of the Matterhorn anonymous role */
  String GLOBAL_ANONYMOUS_USERNAME = "anonymous";

}
