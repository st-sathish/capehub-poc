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
import com.capestartproject.common.util.data.Function;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "incidentDigestTree", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentDigestTree", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentDigestTree {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncidentDigest> incidents;

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
  private List<JaxbIncidentDigestTree> descendants;

  /** Constructor for JAXB */
  public JaxbIncidentDigestTree() {
  }

  public JaxbIncidentDigestTree(IncidentService svc, Locale locale, IncidentTree tree) throws IncidentServiceException,
          NotFoundException {
    this.incidents = mlist(tree.getIncidents()).map(JaxbIncidentDigest.mkFn(svc, locale)).value();
    this.descendants = mlist(tree.getDescendants()).map(mkFn(svc, locale)).value();
  }

  public static Function<IncidentTree, JaxbIncidentDigestTree> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<IncidentTree, JaxbIncidentDigestTree>() {
      @Override
      public JaxbIncidentDigestTree xapply(IncidentTree tree) throws Exception {
        return new JaxbIncidentDigestTree(svc, locale, tree);
      }
    };
  }
}
