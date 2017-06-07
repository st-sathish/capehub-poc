package com.capestartproject.kernel.filter.proxy;

import static com.capestartproject.kernel.filter.proxy.TransparentProxyFilter.X_FORWARDED_FOR;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This wrapper is used to return the client's original IP address even if case of a proxy being the middle man.
 */
class TransparentProxyRequestWrapper extends HttpServletRequestWrapper {

  /** The original IP */
  private final String originalIP;

  /**
   * Wraps the request.
   *
   * @param request
   *          the original request
   */
  TransparentProxyRequestWrapper(HttpServletRequest request) {
    super(request);
    originalIP = request.getHeader(X_FORWARDED_FOR);
  }

  /**
   * Overwrites the original behavior by returning the address transported in the <code>X-FORWARDED-FOR</code> request
   * header instead of the proxy's ip.
   *
   * @see javax.servlet.ServletRequestWrapper#getRemoteAddr()
   */
  @Override
  public String getRemoteAddr() {
    return originalIP;
  }

  /**
   * Overwrites the original behavior by returning the address transported in the <code>X-FORWARDED-FOR</code> request
   * header instead of the proxy's hostname.
   *
   * @see javax.servlet.ServletRequestWrapper#getRemoteHost()
   */
  @Override
  public String getRemoteHost() {
    return originalIP;
  }

}
