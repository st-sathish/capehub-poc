package com.capestartproject.common.emppackage.identifier;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Simple and straightforward implementation of the {@link Id} interface.
 */
@XmlType
@XmlAccessorType(XmlAccessType.NONE)
public class IdImpl implements Id {

  /** The identifier */
  @XmlValue
  protected String id = null;

  /**
   * Needed for JAXB serialization
   */
  public IdImpl() {
  }

  /**
   * Creates a new serial identifier as created by {@link SerialIdBuilder}.
   *
   * @param id
   *          the identifier
   */
  public IdImpl(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.identifier.Id#compact()
   */
  public String compact() {
    return id.replaceAll("/", "-").replaceAll("\\\\", "-");
  }

  @Override
  public String toString() {
    return id;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (o instanceof IdImpl) {
      IdImpl other = (IdImpl) o;
      return id != null && other.id != null && id.equals(other.id);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
