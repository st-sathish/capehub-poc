package com.capestartproject.serviceregistry.impl;

import static com.capestartproject.common.util.data.Monadics.mlist;
import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.Option.option;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.fn.juc.Mutables;
import com.capestartproject.common.job.api.Incident;
import com.capestartproject.common.job.api.IncidentImpl;
import com.capestartproject.common.job.api.IncidentTree;
import com.capestartproject.common.job.api.IncidentTreeImpl;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.serviceregistry.api.IncidentL10n;
import com.capestartproject.common.serviceregistry.api.IncidentService;
import com.capestartproject.common.serviceregistry.api.IncidentServiceException;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.serviceregistry.api.ServiceRegistryException;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Collections;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.data.Tuple;
import com.capestartproject.common.util.data.functions.Functions;
import com.capestartproject.common.util.data.functions.Strings;
import com.capestartproject.common.util.persistence.PersistenceEnv;
import com.capestartproject.common.util.persistence.Queries;

public abstract class AbstractIncidentService implements IncidentService {
	public static final String PERSISTENCE_UNIT_NAME = "com.capestartproject.serviceregistry";

  public static final String NO_TITLE = "-";
  public static final String NO_DESCRIPTION = "-";
  public static final String FIELD_TITLE = "title";
  public static final String FIELD_DESCRIPTION = "description";

  /** The logging instance */
  private static final Logger logger = LoggerFactory.getLogger(AbstractIncidentService.class);

  protected abstract ServiceRegistry getServiceRegistry();

	// protected abstract WorkflowService getWorkflowService();

  protected abstract PersistenceEnv getPenv();

  @Override
  public Incident storeIncident(Job job, Date timestamp, String code, Incident.Severity severity,
          Map<String, String> descriptionParameters, List<Tuple<String, String>> details)
          throws IncidentServiceException, IllegalStateException {
    try {
      job = getServiceRegistry().getJob(job.getId());

      final IncidentDto dto = getPenv().tx(
              Queries.persist(IncidentDto.mk(job.getId(), timestamp, code, severity, descriptionParameters, details)));
      return toIncident(job, dto);
    } catch (NotFoundException e) {
      throw new IllegalStateException("Can't create incident for not-existing job");
    } catch (Exception e) {
      logger.error("Could not store job incident: {}", e.getMessage());
      throw new IncidentServiceException(e);
    }
  }

  @Override
  public Incident getIncident(long id) throws IncidentServiceException, NotFoundException {
    for (IncidentDto dto : getPenv().tx(Queries.find(IncidentDto.class, id))) {
      final Job job = findJob(dto.getJobId());
      if (job != null) {
        return toIncident(job, dto);
      }
    }
    throw new NotFoundException();
  }

  @Override
  public List<Incident> getIncidentsOfJob(List<Long> jobIds) throws IncidentServiceException {
    List<Incident> incidents = new ArrayList<Incident>();
    for (long jobId : jobIds) {
      try {
        incidents.addAll(getIncidentsOfJob(jobId));
      } catch (NotFoundException ignore) {
      }
    }
    return incidents;
  }

  @Override
  public IncidentTree getIncidentsOfJob(long jobId, boolean cascade) throws NotFoundException, IncidentServiceException {
    List<Incident> incidents = getIncidentsOfJob(jobId);
    List<IncidentTree> childIncidents = new ArrayList<IncidentTree>();

    try {
      Job job = getServiceRegistry().getJob(jobId);
      if (cascade && !"START_WORKFLOW".equals(job.getOperation())) {
        childIncidents = getChildIncidents(jobId);
      } else if (cascade && "START_WORKFLOW".equals(job.getOperation())) {
				// get workflow operations
      }
      return new IncidentTreeImpl(incidents, childIncidents);
    } catch (Exception e) {
      logger.error("Error loading child jobs of {}: {}", jobId);
      throw new IncidentServiceException(e);
    }
  }

  private boolean hasIncidents(List<IncidentTree> incidentResults) {
    for (IncidentTree result : incidentResults) {
      if (result.getIncidents().size() > 0 || hasIncidents(result.getDescendants()))
        return true;
    }
    return false;
  }

  @Override
  public IncidentL10n getLocalization(long id, Locale locale) throws IncidentServiceException, NotFoundException {
    final Incident incident = getIncident(id);

    final List<String> loc = localeToList(locale);
    // check if cache map is empty
    // fill cache from
    final String title = findText(loc, incident.getCode(), FIELD_TITLE).getOrElse(NO_TITLE);
    final String description = findText(loc, incident.getCode(), FIELD_DESCRIPTION).map(
            replaceVarsF(incident.getDescriptionParameters())).getOrElse(NO_DESCRIPTION);
    return new IncidentL10n() {
      @Override
      public String getTitle() {
        return title;
      }

      @Override
      public String getDescription() {
        return description;
      }
    };
  }

  /**
   * Find a localization text in the database. The keys to look for are made from the locale, the incident code and the
   * type.
   *
   * @param locale
   *          The locale as a list. See {@link AbstractIncidentService#localeToList(java.util.Locale)}
   * @param incidentCode
   *          The incident code. See {@link org.opencastproject.job.api.Incident#getCode()}
   * @param field
   *          The field, e.g. "title" or "description"
   * @return the found text wrapped in an option
   */
  private Option<String> findText(List<String> locale, String incidentCode, String field) {
    final List<String> keys = genDbKeys(locale, incidentCode + "." + field);
    for (String key : keys) {
      final Option<String> text = getText(key);
      if (text.isSome()) {
        return text;
      }
    }
    return none();
  }

  private final Map<String, String> textCache = new HashMap<String, String>();

  /** Get a text. */
  private Option<String> getText(String key) {
    synchronized (textCache) {
      if (textCache.isEmpty()) {
        textCache.putAll(fetchTextsFromDb());
      }
    }
    return option(textCache.get(key));
  }

  /** Fetch all localizations from the database. */
  private Map<String, String> fetchTextsFromDb() {
    final Map<String, String> locs = new HashMap<String, String>();
    for (IncidentTextDto a : getPenv().tx(IncidentTextDto.findAll)) {
      locs.put(a.getId(), a.getText());
    }
    return locs;
  }

  private List<IncidentTree> getChildIncidents(long jobId) throws NotFoundException, ServiceRegistryException,
          IncidentServiceException {
    List<Job> childJobs = getServiceRegistry().getChildJobs(jobId);
    List<IncidentTree> incidentResults = new ArrayList<IncidentTree>();
    for (Job childJob : childJobs) {
      if (childJob.getParentJobId() != jobId)
        continue;
      List<Incident> incidentsForJob = getIncidentsOfJob(childJob.getId());
      IncidentTree incidentTree = new IncidentTreeImpl(incidentsForJob, getChildIncidents(childJob.getId()));
      if (hasIncidents(Collections.list(incidentTree)))
        incidentResults.add(incidentTree);
    }
    return incidentResults;
  }

  private List<Incident> getIncidentsOfJob(long jobId) throws NotFoundException, IncidentServiceException {
    final Job job = findJob(jobId);
    try {
      return mlist(getPenv().tx(IncidentDto.findByJobId(jobId))).map(toIncident(job)).value();
    } catch (Exception e) {
      logger.error("Could not retrieve incidents of job '{}': {}", job.getId(), e.getMessage());
      throw new IncidentServiceException(e);
    }
  }

  private Job findJob(long jobId) throws NotFoundException, IncidentServiceException {
    try {
      return getServiceRegistry().getJob(jobId);
    } catch (NotFoundException e) {
      logger.info("Job with Id {} does not exist", jobId);
      throw e;
    } catch (ServiceRegistryException e) {
      logger.error("Could not retrieve job {}: {}", jobId, e.getMessage());
      throw new IncidentServiceException(e);
    }
  }

  private static Incident toIncident(Job job, IncidentDto dto) {
    return new IncidentImpl(dto.getId(), job.getId(), job.getJobType(), job.getProcessingHost(), dto.getTimestamp(),
            dto.getSeverity(), dto.getCode(), dto.getTechnicalInformation(), dto.getParameters());
  }

  private static Function<IncidentDto, Incident> toIncident(final Job job) {
    return new Function<IncidentDto, Incident>() {
      @Override
      public Incident apply(IncidentDto dto) {
        return toIncident(job, dto);
      }
    };
  }

  	/**
	 * Create a list of localization database keys from a base key and a locale
	 * split into its part, e.g. ["de", "DE"] or ["en"]. The returned list
	 * starts with the most specific key getting more common, e.g.
	 * ["com.capestartproject.composer.1.title.de.DE",
	 * "com.capestartproject.composer.1.title.de",
	 * "com.capestartproject.composer.1.title"]
	 */
  public static List<String> genDbKeys(List<String> locale, String base) {
    final List<String> keys = mlist(locale).foldl(Mutables.list(base),
            new Function2<List<String>, String, List<String>>() {
              @Override
              public List<String> apply(List<String> sum, String s) {
                sum.add(sum.get(sum.size() - 1) + "." + s);
                return sum;
              }
            });
    return mlist(keys).reverse().value();
  }

  /** Convert a locale into a list of strings, [language, country, variant] */
  public static List<String> localeToList(Locale locale) {
    return mlist(Strings.trimToNone(locale.getLanguage()), Strings.trimToNone(locale.getCountry()),
            Strings.trimToNone(locale.getVariant()))
    // flatten
            .bind(Functions.<Option<String>> identity()).value();
  }

  /** Replace variables of the form #{xxx} in a string template. */
  public static String replaceVars(String template, Map<String, String> params) {
    String s = template;
    for (Map.Entry<String, String> e : params.entrySet()) {
      s = s.replace("#{" + e.getKey() + "}", e.getValue());
    }
    return s;
  }

  	/**
	 * {@link com.capestartproject.serviceregistry.impl.AbstractIncidentService#replaceVars(String, java.util.Map)}
	 * as a function.
	 */
  public static Function<String, String> replaceVarsF(final Map<String, String> params) {
    return new Function<String, String>() {
      @Override
      public String apply(String s) {
        return replaceVars(s, params);
      }
    };
  }
}
