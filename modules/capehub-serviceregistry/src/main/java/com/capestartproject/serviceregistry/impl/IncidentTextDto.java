package com.capestartproject.serviceregistry.impl;

import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.persistence.Queries;

@Entity(name = "IncidentText")
@Access(AccessType.FIELD)
@Table(name = "ch_incident_text")
@NamedQueries({@NamedQuery(name = "IncidentText.findAll", query = "select a from IncidentText a")})
public class IncidentTextDto {
  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "text")
  private String text;

  public String getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public static IncidentTextDto mk(String id, String text) {
    final IncidentTextDto dto = new IncidentTextDto();
    dto.id = id;
    dto.text = text;
    return dto;
  }

  public static final Function<EntityManager, List<IncidentTextDto>> findAll =
          Queries.named.findAll("IncidentText.findAll");
}
