package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.IncidentService;
import com.capestartproject.common.serviceregistry.api.IncidentServiceException;
import com.capestartproject.common.util.NotFoundException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentDigestList", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentDigestList", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentDigestList {

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncidentDigest> incidents;

  /** Constructor for JAXB */
  public JaxbIncidentDigestList() {
  }

  public JaxbIncidentDigestList(IncidentService svc, Locale locale, List<Incident> incidents)
          throws IncidentServiceException, NotFoundException {
    this.incidents = mlist(incidents).map(JaxbIncidentDigest.mkFn(svc, locale)).value();
  }
}
