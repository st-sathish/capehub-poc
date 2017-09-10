package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Collections.nullToNil;
import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.IncidentServiceException;
import com.capestartproject.common.util.NotFoundException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentList", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentList", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentList {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncident> incidents;

  /** Default constructor needed by jaxb */
  public JaxbIncidentList() {
  }

  public JaxbIncidentList(List<Incident> incidents)
          throws IncidentServiceException, NotFoundException {
    this.incidents = mlist(incidents).map(JaxbIncident.mkFn).value();
  }

  public List<Incident> toIncidents() {
    return mlist(nullToNil(incidents)).map(JaxbIncident.toIncidentFn).value();
  }
}
