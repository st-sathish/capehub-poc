package com.capestartproject.helloworld.impl.endpoint;


import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.capestartproject.common.util.doc.rest.RestParameter;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;
import com.capestartproject.helloworld.api.HelloWorldService;

/**
 * The REST endpoint for the {@link HelloWorldService} service
 */
@Path("/")
@RestService(name = "HelloWorldServiceEndpoint",
    title = "Hello World Service Endpoint",
    abstractText = "This is a tutorial service.",
    notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated."
                + "In other words, there is a bug! You should file an error report with your server logs from the time" })
public class HelloWorldRestEndpoint {

  /** The rest docs */
  protected String docs;

  /** The service */
  protected HelloWorldService helloWorldService;

  /**
   * Simple example service call
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @GET
  @Path("helloworld")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "helloworld", description = "example service call",
      reponses = {@RestResponse(description = "Hello World", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "The underlying service could not output something.",
            responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "The text that the service returns.")
  public Response helloWorld() throws Exception {
	  System.out.println("REST call for Hello World");
      return Response.ok().entity(helloWorldService.helloWorld()).build();
  }

  /**
   * Simple example service call with parameter
   *
   * @param name
   *          the optional name of the Person to greet
   * @return A Hello statement with optional name
   * @throws Exception
   */
  @GET
  @Path("helloname")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "helloname", description = "example service call with parameter",
      restParameters = { @RestParameter(description = "name to output", isRequired = false, name = "name",
          type = RestParameter.Type.TEXT) },
      reponses = {@RestResponse(description = "Hello or Hello Name", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The underlying service could not output something.",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "The text that the service returns.")
  public Response helloName(@FormParam("name") String name) throws Exception {
    System.out.println("REST call for Hello Name");
    return Response.ok().entity(helloWorldService.helloName(name)).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  public void setHelloWorldService(HelloWorldService service) {
	  System.out.println("REST call for Hello Name");
	  this.helloWorldService = service;
  }
}
