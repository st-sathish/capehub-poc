package com.capestartproject.serviceregistry.impl.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.capestartproject.common.rest.OsgiAbstractJobProducerEndpoint;
import com.capestartproject.common.util.RestUtil;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;
import com.capestartproject.serviceregistry.impl.NopServiceImpl;

@Path("/")
@RestService(
        name = "nopservice",
        title = "Nop Service",
        notes = {},
        abstractText = "No operation service. Creates empty jobs for testing purposes.")
public class NopServiceEndpoint extends OsgiAbstractJobProducerEndpoint<NopServiceImpl> {
  @GET
  @Path("nop")
  @RestQuery(
          name = "nop",
          description = "Create an empty job for testing purposes.",
          returnDescription = "The service statistics.",
          reponses = { @RestResponse(responseCode = SC_OK, description = "OK") })
  public Response nop() {
    return RestUtil.R.ok(getSvc().nop());
  }
}
