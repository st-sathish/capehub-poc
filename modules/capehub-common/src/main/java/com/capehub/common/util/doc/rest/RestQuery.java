

package com.capehub.common.util.doc.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation type is used for annotating RESTful query(each java method, instead of the class). This annotation
 * type needs to be kept until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RestQuery {

  /**
   * @return the name of the query.
   */
  String name();

  /**
   * @return a description of the query.
   */
  String description();

  /**
   * @return a description of what is returned.
   */
  String returnDescription();

  /**
   * @return a list of possible responses from this query.
   */
  RestResponse[] reponses();

  /**
   * @return a list of path parameters for this query.
   */
  RestParameter[] pathParameters() default { };

  /**
   * @return a list of query parameters for this query.
   */
  RestParameter[] restParameters() default { };

  /**
   * @return a body parameter for this query.
   */
  RestParameter bodyParameter() default @RestParameter(defaultValue = "", description = "", isRequired = false, name = "", type = RestParameter.Type.NO_PARAMETER);
}
