
package com.capestartproject.common.job.api;

import java.util.List;

/** A tree of incidents of a tree of jobs. */
public interface IncidentTree {
  /** Return the incidents of the job associated with this tree. */
  List<Incident> getIncidents();

  /** Return all incidents of the job's descendants. */
  List<IncidentTree> getDescendants();
}
