package com.capestartproject.kernel.security.persistence;

import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.util.NotFoundException;

import java.util.List;

/**
 * API that defines persistent storage of organizations.
 */
public interface OrganizationDatabase {

  /**
   * Returns all organizations in persistent storage.
   *
   * @return the list of stored organizations
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  List<Organization> getOrganizations() throws OrganizationDatabaseException;

  /**
   * Counts all organizations in persistent storage
   *
   * @return the number of organizations
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  int countOrganizations() throws OrganizationDatabaseException;

  /**
   * Gets a single organization by its identifier.
   *
   * @param orgId
   *          the organization's identifier
   * @return the organization
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   * @throws NotFoundException
   *           if the organization with specified ID is not found
   */
  Organization getOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException;

  /**
   * Gets a single organization by host and port.
   *
   * @param host
   *          the organization's host
   * @param port
   *          the organization's port
   * @return the organization
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   * @throws NotFoundException
   *           if the organization with specified URL is not found
   */
  Organization getOrganizationByHost(String host, int port) throws OrganizationDatabaseException, NotFoundException;

  /**
   * Removes an organization from persistent storage.
   *
   * @param orgId
   *          ID of the organization to be removed
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   * @throws NotFoundException
   *           if the organization with specified ID is not found
   */
  void deleteOrganization(String orgId) throws OrganizationDatabaseException, NotFoundException;

  /**
   * Stores (or updates) an organization.
   *
   * @param organization
   *          the organization to store
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  void storeOrganization(Organization organization) throws OrganizationDatabaseException;

  /**
   * Returns <code>true</code> if the given organization by its identifier is found or <code>false</code> if not
   *
   * @param orgId
   *          the organization's identifier
   * @return <code>true</code> if found or <code>false</code> if not
   * @throws OrganizationDatabaseException
   *           if there is a problem communicating with the underlying data store
   */
  boolean containsOrganization(String orgId) throws OrganizationDatabaseException;

}
