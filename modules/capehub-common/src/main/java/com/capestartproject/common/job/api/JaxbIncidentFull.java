package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.IncidentL10n;
import com.capestartproject.common.serviceregistry.api.IncidentService;
import com.capestartproject.common.util.data.Function;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentFull", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentFull", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentFull {
  @XmlElement(name = "id")
  private long id;

  @XmlElement(name = "jobid")
  private long jobId;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "description")
  private String description;

  @XmlElement(name = "serviceType")
  private String serviceType;

  @XmlElement(name = "processingHost")
  private String processingHost;

  @XmlElement(name = "date")
  private Date date;

  @XmlElement(name = "severity")
  private String severity;

  @XmlElement(name = "code")
  private String code;

  @XmlElement(name = "detail")
  @XmlElementWrapper(name = "details")
  private List<JaxbIncidentDetail> details;

  public JaxbIncidentFull() {
  }

  public JaxbIncidentFull(Incident incident, IncidentL10n l10n) {
    this.id = incident.getId();
    this.jobId = incident.getJobId();
    this.serviceType = incident.getServiceType();
    this.title = l10n.getTitle();
    this.processingHost = incident.getProcessingHost();
    this.date = incident.getTimestamp();
    this.severity = incident.getSeverity().name();
    this.code = incident.getCode();
    this.details = mlist(incident.getDetails()).map(JaxbIncidentDetail.mkFn).value();
    this.description = l10n.getDescription();
  }

  public static Function<Incident, JaxbIncidentFull> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<Incident, JaxbIncidentFull>() {
      @Override
      public JaxbIncidentFull xapply(Incident incident) throws Exception {
        return new JaxbIncidentFull(incident, svc.getLocalization(incident.getId(), locale));
      }
    };
  }
}
