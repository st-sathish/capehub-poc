package com.capestartproject.kernel.security;

import com.capestartproject.common.security.api.SecurityConstants;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;

/**
 * This is a special implementation of the anonymous filter that prevents the filter from going with the anonymous user
 * if there is the potential for a real authentication coming later on.
 * <p>
 * The filter is needed in order to allow for security configurations where a url is open to the public but at the same
 * needs to support authorization to Matterhorn's {@link org.opencastproject.security.api.TrustedHttpClient}.
 */
public class TrustedAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

  /**
   * @see org.springframework.security.web.authentication.AnonymousAuthenticationFilter#applyAnonymousForThisRequest(javax
   *      .servlet.http.HttpServletRequest)
   */
  @Override
  @Deprecated
  protected boolean applyAnonymousForThisRequest(HttpServletRequest request) {
    if (StringUtils.isNotBlank(request.getHeader(SecurityConstants.AUTHORIZATION_HEADER))) {
      return false;
    }
    return true;
  }

}
