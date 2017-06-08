package com.capestartproject.common.serviceregistry.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for service registration collections.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "services", namespace = "http://serviceregistry.capestartproject.com")
@XmlRootElement(name = "services", namespace = "http://serviceregistry.capestartproject.com")
public class JaxbServiceRegistrationList {
  /** A list of search items. */
  @XmlElement(name = "service")
  protected List<JaxbServiceRegistration> registrations = new ArrayList<JaxbServiceRegistration>();

  public JaxbServiceRegistrationList() {
  }

  public JaxbServiceRegistrationList(JaxbServiceRegistration registration) {
    this.registrations.add(registration);
  }

  public JaxbServiceRegistrationList(Collection<JaxbServiceRegistration> registrations) {
    for (JaxbServiceRegistration stat : registrations)
      this.registrations.add((JaxbServiceRegistration) stat);
  }

  /**
   * @return the registrations
   */
  public List<JaxbServiceRegistration> getRegistrations() {
    return registrations;
  }

  /**
   * @param registrations
   *          the registrations to set
   */
  public void setStats(List<JaxbServiceRegistration> registrations) {
    this.registrations = registrations;
  }

  public void add(ServiceRegistration registration) {
    if (registration instanceof JaxbServiceRegistration) {
      registrations.add((JaxbServiceRegistration) registration);
    } else {
      throw new IllegalArgumentException("Service registrations must be an instance of JaxbServiceRegistration");
    }
  }
}
