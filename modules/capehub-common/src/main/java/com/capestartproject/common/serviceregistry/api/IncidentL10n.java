
package com.capestartproject.common.serviceregistry.api;

/** Locale dependent information for a {@link org.opencastproject.job.api.Incident}. */
public interface IncidentL10n {

  /** Get the fully processed, localized title. */
  String getTitle();

  /** Get the fully processed, localized description. */
  String getDescription();
}
