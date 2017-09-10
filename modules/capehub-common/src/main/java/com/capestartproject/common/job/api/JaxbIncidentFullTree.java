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
@XmlType(name = "incidentFullTree", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentFullTree", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentFullTree {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncidentFull> incidents;

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
  private List<JaxbIncidentFullTree> descendants;

  /** Constructor for JAXB */
  public JaxbIncidentFullTree() {
  }

  public JaxbIncidentFullTree(IncidentService svc, Locale locale, IncidentTree tree) throws IncidentServiceException,
          NotFoundException {
    this.incidents = mlist(tree.getIncidents()).map(JaxbIncidentFull.mkFn(svc, locale)).value();
    this.descendants = mlist(tree.getDescendants()).map(mkFn(svc, locale)).value();
  }

  public static Function<IncidentTree, JaxbIncidentFullTree> mkFn(final IncidentService svc, final Locale locale) {
    return new Function.X<IncidentTree, JaxbIncidentFullTree>() {
      @Override
      public JaxbIncidentFullTree xapply(IncidentTree tree) throws Exception {
        return new JaxbIncidentFullTree(svc, locale, tree);
      }
    };
  }
}
