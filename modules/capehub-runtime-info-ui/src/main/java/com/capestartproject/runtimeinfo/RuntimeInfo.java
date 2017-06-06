package com.capestartproject.runtimeinfo;

import static com.capestartproject.common.rest.RestConstants.SERVICES_FILTER;

import com.capestartproject.common.rest.RestConstants;
import com.capestartproject.common.security.api.Organization;
import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.security.api.SecurityService;
import com.capestartproject.common.security.api.User;
import com.capestartproject.common.systems.CapehubConstans;
import com.capestartproject.common.util.UrlSupport;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestResponse;
import com.capestartproject.common.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * This REST endpoint provides information about the runtime environment, including the services and user interfaces
 * deployed and the current login context.
 *
 * If the 'com.capestartproject.anonymous.feedback.url' is set in config.properties, this service will also update the
 * Capehub project with the contents of the {@link #getRuntimeInfo()} json feed.
 */
@Path("/")
@RestService(name = "RuntimeInfo", title = "Runtime Information", notes = "", abstractText = "This service provides information about the runtime environment, including the servives that are deployed and the current user context.")
public class RuntimeInfo {

  private static final Logger logger = LoggerFactory.getLogger(RuntimeInfo.class);

  /** Configuration properties id */
  private static final String ADMIN_URL_PROPERTY = "com.capestartproject.admin.ui.url";
  private static final String ENGAGE_URL_PROPERTY = "com.capestartproject.engage.ui.url";

  /**
   * The rest publisher looks for any non-servlet with the 'capestart.service.path' property
   */
  public static final String SERVICE_FILTER = "(&(!(objectClass=javax.servlet.Servlet))("
          + RestConstants.SERVICE_PATH_PROPERTY + "=*))";

  private SecurityService securityService;
  private BundleContext bundleContext;
  private URL serverUrl;

  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  protected ServiceReference[] getRestServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(null, SERVICES_FILTER);
  }

  protected ServiceReference[] getUserInterfaceServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(Servlet.class.getName(), "(&(alias=*)(classpath=*))");
  }

  public void activate(ComponentContext cc) throws MalformedURLException {
    logger.debug("start()");
    this.bundleContext = cc.getBundleContext();
    serverUrl = new URL(bundleContext.getProperty(CapehubConstans.SERVER_URL_PROPERTY));
  }

  public void deactivate() {
    // Nothing to do
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("components.json")
  @RestQuery(name = "services", description = "List the REST services and user interfaces running on this host", reponses = { @RestResponse(description = "The components running on this host", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  @SuppressWarnings("unchecked")
  public String getRuntimeInfo(@Context HttpServletRequest request) throws MalformedURLException {
    Organization organization = securityService.getOrganization();

    // Get request protocol and port
    String targetScheme = request.getScheme();

    // Create the engage target URL
    URL targetEngageBaseUrl = null;
    String orgEngageBaseUrl = organization.getProperties().get(ENGAGE_URL_PROPERTY);
    if (StringUtils.isNotBlank(orgEngageBaseUrl)) {
      try {
        targetEngageBaseUrl = new URL(orgEngageBaseUrl);
      } catch (MalformedURLException e) {
        logger.warn("Engage url '{}' of organization '{}' is malformed", orgEngageBaseUrl, organization.getId());
      }
    }

    if (targetEngageBaseUrl == null) {
      logger.debug(
              "Using 'org.opencastproject.server.url' as a fallback for the non-existing organization level key '{}' for the components.json response",
              ENGAGE_URL_PROPERTY);
      targetEngageBaseUrl = new URL(targetScheme, serverUrl.getHost(), serverUrl.getPort(), serverUrl.getFile());
    }

    // Create the admin target URL
    URL targetAdminBaseUrl = null;
    String orgAdminBaseUrl = organization.getProperties().get(ADMIN_URL_PROPERTY);
    if (StringUtils.isNotBlank(orgAdminBaseUrl)) {
      try {
        targetAdminBaseUrl = new URL(orgAdminBaseUrl);
      } catch (MalformedURLException e) {
        logger.warn("Admin url '{}' of organization '{}' is malformed", orgAdminBaseUrl, organization.getId());
      }
    }

    if (targetAdminBaseUrl == null) {
      logger.debug(
              "Using 'org.opencastproject.server.url' as a fallback for the non-existing organization level key '{}' for the components.json response",
              ADMIN_URL_PROPERTY);
      targetAdminBaseUrl = new URL(targetScheme, serverUrl.getHost(), serverUrl.getPort(), serverUrl.getFile());
    }

    JSONObject json = new JSONObject();
    json.put("engage", targetEngageBaseUrl.toString());
    json.put("admin", targetAdminBaseUrl.toString());
    json.put("rest", getRestEndpointsAsJson(request));
    json.put("ui", getUserInterfacesAsJson());

    return json.toJSONString();
  }

  @GET
  @Path("me.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "me", description = "Information about the curent user", reponses = { @RestResponse(description = "Returns information about the current user", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  @SuppressWarnings("unchecked")
  public String getMyInfo() {
    JSONObject json = new JSONObject();

    User user = securityService.getUser();
    json.put("username", user.getUsername());

    // Add the current user's roles
    JSONArray roles = new JSONArray();
    for (Role role : user.getRoles()) {
      roles.add(role.getName());
    }
    json.put("roles", roles);

    // Add the current user's organizational information
    Organization org = securityService.getOrganization();

    JSONObject jsonOrg = new JSONObject();
    jsonOrg.put("id", org.getId());
    jsonOrg.put("name", org.getName());
    jsonOrg.put("adminRole", org.getAdminRole());
    jsonOrg.put("anonymousRole", org.getAnonymousRole());

    // and organization properties
    JSONObject orgProps = new JSONObject();
    jsonOrg.put("properties", orgProps);
    for (Entry<String, String> entry : org.getProperties().entrySet()) {
      orgProps.put(entry.getKey(), entry.getValue());
    }
    json.put("org", jsonOrg);

    return json.toJSONString();
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getRestEndpointsAsJson(HttpServletRequest request) throws MalformedURLException {
    JSONArray json = new JSONArray();
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = getRestServiceReferences();
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null)
      return json;
    for (ServiceReference servletRef : sort(serviceRefs)) {
      String version = servletRef.getBundle().getVersion().toString();
      String description = (String) servletRef.getProperty(Constants.SERVICE_DESCRIPTION);
      String type = (String) servletRef.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
      String servletContextPath = (String) servletRef.getProperty(RestConstants.SERVICE_PATH_PROPERTY);
      JSONObject endpoint = new JSONObject();
      endpoint.put("description", description);
      endpoint.put("version", version);
      endpoint.put("type", type);
      URL url = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), servletContextPath);
      endpoint.put("path", servletContextPath);
      endpoint.put("docs", UrlSupport.concat(url.toExternalForm(), "/docs")); // This is a Matterhorn convention
      endpoint.put("wadl", UrlSupport.concat(url.toExternalForm(), "/?_wadl&_type=xml")); // This triggers a
      json.add(endpoint);
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getUserInterfacesAsJson() {
    JSONArray json = new JSONArray();
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = getUserInterfaceServiceReferences();
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null)
      return json;
    for (ServiceReference ref : sort(serviceRefs)) {
      String description = (String) ref.getProperty(Constants.SERVICE_DESCRIPTION);
      String version = ref.getBundle().getVersion().toString();
      String alias = (String) ref.getProperty("alias");
      String welcomeFile = (String) ref.getProperty("welcome.file");
      String welcomePath = "/".equals(alias) ? alias + welcomeFile : alias + "/" + welcomeFile;
      JSONObject endpoint = new JSONObject();
      endpoint.put("description", description);
      endpoint.put("version", version);
      endpoint.put("welcomepage", serverUrl + welcomePath);
      json.add(endpoint);
    }
    return json;
  }

  /**
   * Returns the array of references sorted by their {@link Constants.SERVICE_DESCRIPTION} property.
   *
   * @param references
   *          the referencens
   * @return the sorted set of references
   */
  protected static SortedSet<ServiceReference> sort(ServiceReference[] references) {
    // Sort the service references
    SortedSet<ServiceReference> sortedServiceRefs = new TreeSet<ServiceReference>(new Comparator<ServiceReference>() {
      @Override
      public int compare(ServiceReference o1, ServiceReference o2) {
        String o1Description = (String) o1.getProperty(Constants.SERVICE_DESCRIPTION);
        if (StringUtils.isBlank(o1Description))
          o1Description = o1.toString();
        String o2Description = (String) o2.getProperty(Constants.SERVICE_DESCRIPTION);
        if (StringUtils.isBlank(o2Description))
          o2Description = o2.toString();
        return o1Description.compareTo(o2Description);
      }
    });
    sortedServiceRefs.addAll(Arrays.asList(references));
    return sortedServiceRefs;
  }
}
