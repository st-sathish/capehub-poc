package com.capestartproject.kernel.filter.https;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This wrapper is used to pretend the <code>HTTPS</code> scheme.
 */
public class HttpsRequestWrapper extends HttpServletRequestWrapper {

  /** The original URL */
  private String originalURL;

  /**
   * Wraps the request to enforce https as the request scheme.
   *
   * @param request
   *          the original request
   */
  public HttpsRequestWrapper(HttpServletRequest request) {
    super(request);
    StringBuffer url = super.getRequestURL();
    int protocolIndex = url.indexOf("://");
    originalURL = "https" + url.substring(protocolIndex);
    originalURL = url.toString();
  }

  /**
   * Overwrites the original request's scheme to return <code>https</code>.
   *
   * @return always returns <code>https</code>
   */
  public String getScheme() {
    return "https";
  }

  /**
   * Indicates that this is a secured request.
   *
   * @return always returns <code>true</code>
   */
  public boolean isSecure() {
    return true;
  }

  /**
   * Returns the request url with a "fixed" scheme of <code>https</code>.
   *
   * @return the original url featuring the <code>https</code> scheme
   */
  public StringBuffer getRequestURL() {
    return new StringBuffer(this.originalURL);
  }
}
