package com.capestartproject.common.util.doc.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation type is used for annotating RESTful service(each java class). This annotation type needs to be kept
 * until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestService {

  /**
   * @return the name of the service.
   */
  String name();

  /**
   * @return a title of the service documentation.
   */
  String title();

  /**
   * @return an array of notes to add into the end of the documentation.
   */
  String[] notes();

  /**
   * @return an abstract section which is displayed at the top of the documentation.
   */
  String abstractText();

}
