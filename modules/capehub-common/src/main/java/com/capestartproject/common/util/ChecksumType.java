package com.capestartproject.common.util;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checksum type represents the method used to generate a checksum.
 */
@XmlJavaTypeAdapter(ChecksumType.Adapter.class)
@XmlType(name = "checksumtype", namespace = "http://employeepackage.capestartproject.com")
public final class ChecksumType implements Serializable {

  private static final Logger logger = LoggerFactory.getLogger(ChecksumType.class);

  /** Serial version uid */
  private static final long serialVersionUID = 1L;

  /** List of all known checksum types */
  private static final Map<String, ChecksumType> TYPES = new HashMap<String, ChecksumType>();

  /** Default type md5 */
  public static final ChecksumType DEFAULT_TYPE = new ChecksumType("md5");

  /** The type name */
  @XmlValue
  protected String type = null;

  /** Needed by JAXB */
  public ChecksumType() {
  }

  /**
   * Creates a new checksum type with the given type name.
   *
   * @param type
   *          the type name
   */
  protected ChecksumType(String type) {
    this.type = type;
    TYPES.put(type, this);
  }

  /**
   * Returns the checksum value.
   *
   * @return the value
   */
  public String getName() {
    return type;
  }

  /**
   * Returns a checksum type for the given string. <code>Type</code> is considered to be the name of a checksum type.
   *
   * @param type
   *          the type name
   * @return the checksum type
   * @throws NoSuchAlgorithmException
   *           if the digest is not supported by the java environment
   */
  public static ChecksumType fromString(String type) throws NoSuchAlgorithmException {
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' is null");
    type = type.toLowerCase();
    ChecksumType checksumType = TYPES.get(type);
    if (checksumType == null) {
      MessageDigest.getInstance(type);
      checksumType = new ChecksumType(type);
      TYPES.put(type, checksumType);
    }
    return checksumType;
  }

  /**
   * Returns the type of the checksum gathered from the provided value.
   *
   * @param value
   *          the checksum value
   * @return the type
   */
  public static ChecksumType fromValue(String value) {
    // TODO: Implement
    throw new IllegalStateException("Not yet implemented");
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ChecksumType) {
      return type.equals(((ChecksumType) obj).type);
    }
    return false;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public String toString() {
    return type;
  }

  static class Adapter extends XmlAdapter<String, ChecksumType> {
    @Override
    public String marshal(ChecksumType checksumType) throws Exception {
      return checksumType.type;
    }

    @Override
    public ChecksumType unmarshal(String str) throws Exception {
      try {
        return ChecksumType.fromString(str);
      } catch (NoSuchAlgorithmException e) {
        logger.warn(e.getMessage(), e);
        throw e;
      }
    }
  }
}
