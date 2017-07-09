
package com.capestartproject.kernel.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;

/**
 * An {@link AuthenticationEntryPoint} that delegates to a default implementation unless a "X-Requested-Auth" header
 * with a value of "Digest".
 */
public class DelegatingAuthenticationEntryPoint implements AuthenticationEntryPoint {
  public static final String REQUESTED_AUTH_HEADER = "X-Requested-Auth";
  public static final String DIGEST_AUTH = "Digest";
  public static final String OAUTH_SIGNATURE = "oauth_signature";
  public static final String INITIAL_REQUEST_PATH = "initial_request_path";

  protected AuthenticationEntryPoint userEntryPoint;
  protected DigestAuthenticationEntryPoint digestAuthenticationEntryPoint;

  /**
   * {@inheritDoc}
   *
   * @see org.springframework.security.web.AuthenticationEntryPoint#commence(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse, org.springframework.security.core.AuthenticationException)
   */
  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
          throws IOException, ServletException {
    if (DIGEST_AUTH.equals(request.getHeader(REQUESTED_AUTH_HEADER))) {
      digestAuthenticationEntryPoint.commence(request, response, authException);
    } else {
      // if the user attempted to access a url other than /, store this in the session so we can forward the user there
      // after a successful login
      String requestUri = request.getRequestURI();
      String queryString = request.getQueryString();
      if (requestUri != null && !requestUri.isEmpty() & !"/".equals(requestUri)) {
        if (queryString == null) {
          request.getSession().setAttribute(INITIAL_REQUEST_PATH, requestUri);
        } else {
          request.getSession().setAttribute(INITIAL_REQUEST_PATH, requestUri + "?" + queryString);
        }
      } else {
        request.getSession().removeAttribute(INITIAL_REQUEST_PATH);
      }
      userEntryPoint.commence(request, response, authException);
    }
  }

  /**
   * @param userEntryPoint
   *          the user entrypoint to set
   */
  public void setUserEntryPoint(AuthenticationEntryPoint userEntryPoint) {
    this.userEntryPoint = userEntryPoint;
  }

  /**
   * @param digestAuthenticationEntryPoint
   *          the digest auth entrypoint to set
   */
  public void setDigestAuthenticationEntryPoint(DigestAuthenticationEntryPoint digestAuthenticationEntryPoint) {
    this.digestAuthenticationEntryPoint = digestAuthenticationEntryPoint;
  }
}
