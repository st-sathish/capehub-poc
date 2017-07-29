package com.capestartproject.userdirectory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.security.api.Group;

/**
 * A wrapper for group collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "groups", namespace = "http://com.capestart.security")
@XmlRootElement(name = "groups", namespace = "http://com.capestart.security")
public class JaxbGroupList {

  /** A list of roles. */
  @XmlElement(name = "group")
  protected List<JaxbGroup> groups = new ArrayList<JaxbGroup>();

  public JaxbGroupList() {
  }

  public JaxbGroupList(JaxbGroup group) {
    groups.add(group);
  }

  public JaxbGroupList(Collection<JaxbGroup> groups) {
    for (JaxbGroup group : groups)
      groups.add(group);
  }

  /**
   * @return the roles
   */
  public List<JaxbGroup> getRoles() {
    return groups;
  }

  /**
   * @param roles
   *          the roles to set
   */
  public void setRoles(List<JaxbGroup> roles) {
    this.groups = roles;
  }

  public void add(Group group) {
    if (group instanceof JaxbGroup) {
      groups.add((JaxbGroup) group);
    } else {
      groups.add(JaxbGroup.fromGroup(group));
    }
  }

}
