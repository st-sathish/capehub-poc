
package com.capehub.common.util.doc.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation type is used for annotating responses for RESTful query. This annotation type needs to be kept until
 * runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestResponse {

  /**
   * @return a HTTP response code, such as 200, 400 etc. Developer should use constants in <a
   *         href="http://download.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html"
   *         >javax.servlet.http.HttpServletResponse</a> instead of magic numbers.
   */
  int responseCode();

  /**
   * @return a description of the response.
   */
  String description();

}
