package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Collections.nullToNil;
import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.capestartproject.common.serviceregistry.api.IncidentServiceException;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Function;

/** 1:1 serialization of a {@link IncidentTreeImpl}. */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "incidentTree", namespace = "http://job.capestartproject.com")
@XmlRootElement(name = "incidentTree", namespace = "http://job.capestartproject.com")
public final class JaxbIncidentTree {
  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_INCIDENT)
  private List<JaxbIncident> incidents;

  @XmlElement(name = JaxbIncidentUtil.ELEM_NESTED_TREE)
  private List<JaxbIncidentTree> descendants;

  /** Constructor for JAXB */
  public JaxbIncidentTree() {
  }

  public JaxbIncidentTree(IncidentTree tree) throws IncidentServiceException, NotFoundException {
    this.incidents = mlist(tree.getIncidents()).map(JaxbIncident.mkFn).value();
    this.descendants = mlist(tree.getDescendants()).map(mkFn).value();
  }

  public static final Function<IncidentTree, JaxbIncidentTree> mkFn = new Function.X<IncidentTree, JaxbIncidentTree>() {
    @Override public JaxbIncidentTree xapply(IncidentTree tree) throws Exception {
      return new JaxbIncidentTree(tree);
    }
  };

  public IncidentTree toIncidentTree() {
    return new IncidentTreeImpl(
            mlist(nullToNil(incidents)).map(JaxbIncident.toIncidentFn).value(),
            mlist(nullToNil(descendants)).map(toIncidentTreeFn).value());
  }

  public static final Function<JaxbIncidentTree, IncidentTree> toIncidentTreeFn = new Function<JaxbIncidentTree, IncidentTree>() {
    @Override public IncidentTree apply(JaxbIncidentTree dto) {
      return dto.toIncidentTree();
    }
  };
}
