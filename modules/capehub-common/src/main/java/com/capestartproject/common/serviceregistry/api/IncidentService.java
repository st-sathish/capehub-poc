package com.capestartproject.common.serviceregistry.api;

import com.capestartproject.common.job.api.Incident;
import com.capestartproject.common.job.api.IncidentTree;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Tuple;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Manages storing and retrieving of job incidents. */
public interface IncidentService {
  /**
   * Identifier for service registration and location
   */
  String JOB_TYPE = "org.opencastproject.incident";

  /**
   * Stores a new job incident.
   *
   * @throws IllegalStateException
   *           if no job related job exists
   * @throws IncidentServiceException
   *           if there is a problem communicating with the underlying data store
   */
  Incident storeIncident(Job job, Date timestamp, String code, Incident.Severity severity,
          Map<String, String> descriptionParameters, List<Tuple<String, String>> details)
          throws IncidentServiceException, IllegalStateException;

  /**
   * Gets a job incident by a given incident identifier.
   *
   * @param id
   *          the incident indentifier
   * @return the job incident
   * @throws IncidentServiceException
   *           if there is a problem communicating with the underlying data store
   * @throws NotFoundException
   *           if there is no job incident with this incident identifier
   */
  Incident getIncident(long id) throws IncidentServiceException, NotFoundException;

  /**
   * Get the hierarchy of incidents for a given job identifier.
   *
   * @param jobId
   *          the job identifier
   * @param cascade
   *          if true, return the incidents of the given job and those of of its descendants; if false, just return the
   *          incidents of the given job, which means that the list returned by
   *          {@link org.opencastproject.job.api.IncidentTree#getDescendants()} will always be empty
   * @return the list of incidents
   * @throws NotFoundException
   *           if there is no incident with this job identifier
   * @throws IncidentServiceException
   *           if there is a problem communicating with the underlying data store
   */
  IncidentTree getIncidentsOfJob(long jobId, boolean cascade) throws NotFoundException, IncidentServiceException;

  /**
   * Get the directly related incidents of all given jobs and return them concatenated into a single list. No incidents
   * of any descendants will be returned.
   *
   * @param jobIds
   *          the job identifiers
   * @return the concatenated list of directly related incidents
   * @throws IncidentServiceException
   *           if there is a problem communicating with the underlying data store
   */
  List<Incident> getIncidentsOfJob(List<Long> jobIds) throws IncidentServiceException;

  /**
   * Gets the localized texts for an incident by a given incident identifier and locale. If there are no localized texts
   * of the given locale, the default localized texts are returned.
   *
   * @param id
   *          the incident identifier
   * @param locale
   *          the locale
   * @return the incident localization
   * @throws NotFoundException
   *           if there is no job incident with this incident identifier
   * @throws IncidentServiceException
   *           if there is a problem communicating with the underlying data store
   */
  IncidentL10n getLocalization(long id, Locale locale) throws IncidentServiceException, NotFoundException;
}
