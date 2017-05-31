package com.capestartproject.common.security.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A JAXB-annotated list of organizations.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "organizations", namespace = "http://com.capestartproject.security")
@XmlRootElement(name = "organizations", namespace = "http://com.capestartproject.security")
public class OrganizationList {

  /** The list of organizations */
  @XmlElement(name = "organization")
  protected List<Organization> organizations;

  /**
   * No arg constructor needed by JAXB
   */
  public OrganizationList() {
  }

  /**
   * Constructs a new OrganizationList wrapper from a list of organizations.
   *
   * @param organizations
   *          the list or organizations
   */
  public OrganizationList(List<Organization> organizations) {
    this.organizations = organizations;
  }

  /**
   * @return the organizations
   */
  public List<Organization> getOrganizations() {
    return organizations;
  }

  /**
   * @param organizations
   *          the organizations to set
   */
  public void setOrganizations(List<Organization> organizations) {
    this.organizations = organizations;
  }
}
