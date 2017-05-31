package com.capestartproject.common.security.api;

import com.capestartproject.common.util.NotFoundException;

import java.net.URL;
import java.util.List;

/**
 * Manages organizations.
 */
public interface OrganizationDirectoryService {

  /**
   * Gets an organization by its identifier.
   *
   * @param id
   *          the identifier
   * @return the organization with this identifier
   */
  Organization getOrganization(String id) throws NotFoundException;

  /**
   * Gets an organization by request URL.
   *
   * @param url
   *          a request URL
   * @return the organization that is mapped to this URL
   */
  Organization getOrganization(URL url) throws NotFoundException;

  /**
   * Gets all registered organizations.
   *
   * @return the organizations
   */
  List<Organization> getOrganizations();
}
