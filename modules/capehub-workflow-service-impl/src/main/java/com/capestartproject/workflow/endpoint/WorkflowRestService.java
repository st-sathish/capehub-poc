package com.capestartproject.workflow.endpoint;

import static com.capestartproject.common.util.doc.rest.RestParameter.Type.STRING;
import static com.capestartproject.common.util.doc.rest.RestParameter.Type.TEXT;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.job.api.JobProducer;
import com.capestartproject.common.rest.AbstractJobProducerEndpoint;
import com.capestartproject.common.rest.RestConstants;
import com.capestartproject.common.security.api.UnauthorizedException;
import com.capestartproject.common.serviceregistry.api.ServiceRegistry;
import com.capestartproject.common.systems.CapehubConstans;
import com.capestartproject.common.util.MultiResourceLock;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.UrlSupport;
import com.capestartproject.common.util.doc.rest.RestParameter;
import com.capestartproject.common.util.doc.rest.RestParameter.Type;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;
import com.capestartproject.workflow.api.Configurable;
import com.capestartproject.workflow.api.WorkflowDatabaseException;
import com.capestartproject.workflow.api.WorkflowDefinition;
import com.capestartproject.workflow.api.WorkflowDefinitionImpl;
import com.capestartproject.workflow.api.WorkflowDefinitionSet;
import com.capestartproject.workflow.api.WorkflowException;
import com.capestartproject.workflow.api.WorkflowInstance;
import com.capestartproject.workflow.api.WorkflowOperationHandler;
import com.capestartproject.workflow.api.WorkflowOperationInstance;
import com.capestartproject.workflow.api.WorkflowParser;
import com.capestartproject.workflow.api.WorkflowService;
import com.capestartproject.workflow.api.WorkflowStatistics;
import com.capestartproject.workflow.impl.WorkflowServiceImpl;
import com.capestartproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;

/**
 * A REST endpoint for the {@link WorkflowService}
 */
@Path("/")
@RestService(name = "workflowservice", title = "Workflow Service", abstractText = "This service lists available workflows and starts, stops, suspends and resumes workflow instances.", notes = {
		"All paths above are relative to the REST endpoint base (something like http://your.server/files)",
		"If the service is down or not working it will return a status 503, this means the the underlying service is "
				+ "not working and is either restarting or has failed",
		"A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
				+ "other words, there is a bug! You should file an error report with your server logs from the time when the "
				+ "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class WorkflowRestService extends AbstractJobProducerEndpoint {

	/** The default number of results returned */
	private static final int DEFAULT_LIMIT = 20;
	/**
	 * The constant used to negate a querystring parameter. This is only
	 * supported on some parameters.
	 */
	public static final String NEGATE_PREFIX = "-";
	/**
	 * The constant used to switch the direction of the sorting querystring
	 * parameter.
	 */
	public static final String DESCENDING_SUFFIX = "_DESC";
	/** The logger */
	private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);
	/** The default server URL */
	protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
	/** The default service URL */
	protected String serviceUrl = serverUrl + "/workflow";
	/** The workflow service instance */
	private WorkflowService service;
	/** The service registry */
	protected ServiceRegistry serviceRegistry = null;
	/** The workspace */
	// private Workspace workspace;

	/** Resource lock */
	private final MultiResourceLock lock = new MultiResourceLock();

	/**
	 * Callback from the OSGi declarative services to set the service registry.
	 *
	 * @param serviceRegistry
	 *            the service registry
	 */
	protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	/**
	 * Sets the workflow service
	 *
	 * @param service
	 *            the workflow service instance
	 */
	public void setService(WorkflowService service) {
		this.service = service;
	}

	/**
	 * Callback from the OSGi declarative services to set the workspace.
	 *
	 * @param workspace
	 *            the workspace
	 */
	// public void setWorkspace(Workspace workspace) {
	// this.workspace = workspace;
	// }

	/**
	 * OSGI callback for component activation
	 *
	 * @param cc
	 *            the OSGI declarative services component context
	 */
	public void activate(ComponentContext cc) {
		// Get the configured server URL
		if (cc == null) {
			serverUrl = UrlSupport.DEFAULT_BASE_URL;
		} else {
			String ccServerUrl = cc.getBundleContext().getProperty(CapehubConstans.SERVER_URL_PROPERTY);
			logger.info("configured server url is {}", ccServerUrl);
			if (ccServerUrl == null) {
				serverUrl = UrlSupport.DEFAULT_BASE_URL;
			} else {
				serverUrl = ccServerUrl;
			}
			serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
		}
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/count")
	@RestQuery(name = "count", description = "Returns the number of workflow instances in a specific state and operation", returnDescription = "Returns the number of workflow instances in a specific state and operation", restParameters = {
			@RestParameter(name = "state", isRequired = false, description = "The workflow state", type = STRING),
			@RestParameter(name = "operation", isRequired = false, description = "The current operation", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "The number of workflow instances.") })
	public Response getCount(@QueryParam("state") WorkflowInstance.WorkflowState state,
			@QueryParam("operation") String operation) {
		try {
			Long count = service.countWorkflowInstances(state, operation);
			return Response.ok(count).build();
		} catch (WorkflowDatabaseException e) {
			throw new WebApplicationException(e);
		}
	}

	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("/statistics.xml")
	@RestQuery(name = "statisticsasxml", description = "Returns the workflow statistics as XML", returnDescription = "An XML representation of the workflow statistics.", reponses = {
			@RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow statistics.") })
	public WorkflowStatistics getStatisticsAsXml() throws WorkflowDatabaseException {
		return service.getStatistics();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/statistics.json")
	@RestQuery(name = "statisticsasjson", description = "Returns the workflow statistics as JSON", returnDescription = "A JSON representation of the workflow statistics.", reponses = {
			@RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow statistics.") })
	public WorkflowStatistics getStatisticsAsJson() throws WorkflowDatabaseException {
		return getStatisticsAsXml();
	}

	@GET
	@Path("definitions.json")
	@Produces(MediaType.APPLICATION_JSON)
	@RestQuery(name = "definitions", description = "List all available workflow definitions as JSON", returnDescription = "Returns the workflow definitions as JSON", reponses = {
			@RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
	public WorkflowDefinitionSet getWorkflowDefinitionsAsJson() throws Exception {
		return getWorkflowDefinitionsAsXml();
	}

	@GET
	@Path("definitions.xml")
	@Produces(MediaType.APPLICATION_XML)
	@RestQuery(name = "definitions", description = "List all available workflow definitions as XML", returnDescription = "Returns the workflow definitions as XML", reponses = {
			@RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
	public WorkflowDefinitionSet getWorkflowDefinitionsAsXml() throws Exception {
		List<WorkflowDefinition> list = service.listAvailableWorkflowDefinitions();
		return new WorkflowDefinitionSet(list);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("definition/{id}.json")
	@RestQuery(name = "definitionasjson", description = "Returns a single workflow definition", returnDescription = "Returns a JSON representation of the workflow definition with the specified identifier", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "The workflow definition."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "Workflow definition not found.") })
	public Response getWorkflowDefinitionAsJson(@PathParam("id") String workflowDefinitionId) throws NotFoundException {
		WorkflowDefinition def = null;
		try {
			def = service.getWorkflowDefinitionById(workflowDefinitionId);
		} catch (WorkflowDatabaseException e) {
			throw new WebApplicationException(e);
		}
		return Response.ok(def).build();
	}

	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("definition/{id}.xml")
	@RestQuery(name = "definitionasxml", description = "Returns a single workflow definition", returnDescription = "Returns an XML representation of the workflow definition with the specified identifier", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "The workflow definition."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "Workflow definition not found.") })
	public Response getWorkflowDefinitionAsXml(@PathParam("id") String workflowDefinitionId) throws NotFoundException {
		return getWorkflowDefinitionAsJson(workflowDefinitionId);
	}

	/**
	 * Returns the workflow configuration panel HTML snippet for the workflow
	 * definition specified by
	 *
	 * @param definitionId
	 * @return config panel HTML snippet
	 */
	@GET
	@Produces(MediaType.TEXT_HTML)
	@Path("configurationPanel")
	@RestQuery(name = "configpanel", description = "Get the configuration panel for a specific workflow", returnDescription = "The HTML workflow configuration panel", restParameters = {
			@RestParameter(name = "definitionId", isRequired = false, description = "The workflow definition identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "The workflow configuration panel.") })
	public Response getConfigurationPanel(@QueryParam("definitionId") String definitionId) throws NotFoundException {
		WorkflowDefinition def = null;
		try {
			def = service.getWorkflowDefinitionById(definitionId);
			String out = def.getConfigurationPanel();
			return Response.ok(out).build();
		} catch (WorkflowDatabaseException e) {
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
	}

	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("instance/{id}.xml")
	@RestQuery(name = "workflowasxml", description = "Get a specific workflow instance.", returnDescription = "An XML representation of a workflow instance", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow instance."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
	public WorkflowInstance getWorkflowAsXml(@PathParam("id") long id)
			throws WorkflowDatabaseException, NotFoundException, UnauthorizedException {
		return service.getWorkflowById(id);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("instance/{id}.json")
	@RestQuery(name = "workflowasjson", description = "Get a specific workflow instance.", returnDescription = "A JSON representation of a workflow instance", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow instance."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
	public WorkflowInstance getWorkflowAsJson(@PathParam("id") long id)
			throws WorkflowDatabaseException, NotFoundException, UnauthorizedException {
		return getWorkflowAsXml(id);
	}

	@POST
	@Path("stop")
	@Produces(MediaType.TEXT_XML)
	@RestQuery(name = "stop", description = "Stops a workflow instance.", returnDescription = "An XML representation of the stopped workflow instance", restParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "An XML representation of the stopped workflow instance."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
	public WorkflowInstance stop(@FormParam("id") long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		return service.stop(workflowInstanceId);
	}

	@DELETE
	@Path("remove/{id}")
	@Produces(MediaType.TEXT_PLAIN)
	@RestQuery(name = "remove", description = "Danger! Permenantly removes a workflow instance including all its child jobs. In most circumstances, /stop is what you should use.", returnDescription = "HTTP 204 No Content", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "If workflow instance could be removed successfully, no content is returned"),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
	public Response remove(@PathParam("id") long workflowInstanceId)
			throws WorkflowException, NotFoundException, UnauthorizedException {
		service.remove(workflowInstanceId);
		return Response.noContent().build();
	}

	@POST
	@Path("suspend")
	@Produces(MediaType.TEXT_XML)
	@RestQuery(name = "suspend", description = "Suspends a workflow instance.", returnDescription = "An XML representation of the suspended workflow instance", restParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_OK, description = "An XML representation of the suspended workflow instance."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
	public Response suspend(@FormParam("id") long workflowInstanceId) throws NotFoundException, UnauthorizedException {
		try {
			WorkflowInstance workflow = service.suspend(workflowInstanceId);
			return Response.ok(workflow).build();
		} catch (WorkflowException e) {
			throw new WebApplicationException(e);
		}
	}

	@POST
	@Path("update")
	@RestQuery(name = "update", description = "Updates a workflow instance.", returnDescription = "No content.", restParameters = {
			@RestParameter(name = "workflow", isRequired = true, description = "The XML representation of the workflow instance.", type = TEXT) }, reponses = {
					@RestResponse(responseCode = SC_NO_CONTENT, description = "Workflow instance updated.") })
	public Response update(@FormParam("workflow") String workflowInstance)
			throws NotFoundException, UnauthorizedException {
		try {
			WorkflowInstance instance = WorkflowParser.parseWorkflowInstance(workflowInstance);
			service.update(instance);
			return Response.noContent().build();
		} catch (WorkflowException e) {
			throw new WebApplicationException(e);
		}
	}

	@GET
	@Path("handlers.json")
	@SuppressWarnings("unchecked")
	@RestQuery(name = "handlers", description = "List all registered workflow operation handlers (implementations).", returnDescription = "A JSON representation of the registered workflow operation handlers.", reponses = {
			@RestResponse(responseCode = SC_OK, description = "A JSON representation of the registered workflow operation handlers") })
	public Response getOperationHandlers() {
		JSONArray jsonArray = new JSONArray();
		for (HandlerRegistration reg : ((WorkflowServiceImpl) service).getRegisteredHandlers()) {
			WorkflowOperationHandler handler = reg.getHandler();
			JSONObject jsonHandler = new JSONObject();
			jsonHandler.put("id", handler.getId());
			jsonHandler.put("description", handler.getDescription());
			JSONObject jsonConfigOptions = new JSONObject();
			for (Entry<String, String> configEntry : handler.getConfigurationOptions().entrySet()) {
				jsonConfigOptions.put(configEntry.getKey(), configEntry.getValue());
			}
			jsonHandler.put("options", jsonConfigOptions);
			jsonArray.add(jsonHandler);
		}
		return Response.ok(jsonArray.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
	}

	@PUT
	@Path("/definition")
	@RestQuery(name = "updatedefinition", description = "Updates a workflow definition.", returnDescription = "A location headers containing the URL to the updated workflow definition.", restParameters = {
			@RestParameter(name = "workflowDefinition", isRequired = true, description = "The XML representation of the updated workflow definition.", type = TEXT) }, reponses = {
					@RestResponse(responseCode = SC_CREATED, description = "Workflow definition updated."),
					@RestResponse(responseCode = SC_PRECONDITION_FAILED, description = "Workflow definition already registered.") })
	public Response registerWorkflowDefinition(
			@FormParam("workflowDefinition") WorkflowDefinitionImpl workflowDefinition) {
		if (workflowDefinition == null)
			return Response.status(Status.BAD_REQUEST).build();

		try {
			service.getWorkflowDefinitionById(workflowDefinition.getId());
			return Response.status(Status.PRECONDITION_FAILED).build(); // the
																		// workflow
																		// definition
																		// should
																		// be
																		// unregistered
		} catch (NotFoundException notFoundException) {
			try {
				service.registerWorkflowDefinition(workflowDefinition);
				return Response
						.created(new URI(UrlSupport
								.concat(new String[] { serverUrl, "definition", workflowDefinition.getId() + ".xml" })))
						.build();
			} catch (WorkflowDatabaseException e) {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			} catch (URISyntaxException e) {
				throw new IllegalStateException("Unable to generate a URI for workflow definitions", e);
			}
		} catch (WorkflowDatabaseException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DELETE
	@Path("/definition/{id}")
	@RestQuery(name = "deletedefinition", description = "Deletes a workflow definition.", returnDescription = "No content.", pathParameters = {
			@RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier.", type = STRING) }, reponses = {
					@RestResponse(responseCode = SC_NO_CONTENT, description = "Workflow definition deleted."),
					@RestResponse(responseCode = SC_NOT_FOUND, description = "Workflow definition not found.") })
	public Response unregisterWorkflowDefinition(@PathParam("id") String workflowDefinitionId)
			throws NotFoundException {
		try {
			service.unregisterWorkflowDefinition(workflowDefinitionId);
			return Response.status(Status.NO_CONTENT).build();
		} catch (NotFoundException e) {
			return Response.status(Status.NOT_FOUND).build();
		} catch (WorkflowDatabaseException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Path("/cleanup")
	@RestQuery(name = "cleanup", description = "Cleans up workflow instances", returnDescription = "No return value", reponses = {
			@RestResponse(responseCode = SC_OK, description = "Cleanup OK"),
			@RestResponse(responseCode = SC_BAD_REQUEST, description = "Couldn't parse given state"),
			@RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to cleanup. Maybe you need to authenticate."),
			@RestResponse(responseCode = SC_FORBIDDEN, description = "It's not allowed to delete other workflow instance statues than STOPPED, SUCCEEDED and FAILED") }, restParameters = {
					@RestParameter(name = "lifetime", type = Type.INTEGER, defaultValue = "30", isRequired = true, description = "Lifetime in days a workflow instance should live"),
					@RestParameter(name = "state", type = Type.STRING, isRequired = true, description = "Workflow instance state, only STOPPED, SUCCEEDED and FAILED are allowed values here") })
	public Response cleanup(@FormParam("lifetime") int lifetime, @FormParam("state") String stateParam)
			throws UnauthorizedException {

		WorkflowInstance.WorkflowState state;
		try {
			state = WorkflowInstance.WorkflowState.valueOf(stateParam);
		} catch (IllegalArgumentException e) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		if (state != WorkflowInstance.WorkflowState.SUCCEEDED && state != WorkflowInstance.WorkflowState.FAILED
				&& state != WorkflowInstance.WorkflowState.STOPPED)
			return Response.status(Status.FORBIDDEN).build();

		try {
			service.cleanupWorkflowInstances(lifetime, state);
			return Response.ok().build();
		} catch (WorkflowDatabaseException e) {
			throw new WebApplicationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected JSONArray getOperationsAsJson(List<WorkflowOperationInstance> operations) {
		JSONArray jsonArray = new JSONArray();
		for (WorkflowOperationInstance op : operations) {
			JSONObject jsOp = new JSONObject();
			jsOp.put("name", op.getTemplate());
			jsOp.put("description", op.getDescription());
			jsOp.put("state", op.getState().name().toLowerCase());
			jsOp.put("configurations", getConfigsAsJson(op));
			jsonArray.add(jsOp);
		}
		return jsonArray;
	}

	@SuppressWarnings("unchecked")
	protected JSONArray getConfigsAsJson(Configurable entity) {
		JSONArray json = new JSONArray();
		Set<String> keys = entity.getConfigurationKeys();
		if (keys != null) {
			for (String key : keys) {
				JSONObject jsConfig = new JSONObject();
				jsConfig.put(key, entity.getConfiguration(key));
				json.add(jsConfig);
			}
		}
		return json;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.rest.AbstractJobProducerEndpoint#getService()
	 */
	@Override
	public JobProducer getService() {
		if (service instanceof JobProducer) {
			return (JobProducer) service;
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.rest.AbstractJobProducerEndpoint#getServiceRegistry()
	 */
	@Override
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}
