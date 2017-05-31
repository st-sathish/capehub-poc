package com.capestartproject.common.security.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for role collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "roles", namespace = "http://com.capestartproject.security")
@XmlRootElement(name = "roles", namespace = "http://com.capestartproject.security")
public class JaxbRoleList {

  /** A list of roles. */
  @XmlElement(name = "role")
  protected List<JaxbRole> roles = new ArrayList<JaxbRole>();

  public JaxbRoleList() {
  }

  public JaxbRoleList(JaxbRole role) {
    roles.add(role);
  }

  public JaxbRoleList(Collection<JaxbRole> roles) {
    for (JaxbRole role : roles)
      this.roles.add(role);
  }

  /**
   * @return the roles
   */
  public List<JaxbRole> getRoles() {
    return roles;
  }

  /**
   * @param roles
   *          the roles to set
   */
  public void setRoles(List<JaxbRole> roles) {
    this.roles = roles;
  }

  public void add(Role role) {
    if (role instanceof JaxbRole) {
      roles.add((JaxbRole) role);
    } else {
      roles.add(JaxbRole.fromRole(role));
    }
  }

}
