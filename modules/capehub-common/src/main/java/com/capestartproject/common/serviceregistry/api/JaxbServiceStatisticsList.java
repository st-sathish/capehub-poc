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
 * A wrapper for service statistics.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "statistics", namespace = "http://serviceregistry.capestartproject.com")
@XmlRootElement(name = "statistics", namespace = "http://serviceregistry.capestartproject.com")
public class JaxbServiceStatisticsList {
  /** A list of search items. */
  @XmlElement(name = "service")
  protected List<JaxbServiceStatistics> stats = new ArrayList<JaxbServiceStatistics>();

  public JaxbServiceStatisticsList() {
  }

  public JaxbServiceStatisticsList(Collection<ServiceStatistics> stats) {
    for (ServiceStatistics stat : stats)
      this.stats.add((JaxbServiceStatistics) stat);
  }

  /**
   * @return the stats
   */
  public List<JaxbServiceStatistics> getStats() {
    return stats;
  }

  /**
   * @param stats
   *          the stats to set
   */
  public void setStats(List<JaxbServiceStatistics> stats) {
    this.stats = stats;
  }
}
