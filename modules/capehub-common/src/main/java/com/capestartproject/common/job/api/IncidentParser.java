package com.capestartproject.common.job.api;

import java.io.IOException;
import java.io.InputStream;

import com.capestartproject.common.util.jaxb.JaxbParser;

/** JAXB parser for JAXB DTOs of {@link Incident}. */
public final class IncidentParser extends JaxbParser {
  /** Instance of IncidentParser */
  public static final IncidentParser I =
			new IncidentParser("com.capestartproject.common.job.api:com.capestartproject.common.serviceregistry.api");

  private IncidentParser(String contextPath) {
    super(contextPath);
  }

  public JaxbIncidentDigestList parseDigestFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentDigestList.class, xml);
  }

  public JaxbIncident parseIncidentFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncident.class, xml);
  }

  public JaxbIncidentList parseIncidentsFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentList.class, xml);
  }

  public JaxbIncidentTree parseIncidentTreeFromXml(InputStream xml) throws IOException {
    return unmarshal(JaxbIncidentTree.class, xml);
  }

  public String toXml(JaxbIncident incident) throws IOException {
    return marshal(incident);
  }

  public String toXml(JaxbIncidentTree tree) throws IOException {
    return marshal(tree);
  }
}
