package com.capestartproject.workflow.impl;

import static com.capestartproject.common.security.util.SecurityUtil.createSystemUser;

import java.util.Dictionary;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.util.SecurityContext;
import com.capestartproject.common.security.util.SecurityUtil;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.workflow.api.WorkflowService;

/**
 * Implementation of {@link AbstractWorkflowCleanupScheduler} to run in an OSGi environment
 */
public class OsgiWorkflowCleanupScheduler extends AbstractWorkflowCleanupScheduler implements ManagedService {

  /** Reference to the Workflow service */
  private WorkflowService workflowService;

  /** Reference to the service registry */
  private ServiceRegistry serviceRegistry;

  /** Reference to the security service */
  private SecurityService securityService;

  /** Reference to the organization directory service */
  private OrganizationDirectoryService directoryService;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OsgiWorkflowCleanupScheduler.class);

  private static final String PARAM_KEY_ENABLED = "enabled";
  private static final String PARAM_KEY_CRON_EXPR = "cron-expression";
  private static final String PARAM_KEY_LIFETIME_SUCCEEDED = "lifetime.succeeded";
  private static final String PARAM_KEY_LIFETIME_FAILED = "lifetime.failed";
  private static final String PARAM_KEY_LIFETIME_STOPPED = "lifetime.stopped";
  private static final String PARAM_KEY_LIFETIME_PARENTLESS = "lifetime.parentless";

  private String systemUserName;

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(PARAM_KEY_ENABLED));
      logger.debug("enabled = {}", enabled);

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression))
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      logger.debug("cronExpression = {}", cronExpression);

      try {
        lifetimeSuccessfulJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_SUCCEEDED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_LIFETIME_SUCCEEDED, "Lifetime must be a valid integer", e);
      }
      logger.debug("lifetimeFinishedJobs = {}", lifetimeSuccessfulJobs);

      try {
        lifetimeFailedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_FAILED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_LIFETIME_FAILED, "Lifetime must be a valid integer", e);
      }
      logger.debug("lifetimeFailedJobs = {}", lifetimeFailedJobs);

      try {
        lifetimeStoppedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_STOPPED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_LIFETIME_STOPPED, "Lifetime must be a valid integer", e);
      }
      logger.debug("lifetimeStoppedJobs = {}", lifetimeStoppedJobs);

      try {
        lifetimeParentlessJobs = Integer.valueOf((String) properties.get(PARAM_KEY_LIFETIME_PARENTLESS));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_LIFETIME_PARENTLESS, "Lifetime must be a valid integer", e);
      }
      logger.debug("lifetimeParentlessJobs = {}", lifetimeParentlessJobs);
    }

    schedule();
  }

  @Override
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return directoryService;
  }

  @Override
  public SecurityContext getAdminContextFor(String orgId) {
    try {
      final Organization org = directoryService.getOrganization(orgId);
      return new SecurityContext(securityService, org, createSystemUser(systemUserName, org));
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  @Override
  public WorkflowService getWorkflowService() {
    return this.workflowService;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  /** OSGi component activate callback */
  protected void activate(ComponentContext cc) {
    systemUserName = cc.getBundleContext().getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
  }

  /** OSGi deactivate component callback. */
  public void deactivate() {
    shutdown();
  }

  /** OSGi callback to set workflow service */
  protected void bindWorkflowService(WorkflowService srv) {
    this.workflowService = srv;
  }

  /** OSGi callback to set organization directory service */
  protected void bindOrganizationDirectoryService(OrganizationDirectoryService directoryService) {
    this.directoryService = directoryService;
  }

  /** OSGi callback to set security service */
  protected void bindSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected void bindServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

}
