package com.capestartproject.kernel.filter.https;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * This filter is wrapping <code>HttpServletRequest</code>s in such a way that they feature the https scheme.
 */
public class HttpsFilter implements Filter {

  /** Request header that is set when behind an SSL proxy */
  public static final String X_FORWARDED_SSL = "X-Forwarded-SSL";

  /** Value of the X-Forwarded-SSL header that activates request wrapping */
  public static final String X_FORWARDED_SSL_VALUE = "on";

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
   *      javax.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
          ServletException {
    HttpServletRequest httpReqquest = (HttpServletRequest) request;

    // Check if the forwarded SSL header is set
    if (X_FORWARDED_SSL_VALUE.equalsIgnoreCase(httpReqquest.getHeader(X_FORWARDED_SSL))) {
      httpReqquest = new HttpsRequestWrapper(httpReqquest);
    }
    chain.doFilter(httpReqquest, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

}
