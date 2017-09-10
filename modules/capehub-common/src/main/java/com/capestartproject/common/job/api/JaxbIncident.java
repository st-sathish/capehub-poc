package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Collections.nullToNil;
import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.capestartproject.common.fn.juc.Mutables;
import com.capestartproject.common.job.api.Incident.Severity;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.jaxb.UtcTimestampAdapter;

/** 1:1 serialization of a {@link Incident}. */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "incident", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incident", namespace = "http://job.capestartproject.com")
public final class JaxbIncident {
  @XmlElement(name = "id")
  private long id;

  @XmlElement(name = "jobId")
  private long jobId;

  @XmlElement(name = "serviceType")
  private String serviceType;

  @XmlElement(name = "processingHost")
  private String processingHost;

  @XmlElement(name = "timestamp")
  @XmlJavaTypeAdapter(UtcTimestampAdapter.class)
  private Date timestamp;

  @XmlElement(name = "severity")
  private Severity severity;

  @XmlElement(name = "code")
  private String code;

  @XmlElementWrapper(name = "descriptionParameters")
  @XmlElement(name = "param")
  private List<Param> descriptionParameters;

  @XmlElementWrapper(name = "details")
  @XmlElement(name = "detail")
  private List<JaxbIncidentDetail> details;

  /** Constructor for JAXB */
  public JaxbIncident() {
  }

  public JaxbIncident(Incident incident) {
    this.id = incident.getId();
    this.jobId = incident.getJobId();
    this.serviceType = incident.getServiceType();
    this.processingHost = incident.getProcessingHost();
    this.timestamp = new Date(incident.getTimestamp().getTime());
    this.severity = incident.getSeverity();
    this.code = incident.getCode();
    this.descriptionParameters = mlist(incident.getDescriptionParameters().entrySet()).map(Param.mkFn).value();
    this.details = mlist(incident.getDetails()).map(JaxbIncidentDetail.mkFn).value();
  }

  public static final Function<Incident, JaxbIncident> mkFn = new Function<Incident, JaxbIncident>() {
    @Override
    public JaxbIncident apply(Incident incident) {
      return new JaxbIncident(incident);
    }
  };

  public Incident toIncident() {
    return new IncidentImpl(id, jobId, serviceType, processingHost, timestamp, severity, code,
            mlist(nullToNil(details)).map(JaxbIncidentDetail.toDetailFn).value(), mlist(
                    nullToNil(descriptionParameters)).foldl(Mutables.<String, String> hashMap(),
                    new Function2<Map<String, String>, Param, Map<String, String>>() {
                      @Override
                      public Map<String, String> apply(Map<String, String> sum, Param param) {
                        sum.put(param.getName(), param.getValue());
                        return sum;
                      }
                    }));
  }

  public static final Function<JaxbIncident, Incident> toIncidentFn = new Function<JaxbIncident, Incident>() {
    @Override
    public Incident apply(JaxbIncident dto) {
      return dto.toIncident();
    }
  };

  /**
   * An description parameter. To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "param", namespace = "http://job.opencastproject.org")
  public static final class Param {
    @XmlAttribute(name = "name")
    private String name;

    @XmlValue
    private String value;

    public static Param mk(Entry<String, String> entry) {
      final Param dto = new Param();
      dto.name = entry.getKey();
      dto.value = entry.getValue();
      return dto;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public static final Function<Entry<String, String>, Param> mkFn = new Function<Entry<String, String>, Param>() {
      @Override
      public Param apply(Entry<String, String> entry) {
        return mk(entry);
      }
    };
  }
}
