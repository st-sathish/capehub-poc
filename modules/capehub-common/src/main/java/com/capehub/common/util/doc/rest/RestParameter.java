package com.capehub.common.util.doc.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation type is used for annotating parameters for RESTful query. This annotation type needs to be kept until
 * runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestParameter {

  enum Type {
    NO_PARAMETER, // This is a special type to represent that there is no parameter. We need this because java
                  // annotation cannot be set to null.
    BOOLEAN, FILE, STRING, TEXT, INTEGER, FLOAT
  };

  /**
   * @return a name of the parameter.
   */
  String name();

  /**
   * @return a description of the parameter.
   */
  String description();

  /**
   * @return a default value of the parameter.
   */
  String defaultValue() default "";

  /**
   * @return a RestParameter.Type enum specifying the type of the parameter.
   */
  Type type();

  /**
   * @return the {@link javax.xml.bind.annotation.XmlType} or {@link javax.xml.bind.annotation.XmlRootElement} annotated
   *         class that models this parameter.
   */
  Class<?> jaxbClass() default Object.class;

  /**
   * @return a boolean indicating whether this parameter is required.
   */
  boolean isRequired();
}
