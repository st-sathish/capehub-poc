package com.capestartproject.userdirectory.endpoint;

import static com.capestartproject.common.util.RestUtil.getEndpointUrl;
import static com.capestartproject.common.util.UrlSupport.uri;
import static com.capestartproject.common.util.doc.rest.RestParameter.Type.STRING;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.security.api.JaxbUser;
import com.capestartproject.common.security.api.JaxbUserList;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.UrlSupport;
import com.capestartproject.common.util.data.Tuple;
import com.capestartproject.common.util.doc.rest.RestParameter;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;
import com.capestartproject.kernel.security.persistence.JpaOrganization;
import com.capestartproject.userdirectory.JpaRole;
import com.capestartproject.userdirectory.JpaUser;
import com.capestartproject.userdirectory.JpaUserAndRoleProvider;

/**
 * Provides a sorted set of known users
 */
@Path("/")
@RestService(name = "UsersUtils", title = "User utils", notes = "This service offers the default CRUD Operations for the internal matterhorn users.", abstractText = "Provides operations for internal matterhorn users")
public class UserEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(UserEndpoint.class);

  private JpaUserAndRoleProvider jpaUserAndRoleProvider;

  private SecurityService securityService;

  private String endpointBaseUrl;

  /** OSGi callback. */
  public void activate(ComponentContext cc) {
    logger.info("Start users endpoint");
    final Tuple<String, String> endpointUrl = getEndpointUrl(cc);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setJpaUserAndRoleProvider(JpaUserAndRoleProvider jpaUserAndRoleProvider) {
    this.jpaUserAndRoleProvider = jpaUserAndRoleProvider;
  }

  @GET
  @Path("users.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "allusersasjson", description = "Returns a list of users", returnDescription = "Returns a JSON representation of the list of user accounts", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  public JaxbUserList getUsersAsJson(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
          throws IOException {

    // Set the maximum number of items to return to 100 if this limit parameter is not given
    if (limit < 1)
      limit = 100;

    JaxbUserList userList = new JaxbUserList();
    for (Iterator<User> i = jpaUserAndRoleProvider.findUsers("%", offset, limit); i.hasNext();) {
      userList.add(i.next());
    }
    return userList;
  }

  @GET
  @Path("{username}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "user", description = "Returns a user", returnDescription = "Returns a JSON representation of a user", pathParameters = { @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The user account."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found") })
  public JaxbUser getUserAsJson(@PathParam("username") String username) throws NotFoundException {
    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null)
      throw new NotFoundException();
    return JaxbUser.fromUser(user);
  }

  @POST
  @Path("/")
  @RestQuery(name = "createUser", description = "Create a new  user", returnDescription = "The location of the new ressource", restParameters = {
          @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING),
          @RestParameter(description = "The password.", isRequired = true, name = "password", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array, for example: [\"ROLE_USER\", \"ROLE_ADMIN\"]") }, reponses = {
          @RestResponse(responseCode = SC_CREATED, description = "User has been created."),
          @RestResponse(responseCode = SC_CONFLICT, description = "An user with this username already exist.") })
  public Response createUser(@FormParam("username") String username, @FormParam("password") String password,
          @FormParam("roles") String roles) throws NotFoundException {

    User existingUser = jpaUserAndRoleProvider.loadUser(username);
    if (existingUser != null) {
      return Response.status(SC_CONFLICT).build();
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();

    JSONArray rolesArray = (JSONArray) JSONValue.parse(roles);
    Set<JpaRole> rolesSet = new HashSet<JpaRole>();

    // Add the roles given
    if (rolesArray != null) {
      // Add the roles given
      for (Object role : rolesArray) {
        rolesSet.add(new JpaRole((String) role, organization));
      }
    } else {
      rolesSet.add(new JpaRole(organization.getAnonymousRole(), organization));
    }

    JpaUser user = new JpaUser(username, password, organization, rolesSet);
    jpaUserAndRoleProvider.addUser(user);

    return Response.created(uri(endpointBaseUrl, user.getUsername() + ".json")).build();
  }

  @PUT
  @Path("{username}.json")
  @RestQuery(name = "updateUser", description = "Update an user", returnDescription = "Status ok", restParameters = {
          @RestParameter(description = "The password.", isRequired = false, name = "password", type = STRING),
          @RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array, for example: [\"ROLE_USER\", \"ROLE_ADMIN\"]") }, pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been updated."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response updateUser(@PathParam("username") String username, @FormParam("password") String password,
          @FormParam("roles") String roles) throws NotFoundException {

    User user = jpaUserAndRoleProvider.loadUser(username);
    if (user == null) {
      throw new NotFoundException("User " + username + " does not exist.");
    }

    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();
    Set<JpaRole> rolesSet = new HashSet<JpaRole>();

    JSONArray rolesArray = (JSONArray) JSONValue.parse(roles);
    if (rolesArray != null) {
      // Add the roles given
      for (Object role : rolesArray) {
        rolesSet.add(new JpaRole((String) role, organization));
      }
    } else {
      // Or the use the one from the user if no one is given
      for (Role role : user.getRoles()) {
        rolesSet.add(new JpaRole(role.getName(), organization));
      }
    }

    jpaUserAndRoleProvider.updateUser(new JpaUser(username, password, (JpaOrganization) organization, rolesSet));
    return Response.status(SC_OK).build();
  }

  @DELETE
  @Path("{username}.json")
  @RestQuery(name = "deleteUser", description = "Deleter a new  user", returnDescription = "Status ok", pathParameters = @RestParameter(name = "username", type = STRING, isRequired = true, description = "The username"), reponses = {
          @RestResponse(responseCode = SC_OK, description = "User has been deleted."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found.") })
  public Response deleteUser(@PathParam("username") String username) throws NotFoundException {
    Organization organization = securityService.getOrganization();

    try {
      jpaUserAndRoleProvider.deleteUser(username, organization.getId());
    } catch (NotFoundException e) {
      logger.error("User {} not found.", username);
      return Response.status(SC_NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Error during deletion of user {}: {}", username, e);
      return Response.status(SC_INTERNAL_SERVER_ERROR).build();
    }

    logger.debug("User {} removed.", username);
    return Response.status(SC_OK).build();
  }
}
