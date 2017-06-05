package com.capestartproject.common.job.api;

import com.capestartproject.common.util.data.Tuple;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** Describes an incident relating to a {@link Job}. */
public interface Incident {
  public enum Severity {
    INFO,
    WARNING,

    /**
     * An incident of type FAILURE shall only be recorded when a job fails, i.e. enters the
     * {@link com.capestartproject.job.api.Job.Status#FAILED} state. That implies that there
     * can be at most one FAILURE incident per job.
     */
    FAILURE
  }

  /** Return the incident id. */
  long getId();

  /**
   * The job related to this incident.
   *
   * @return the job id
   */
  long getJobId();

  /**
   * The service type on which the incident was occurring.
   *
   * @return the service type
   */
  String getServiceType();

  /**
   * The processing host running the job where the incident was occurring.
   *
   * @return the processing host
   */
  String getProcessingHost();

  /**
   * The date where the incident was happening.
   *
   * @return the date
   */
  Date getTimestamp();

  /**
   * The severity of this incident.
   *
   * @return the severity
   */
  Severity getSeverity();

  /**
   * The unique code of this incident. Incident codes may be mapped to plain text, possibly localized.
   * It is recommended to create codes after the schema <code>service_type.number</code>,
   * e.g. <code>org.opencastproject.service.1511</code>
   *
   * @return the incident code
   * @see org.opencastproject.job.api.Job#getJobType()
   */
  String getCode();

  /**
   * List of additional technical information having a name and a text <code>[(name, text)]</code>.
   * This may be an exception, an ffmpeg commandline, memory statistics, etc.
   *
   * @return a list of technical background information describing the incident in depth
   *         [(detail_name, detail)]
   */
  List<Tuple<String, String>> getDetails();

  /**
   * Named parameters describing the incident in more detail. These parameters may be used to
   * construct a description message.
   *
   * @return the message parameters; parameter_name -> parameter_value
   */
  Map<String, String> getDescriptionParameters();
}
