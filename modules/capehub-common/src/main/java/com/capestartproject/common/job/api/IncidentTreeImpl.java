package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.EqualsUtil.eq;
import static com.capestartproject.common.util.EqualsUtil.hash;

import java.util.List;

import com.capestartproject.common.fn.juc.Immutables;

public final class IncidentTreeImpl implements IncidentTree {
  private final List<Incident> incidents;

  private final List<IncidentTree> descendants;

  public IncidentTreeImpl(List<Incident> incidents, List<IncidentTree> descendants) {
    this.incidents = Immutables.mk(incidents);
    if (descendants != null) {
      this.descendants = Immutables.mk(descendants);
    } else {
      this.descendants = Immutables.nil();
    }
  }

  @Override public List<Incident> getIncidents() {
    return incidents;
  }

  @Override public List<IncidentTree> getDescendants() {
    return descendants;
  }

  @Override public int hashCode() {
    return hash(incidents, descendants);
  }

  @Override public boolean equals(Object that) {
    return (this == that) || (that instanceof IncidentTree && eqFields((IncidentTree) that));
  }

  private boolean eqFields(IncidentTree that) {
    return eq(incidents, that.getIncidents())
            && eq(descendants, that.getDescendants());
  }
}
