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
 * A wrapper for user collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "users", namespace = "http://com.capestartproject.security")
@XmlRootElement(name = "users", namespace = "http://com.capestartproject.security")
public class JaxbUserList {

  /** A list of users. */
  @XmlElement(name = "user")
  protected List<JaxbUser> users = new ArrayList<JaxbUser>();

  public JaxbUserList() {
  }

  public JaxbUserList(JaxbUser user) {
    users.add(user);
  }

  public JaxbUserList(Collection<JaxbUser> users) {
    for (JaxbUser user : users)
      this.users.add(user);
  }

  /**
   * @return the users
   */
  public List<JaxbUser> getUsers() {
    return users;
  }

  /**
   * @param users
   *          the users to set
   */
  public void setUsers(List<JaxbUser> users) {
    this.users = users;
  }

  public void add(User user) {
    if (user instanceof JaxbUser) {
      users.add((JaxbUser) user);
    } else {
      users.add(JaxbUser.fromUser(user));
    }
  }

}
