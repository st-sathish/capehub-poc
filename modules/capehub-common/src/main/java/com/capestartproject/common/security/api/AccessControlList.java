
package com.capestartproject.common.security.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A list of {@link AccessControlEntry}s.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acl", namespace = "http://com.capehub.security")
@XmlRootElement(name = "acl", namespace = "http://com.capehub.security")
public final class AccessControlList {

  /** The list of access control entries */
  @XmlElement(name = "ace")
  private List<AccessControlEntry> entries;

  /**
   * No-arg constructor needed by JAXB
   */
  public AccessControlList() {
    this.entries = new ArrayList<AccessControlEntry>();
  }

  public AccessControlList(AccessControlEntry... entries) {
    this.entries = new ArrayList<AccessControlEntry>(Arrays.asList(entries));
  }

  public AccessControlList(List<AccessControlEntry> entries) {
    this.entries = new ArrayList<AccessControlEntry>(entries);
  }

  /**
   * @return the entries
   */
  public List<AccessControlEntry> getEntries() {
    return entries;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return entries.toString();
  }

}
