package com.capestartproject.userdirectory;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.security.api.Group;
import com.capestartproject.common.security.api.JaxbOrganization;
import com.capestartproject.common.security.api.JaxbRole;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.util.EqualsUtil;

/**
 * A simple user model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group", namespace = "http://com.capestart.security")
@XmlRootElement(name = "group", namespace = "http://com.capestart.security")
public final class JaxbGroup implements Group {

  @XmlElement(name = "id")
  protected String groupId;

  @XmlElement(name = "organization")
  protected JaxbOrganization organization;

  @XmlElement(name = "name")
  protected String name;

  @XmlElement(name = "description")
  protected String description;

  @XmlElement(name = "role")
  protected String role;

  @XmlElement(name = "member")
  @XmlElementWrapper(name = "members")
  protected Set<String> members;

  @XmlElement(name = "role")
  @XmlElementWrapper(name = "roles")
  protected Set<JaxbRole> roles;

  /**
   * No-arg constructor needed by JAXB
   */
  public JaxbGroup() {
  }

  /**
   * Constructs a group with the specified groupId, name, description and group role.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description) {
    super();
    this.groupId = groupId;
    this.organization = organization;
    this.name = name;
    this.description = description;
    this.role = ROLE_PREFIX + groupId.toUpperCase();
    this.roles = new HashSet<JaxbRole>();
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description, Set<JaxbRole> roles) {
    this(groupId, organization, name, description);
    this.roles = roles;
  }

  /**
   * Constructs a group with the specified groupId, name, description, group role and roles.
   *
   * @param groupId
   *          the group id
   * @param organization
   *          the organization
   * @param name
   *          the name
   * @param description
   *          the description
   * @param roles
   *          the additional group roles
   * @param members
   *          the group members
   */
  public JaxbGroup(String groupId, JaxbOrganization organization, String name, String description, Set<JaxbRole> roles,
          Set<String> members) {
    this(groupId, organization, name, description, roles);
    this.members = members;
  }

  public static JaxbGroup fromGroup(Group group) {
    JaxbOrganization organization = JaxbOrganization.fromOrganization(group.getOrganization());
    Set<JaxbRole> roles = new HashSet<JaxbRole>();
    for (Role role : group.getRoles()) {
      if (role instanceof JaxbRole)
        roles.add((JaxbRole) role);
      roles.add(JaxbRole.fromRole(role));
    }
    return new JaxbGroup(group.getGroupId(), organization, group.getName(), group.getDescription(), roles,
            group.getMembers());
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getGroupId()
	 */
  @Override
  public String getGroupId() {
    return groupId;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getName()
	 */
  @Override
  public String getName() {
    return name;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getOrganization()
	 */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getDescription()
	 */
  @Override
  public String getDescription() {
    return description;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getRole()
	 */
  @Override
  public String getRole() {
    return role;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getMembers()
	 */
  @Override
  public Set<String> getMembers() {
    return members;
  }

  	/**
	 * @see com.capestartproject.common.security.api.Group#getRoles()
	 */
  @Override
  public Set<Role> getRoles() {
    return new HashSet<Role>(roles);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(groupId, organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Group))
      return false;
    Group other = (Group) obj;
    return groupId.equals(other.getGroupId()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(groupId).append(":").append(organization).toString();
  }

}
