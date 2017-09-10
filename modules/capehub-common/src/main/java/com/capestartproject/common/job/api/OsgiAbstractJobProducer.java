package com.capestartproject.common.job.api;

import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;

/**
 * Refined implementation of {@link org.opencastproject.job.api.AbstractJobProducer} suitable for use in an
 * OSGi environment.
 * <p/>
 * OSGi dependency injection methods are provided to reduce the amount of boilerplate code needed per
 * service implementation.
 */
public abstract class OsgiAbstractJobProducer extends AbstractJobProducer {
  private ServiceRegistry serviceRegistry;
  private SecurityService securityService;
  private UserDirectoryService userDirectoryService;
  private OrganizationDirectoryService organizationDirectoryService;

  protected OsgiAbstractJobProducer(String jobType) {
    super(jobType);
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Override
  public OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }
}
