package com.capestartproject.google.endpoint;

import static com.capestartproject.common.util.doc.rest.RestParameter.Type.STRING;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.doc.rest.RestParameter;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;

/**
 * Provides a sorted set of known users
 */
@Path("/")
@RestService(name = "User Directory Google Endpoint", title = "User utils", notes = "This service offers the default CRUD Operations for the internal capehub users.", abstractText = "Provides operations for internal capehub users")
public class UserDirectoryGoogleRestEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(UserDirectoryGoogleRestEndpoint.class);

	/** OSGi callback. */
	public void activate(ComponentContext cc) {
		logger.info("Started users directory google endpoint");
	}

	/** OSGI callback deactivate */
	public void deactivate() {
		logger.info("Deactivated users directory google endpoint");
	}

	@POST
	@Path("/users")
	@RestQuery(name = "createUser", description = "Create a user in google user directory", returnDescription = "The location of the new ressource", restParameters = {
			@RestParameter(description = "The email.", isRequired = true, name = "email", type = STRING),
			@RestParameter(description = "The firstName.", isRequired = true, name = "firstName", type = STRING),
			@RestParameter(description = "The lastName.", isRequired = true, name = "lastName", type = STRING),
			@RestParameter(description = "User Mobile Number.", isRequired = true, name = "mobileNumber", type = STRING),
			@RestParameter(name = "roles", type = STRING, isRequired = false, description = "The user roles as a json array, for example: [\"ROLE_USER\", \"ROLE_ADMIN\"]") }, reponses = {
					@RestResponse(responseCode = SC_CREATED, description = "User has been created."),
					@RestResponse(responseCode = SC_CONFLICT, description = "An user with this email already exist.") })
	public Response createUser(@FormParam("user") String user, @FormParam("roles") String roles)
			throws NotFoundException {
		logger.info("Creating new user in google user directory {}", user);
		return Response.ok().build();
	}
}
