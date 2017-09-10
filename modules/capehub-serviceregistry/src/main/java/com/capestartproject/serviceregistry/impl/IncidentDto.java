package com.capestartproject.serviceregistry.impl;

import static com.capestartproject.common.util.JsonVal.asString;
import static com.capestartproject.common.util.Jsons.obj;
import static com.capestartproject.common.util.Jsons.p;
import static com.capestartproject.common.util.data.Tuple.tuple;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.capestartproject.common.job.api.Incident;
import com.capestartproject.common.util.JsonObj;
import com.capestartproject.common.util.JsonVal;
import com.capestartproject.common.util.Jsons;
import com.capestartproject.common.util.Jsons.Prop;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Tuple;
import com.capestartproject.common.util.persistence.Queries;

@Entity(name = "Incident")
@Access(AccessType.FIELD)
@Table(name = "mh_incident")
@NamedQueries({@NamedQuery(name = "Incident.findByJobId",
                           query = "select a from Incident a where a.jobId = :jobId")})
public class IncidentDto {
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "jobid")
  private Long jobId;

  @Column(name = "timestamp")
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;

  @Column(name = "code")
  private String code;

  @Column(name = "severity")
  private Incident.Severity severity;

  @Lob
  @Column(name = "parameters", length = 65535)
  private String parameters;

  @Lob
  @Column(name = "details", length = 65535)
  private String details;

  /** Constructor method. */
  public static IncidentDto mk(
          Long jobId,
          Date date,
          String code,
          Incident.Severity severity,
          Map<String, String> parameters,
          List<Tuple<String, String>> details) {
    IncidentDto dto = new IncidentDto();
    dto.jobId = jobId;
    dto.timestamp = date;
    dto.code = code;
    dto.severity = severity;

    List<Prop> props = new ArrayList<Jsons.Prop>();
    for (Entry<String, String> entry : parameters.entrySet()) {
      props.add(p(entry.getKey(), entry.getValue()));
    }
    dto.parameters = obj(props.toArray(new Prop[props.size()])).toJson();

    props = new ArrayList<Jsons.Prop>();
    for (Tuple<String, String> t : details) {
      props.add(p(t.getA(), t.getB()));
    }
    dto.details = obj(props.toArray(new Prop[props.size()])).toJson();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public long getJobId() {
    return jobId;
  }

  /** @see org.opencastproject.job.api.Incident#getTimestamp() */
  public Date getTimestamp() {
    return timestamp;
  }

  /** @see org.opencastproject.job.api.Incident#getSeverity() */
  public Incident.Severity getSeverity() {
    return severity;
  }

  /** @see org.opencastproject.job.api.Incident#getCode() */
  public String getCode() {
    return code;
  }

  /** @see org.opencastproject.job.api.Incident#getDetails() */
  public List<Tuple<String, String>> getTechnicalInformation() {
    final List<Tuple<String, String>> list = new ArrayList<Tuple<String, String>>();
    JsonObj messageJson = JsonObj.jsonObj(details);
    for (Object k : messageJson.keySet()) {
      String title = JsonVal.asJsonVal.apply(k).as(asString);
      String content = messageJson.val(title).as(asString);
      list.add(tuple(title, content));
    }
    return list;
  }

  /** @see org.opencastproject.job.api.Incident#getDescriptionParameters() */
  public Map<String, String> getParameters() {
    Map<String, String> param = new HashMap<String, String>();
    JsonObj paramJson = JsonObj.jsonObj(parameters);
    for (Object k : paramJson.keySet()) {
      String key = JsonVal.asJsonVal.apply(k).as(asString);
      String value = paramJson.val(key).as(asString);
      param.put(key, value);
    }
    return param;
  }

  public static Function<EntityManager, List<IncidentDto>> findByJobId(long jobId) {
    return Queries.named.findAll("Incident.findByJobId", tuple("jobId", jobId));
  }
}
