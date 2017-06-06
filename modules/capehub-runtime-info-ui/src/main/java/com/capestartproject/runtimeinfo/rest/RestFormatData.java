package com.capestartproject.runtimeinfo.rest;

import javax.ws.rs.core.MediaType;

/**
 * Represents an output format for a REST endpoint.
 */
public final class RestFormatData {
  /**
   * Default URL for the JSON format.
   */
  public static final String JSON_URL = "http://www.json.org/";

  /**
   * Default URL for the XML format.
   */
  public static final String XML_URL = "http://www.w3.org/XML/";

  /**
   * Name of the format, the value should be a constant from <a
   * href="http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html"
   * >javax.ws.rs.core.MediaType</a> or ExtendedMediaType (org.opencastproject.util.doc.rest.ExtendedMediaType).
   */
  private String name;

  /**
   * Description of the format. Currently no format has default description yet.
   */
  private String description;

  /**
   * URL to a page providing more information of the format. Currently only JSON and XML have a default URL.
   */
  private String url;

  /**
   * Constructor that accepts a format name and finds the format's corresponding default URL and description. Currently
   * only JSON and XML have a default URL.
   *
   * @param name
   *          the format name, the value should be a constant from <a
   *          href="http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html"
   *          >javax.ws.rs.core.MediaType</a> or ExtendedMediaType
   *          (org.opencastproject.util.doc.rest.ExtendedMediaType).
   *
   * @throws IllegalArgumentException
   *           when name is null
   */
  public RestFormatData(String name) throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("Name must not be null.");
    }
    this.name = name;
    if ((name.equalsIgnoreCase(MediaType.TEXT_XML)) || (name.equalsIgnoreCase(MediaType.APPLICATION_XML))) {
      url = XML_URL;
    } else if (name.equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
      url = JSON_URL;
    }
  }

  /**
   * Return a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return name + ":(" + url + ")";
  }

  /**
   * Return the name of this format.
   *
   * @return the name of this format
   */
  public String getName() {
    return name;
  }

  /**
   * Return the default description of this format.
   *
   * @return the default description of this format.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Return the default URL of this format.
   *
   * @return the default URL of this format.
   */
  public String getUrl() {
    return url;
  }

}
