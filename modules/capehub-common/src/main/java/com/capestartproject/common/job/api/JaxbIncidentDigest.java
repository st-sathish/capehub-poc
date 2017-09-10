package com.capestartproject.common.job.api;

import java.util.Date;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.IncidentL10n;
import com.capestartproject.common.serviceregistry.api.IncidentService;
import com.capestartproject.common.util.data.Function;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentDigest", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentDigest", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentDigest {
  @XmlElement(name = "id")
  private long id;

  @XmlElement(name = "jobid")
  private long jobId;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "date")
  private Date date;

  @XmlElement(name = "severity")
  private String severity;

  /** Constructor for JAXB */
  public JaxbIncidentDigest() {
  }

  public JaxbIncidentDigest(Incident incident, IncidentL10n l10n) {
    this.id = incident.getId();
    this.jobId = incident.getJobId();
    this.title = l10n.getTitle();
    this.date = incident.getTimestamp();
    this.severity = incident.getSeverity().name();
    this.description = l10n.getDescription();
  }

  public static Function<Incident, JaxbIncidentDigest> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<Incident, JaxbIncidentDigest>() {
      @Override public JaxbIncidentDigest xapply(Incident incident) throws Exception {
        return new JaxbIncidentDigest(incident, svc.getLocalization(incident.getId(), locale));
      }
    };
  }
}
