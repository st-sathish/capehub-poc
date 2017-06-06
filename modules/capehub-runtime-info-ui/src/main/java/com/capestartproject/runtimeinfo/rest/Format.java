package com.capestartproject.runtimeinfo.rest;

import com.capestartproject.common.util.doc.DocData;

/**
 * Represents one possible output format for a REST endpoint
 */
@Deprecated
public final class Format {
  public static final String JSON = "json";
  public static final String XML = "xml";
  public static final String JSON_URL = "http://www.json.org/";
  public static final String XML_URL = "http://www.w3.org/XML/";

  /**
   * @return the standard format object for use with JSON
   */
  public static Format json() {
    return new Format(JSON, null, JSON_URL);
  }

  /**
   * @return the standard format object for use with XML
   */
  public static Format xml() {
    return new Format(XML, null, XML_URL);
  }

  /**
   * @param desc
   *          [optional] description to display with the format
   * @return the standard format object for use with JSON with a description
   */
  public static Format json(String desc) {
    return new Format(JSON, desc, JSON_URL);
  }

  /**
   * @param desc
   *          [optional] description to display with the format
   * @return the standard format object for use with XML with a description
   */
  public static Format xml(String desc) {
    return new Format(XML, desc, XML_URL);
  }

  private String name; // unique key
  private String description;
  private String url;

  /**
   * @param name
   *          the format name (e.g. json)
   * @param description
   *          [optional] a description related to this format
   * @param url
   *          [optional] the url to info about this format OR sample data
   */
  public Format(String name, String description, String url) {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("name must not be null and must be alphanumeric");
    }
    this.name = name;
    this.description = description;
    this.url = url;
  }

  @Override
  public String toString() {
    return name + ":(" + url + ")";
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }

}
