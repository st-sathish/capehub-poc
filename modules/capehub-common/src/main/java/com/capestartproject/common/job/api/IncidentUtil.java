package com.capestartproject.common.job.api;

import static com.capestartproject.common.util.data.Monadics.mlist;

import java.util.List;

import com.capestartproject.common.fn.juc.Mutables;
import com.capestartproject.common.util.data.Function2;

public final class IncidentUtil {
  private IncidentUtil() {
  }

  /** Concat a tree of incidents into a list. */
  public static List<Incident> concat(IncidentTree tree) {
    return mlist(tree.getDescendants()).foldl(
            Mutables.list(tree.getIncidents()),
            new Function2<List<Incident>, IncidentTree, List<Incident>>() {
              @Override public List<Incident> apply(List<Incident> sum, IncidentTree tree) {
                sum.addAll(concat(tree));
                return sum;
              }
            });
  }
}
