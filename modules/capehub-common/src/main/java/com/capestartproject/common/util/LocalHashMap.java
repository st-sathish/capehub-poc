package com.capestartproject.common.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

/**
 * See this JAXB bug for the full explanation: https://jaxb.dev.java.net/issues/show_bug.cgi?id=223
 */
public class LocalHashMap {

  /** The internal backing map */
  protected Map<String, String> map = new HashMap<String, String>();

  /** Returns the internal map storing the properties */
  public Map<String, String> getMap() {
    return map;
  }

  /** No-arg constructor needed by JAXB */
  public LocalHashMap() {
  }

  /**
   * Constructs this map from a properties list, expressed as a string:
   *
   * <code>
   * foo=bar
   * this=that
   * </code>
   *
   * @param in
   *          The properties list
   * @throws IOException
   *           if parsing the string fails
   */
  public LocalHashMap(String in) throws IOException {
    Properties properties = new Properties();
    properties.load(IOUtils.toInputStream(in, "UTF-8"));
    for (Entry<Object, Object> e : properties.entrySet()) {
      map.put((String) e.getKey(), (String) e.getValue());
    }
  }
}
