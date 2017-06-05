package com.capestartproject.kernel.userdirectory;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static com.capestartproject.common.util.doc.rest.RestParameter.Type.STRING;

import com.capestartproject.common.security.api.JaxbUser;
import com.capestartproject.common.security.api.JaxbUserList;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.security.api.UserDirectoryService;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.doc.rest.RestParameter;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;

import java.io.IOException;
import java.util.Iterator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Provides a sorted set of known users
 */
@Path("/")
@RestService(name = "users", title = "User account manager", notes = "This service offers the ability to manage the roles for internal accounts.", abstractText = "Displays the users available in "
        + "the current user's organization")
public class UserEndpoint {

  /** The role directory service */
  protected UserDirectoryService userDirectoryService = null;

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @GET
  @Path("users.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "allusersasxml", description = "Returns a list of users", returnDescription = "Returns a XML representation of the list of user accounts", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  public JaxbUserList getUsersAsXml(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
          throws IOException {
    if (limit < 1)
      limit = 100;

    JaxbUserList userList = new JaxbUserList();
    for (Iterator<User> i = userDirectoryService.findUsers("%", offset, limit); i.hasNext();) {
      userList.add(i.next());
    }
    return userList;
  }

  @GET
  @Path("users.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "allusersasjson", description = "Returns a list of users", returnDescription = "Returns a JSON representation of the list of user accounts", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The user accounts.") })
  public JaxbUserList getUsersAsJson(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
          throws IOException {
    return getUsersAsXml(limit, offset);
  }

  @GET
  @Path("{username}.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "user", description = "Returns a user", returnDescription = "Returns a XML representation of a user", pathParameters = { @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The user account."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found") })
  public JaxbUser getUserAsXml(@PathParam("username") String username) throws NotFoundException {
    User user = userDirectoryService.loadUser(username);
    if (user == null)
      throw new NotFoundException();
    return JaxbUser.fromUser(user);
  }

  @GET
  @Path("{username}.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "user", description = "Returns a user", returnDescription = "Returns a JSON representation of a user", pathParameters = { @RestParameter(description = "The username.", isRequired = true, name = "username", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The user account."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "User not found") })
  public JaxbUser getUserAsJson(@PathParam("username") String username) throws NotFoundException {
    return getUserAsXml(username);
  }

}
