package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Tuple.tuple;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Tuple;

/**
 * JAXB DTO for a technical detail of a job incident. See {@link Incident#getDetails()}.
 * <p/>
 * To read about why this class is necessary, see http://java.net/jira/browse/JAXB-223
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "detail", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentDetail {
  @XmlAttribute(name = "title")
  private String title;

  @XmlValue
  private String content;

  /** Constructor for JAXB */
  public JaxbIncidentDetail() {
  }

  public JaxbIncidentDetail(Tuple<String, String> detail) {
    this.title = detail.getA();
    this.content = detail.getB();
  }

  public static final Function<Tuple<String, String>, JaxbIncidentDetail> mkFn = new Function<Tuple<String, String>, JaxbIncidentDetail>() {
    @Override public JaxbIncidentDetail apply(Tuple<String, String> detail) {
      return new JaxbIncidentDetail(detail);
    }
  };

  public Tuple<String, String> toDetail() {
    return tuple(title, content);
  }

  public static final Function<JaxbIncidentDetail, Tuple<String, String>> toDetailFn = new Function<JaxbIncidentDetail, Tuple<String, String>>() {
    @Override public Tuple<String, String> apply(JaxbIncidentDetail dto) {
      return dto.toDetail();
    }
  };
}
