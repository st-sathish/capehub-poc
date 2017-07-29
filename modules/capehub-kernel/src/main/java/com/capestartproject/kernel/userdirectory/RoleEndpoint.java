package com.capestartproject.kernel.userdirectory;

import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.capestartproject.common.security.api.JaxbRoleList;
import com.capestartproject.common.security.api.OrganizationDirectoryService;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.RoleDirectoryService;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;

/**
 * Provides a sorted set of known roles
 */
@Path("/")
@RestService(name = "roles", title = "User Roles", notes = { "" }, abstractText = "Displays the roles available in "
        + "the current user's organization")
public class RoleEndpoint {

  /** The role directory service */
  protected RoleDirectoryService roleDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  @GET
  @Path("roles.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "rolesasxml", description = "Lists the roles as XML", returnDescription = "The list of roles as XML", reponses = { @RestResponse(responseCode = 200, description = "OK, roles returned") })
  public JaxbRoleList getRolesAsXml() {
    JaxbRoleList roleList = new JaxbRoleList();
    for (Iterator<Role> i = roleDirectoryService.getRoles(); i.hasNext();) {
      roleList.add(i.next());
    }
    return roleList;
  }

  @GET
  @Path("roles.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "rolesasjson", description = "Lists the roles as JSON", returnDescription = "The list of roles as JSON", reponses = { @RestResponse(responseCode = 200, description = "OK, roles returned") })
  public JaxbRoleList getRolesAsJson() {
    return getRolesAsXml();
  }

  /**
   * Sets the role directory service
   *
   * @param roleDirectoryService
   *          the roleDirectoryService to set
   */
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

}
