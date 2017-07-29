package com.capestartproject.userdirectory;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.util.EqualsUtil;
import com.capestartproject.kernel.security.persistence.JpaOrganization;

/**
 * JPA-annotated user object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "ch_user", uniqueConstraints = { @UniqueConstraint(columnNames = { "username", "organization" }) })
@NamedQueries({
        @NamedQuery(name = "User.findByQuery", query = "select u from JpaUser u where UPPER(u.username) like :query and u.organization.id = :org"),
        @NamedQuery(name = "User.findByUsername", query = "select u from JpaUser u where u.username=:u and u.organization.id = :org"),
        @NamedQuery(name = "User.findAll", query = "select u from JpaUser u where u.organization.id = :org") })
public class JpaUser implements User {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "username", length = 128)
  protected String username;

  @Lob
  @Column(name = "password", length = 65535)
  protected String password;

  @OneToOne()
  @JoinColumn(name = "organization")
  protected JpaOrganization organization;

  @ManyToMany(cascade = { CascadeType.MERGE }, fetch = FetchType.EAGER)
	@JoinTable(name = "ch_user_role",
             joinColumns = {@JoinColumn(name = "user_id")},
             inverseJoinColumns = {@JoinColumn(name = "role_id")},
             uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "role_id"})})
  protected Set<JpaRole> roles;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaUser() {
  }

  /**
   * Constructs a user with the specified username and password.
   *
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   */
  public JpaUser(String username, String password, JpaOrganization organization) {
    super();
    this.username = username;
    this.password = password;
    this.organization = organization;
    this.roles = new HashSet<JpaRole>();
  }

  /**
   * Constructs a user with the specified username, password, and roles.
   *
   * @param username
   *          the username
   * @param password
   *          the password
   * @param organization
   *          the organization
   * @param roles
   *          the roles
   */
  public JpaUser(String username, String password, JpaOrganization organization, Set<JpaRole> roles) {
    this(username, password, organization);
    for (Role role : roles) {
      if (role.getOrganization() == null || !organization.getId().equals(role.getOrganization().getId()))
        throw new IllegalArgumentException("Role " + role + " is not from the same organization!");
    }
    this.roles = roles;
  }

  /**
   * Gets this user's clear text password.
   *
   * @return the user account's password
   */
  public String getPassword() {
    return password;
  }

  	/**
	 * @see com.capestartproject.common.security.api.User#canLogin()
	 */
  @Override
  public boolean canLogin() {
    return true;
  }

  	/**
	 * @see com.capestartproject.common.security.api.User#getUsername()
	 */
  @Override
  public String getUsername() {
    return username;
  }

  	/**
	 * @see com.capestartproject.common.security.api.User#hasRole(String)
	 */
  @Override
  public boolean hasRole(String roleName) {
    for (Role role : roles) {
      if (role.getName().equals(roleName))
        return true;
    }
    return false;
  }

  	/**
	 * @see com.capestartproject.security.api.User#getOrganization()
	 */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  	/**
	 * @see com.capestartproject.common.security.api.User#getRoles()
	 */
  @Override
  public Set<Role> getRoles() {
    return new HashSet<Role>(roles);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof User))
      return false;
    User other = (User) obj;
    return username.equals(other.getUsername()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(username, organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(username).append(":").append(organization).toString();
  }
}
