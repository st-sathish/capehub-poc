package com.capestartproject.userdirectory;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.util.EqualsUtil;
import com.capestartproject.kernel.security.persistence.JpaOrganization;

/**
 * JPA-annotated role object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "ch_role", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "organization" }))
@NamedQueries({
        @NamedQuery(name = "Role.findByQuery", query = "select r from JpaRole r where r.organization.id=:org and UPPER(r.name) like :query or UPPER(r.description) like :query"),
        @NamedQuery(name = "Role.findByName", query = "Select r FROM JpaRole r where r.name = :name and r.organization.id = :org"),
        @NamedQuery(name = "Role.findAll", query = "Select r FROM JpaRole r where r.organization.id = :org") })
public final class JpaRole implements Role {
  @Id
  @Column(name = "id")
  @GeneratedValue
  private Long id;

  @Column(name = "name", length = 128)
  private String name;

  @OneToOne()
  @JoinColumn(name = "organization")
  private JpaOrganization organization;

  @Column(name = "description", nullable = true)
  private String description;

  /**
   * No-arg constructor needed by JPA
   */
  public JpaRole() {
  }

  /**
   * Constructs a role with the specified name and organization.
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   */
  public JpaRole(String name, JpaOrganization organization) {
    super();
    this.name = name;
    this.organization = organization;
  }

  /**
   * Constructs a role with the specified name, organization and description.
   *
   * @param name
   *          the name
   * @param organization
   *          the organization
   * @param description
   *          the description
   */
  public JpaRole(String name, JpaOrganization organization, String description) {
    this(name, organization);
    this.description = description;
  }

  /**
   * Gets the identifier.
   *
   * @return the identifier
   */
  public Long getId() {
    return id;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.common.security.api.Role#getName()
	 */
  @Override
  public String getName() {
    return name;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.common.security.api.Role#getDescription()
	 */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description
   *
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.common.security.api.Role#getOrganization()
	 */
  @Override
  public Organization getOrganization() {
    return organization;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return EqualsUtil.hash(name, organization);
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Role))
      return false;
    Role other = (Role) obj;
    return name.equals(other.getName()) && organization.equals(other.getOrganization());
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return new StringBuilder(name).append(":").append(organization).toString();
  }
}
