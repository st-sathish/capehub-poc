package com.capestartproject.workflow.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB annotated implementation of {@link WorkflowConfiguration}
 */
@XmlType(name = "configuration", namespace = "http://workflow.capestartproject.com")
@XmlRootElement(name = "configuration", namespace = "http://workflow.capestartproject.com")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowConfigurationImpl implements WorkflowConfiguration, Comparable<WorkflowConfiguration> {
  @XmlAttribute
  protected String key;
  @XmlValue
  protected String value;

  public WorkflowConfigurationImpl() {
  }

  public WorkflowConfigurationImpl(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WorkflowConfigurationImpl other = (WorkflowConfigurationImpl) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "workflow configuration " + this.key + "=" + this.value;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(WorkflowConfiguration o) {
    return this.key.compareTo(o.getKey());
  }

  /**
   * Allows JAXB handling of {@link WorkflowConfiguration} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowConfigurationImpl, WorkflowConfiguration> {
    public WorkflowConfigurationImpl marshal(WorkflowConfiguration config) throws Exception {
      return (WorkflowConfigurationImpl) config;
    }

    public WorkflowConfiguration unmarshal(WorkflowConfigurationImpl config) throws Exception {
      return config;
    }
  }
}
