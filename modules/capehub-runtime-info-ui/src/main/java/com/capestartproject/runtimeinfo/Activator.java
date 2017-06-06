package com.capestartproject.runtimeinfo;

import static com.capestartproject.common.rest.RestConstants.SERVICES_FILTER;
import static com.capestartproject.common.rest.RestConstants.SERVICE_PATH_PROPERTY;
import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.Option.some;

import com.capestartproject.runtimeinfo.rest.RestDocData;
import com.capestartproject.common.systems.CapehubConstans;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.doc.DocUtil;
import com.capestartproject.common.util.doc.rest.RestQuery;
import com.capestartproject.common.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/** A bundle activator that registers the REST documentation servlet. */
public class Activator extends HttpServlet implements BundleActivator {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);

  /** The query string parameter used to specify a specific service */
  private static final String PATH_PARAM = "path";

  /** java.io serialization UID */
  private static final long serialVersionUID = 6930336096831297329L;

  /** The OSGI bundle context */
  protected BundleContext bundleContext;

  /** The registration for the documentation servlet. */
  protected ServiceRegistration docServletRegistration;

  /** A map of global macro values for REST documentation. */
  private Map<String, String> globalMacro;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    this.bundleContext = bundleContext;
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("alias", "/docs.html");
    prepareMacros();
    bundleContext.registerService(Servlet.class.getName(), this, props);
  }

  /** Add a list of global information, such as the server URL, to the globalMacro map. */
  private void prepareMacros() {
    globalMacro = new HashMap<String, String>();
    globalMacro.put("PING_BACK_URL", bundleContext.getProperty("com.capestartproject.anonymous.feedback.url"));
    globalMacro.put("HOST_URL", bundleContext.getProperty(CapehubConstans.SERVER_URL_PROPERTY));
    globalMacro.put("LOCAL_STORAGE_DIRECTORY", bundleContext.getProperty("org.opencastproject.storage.dir"));
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String docPath = req.getParameter(PATH_PARAM);
    if (StringUtils.isBlank(docPath)) {
      resp.sendRedirect("rest_docs.html");
    } else {
      // write the details for this service
      writeServiceDocumentation(docPath, req, resp);
    }
  }

  private void writeServiceDocumentation(final String docPath, HttpServletRequest req, HttpServletResponse resp)
          throws IOException {
    ServiceReference reference = null;
    for (ServiceReference ref : getRestEndpointServices()) {
      String alias = (String) ref.getProperty(SERVICE_PATH_PROPERTY);
      if (docPath.equalsIgnoreCase(alias)) {
        reference = ref;
        break;
      }
    }

    final StringBuilder docs = new StringBuilder();

    if (reference == null) {
      docs.append("REST docs unavailable for ");
      docs.append(docPath);
    } else {
      final Object restService = bundleContext.getService(reference);
      findRestAnnotation(restService.getClass()).fold(new Option.Match<RestService, Void>() {
        @Override
        public Void some(RestService annotation) {
          globalMacro.put("SERVICE_CLASS_SIMPLE_NAME", restService.getClass().getSimpleName());
          RestDocData data = new RestDocData(annotation.name(), annotation.title(), docPath, annotation.notes(),
                  restService, globalMacro);
          data.setAbstract(annotation.abstractText());

          for (Method m : restService.getClass().getMethods()) {
            RestQuery rq = (RestQuery) m.getAnnotation(RestQuery.class);
            String httpMethodString = null;
            for (Annotation a : m.getAnnotations()) {
              HttpMethod httpMethod = (HttpMethod) a.annotationType().getAnnotation(HttpMethod.class);
              if (httpMethod != null) {
                httpMethodString = httpMethod.value();
              }
            }
            Produces produces = (Produces) m.getAnnotation(Produces.class);
            Path path = (Path) m.getAnnotation(Path.class);
            Class<?> returnType = m.getReturnType();
            if ((rq != null) && (httpMethodString != null) && (path != null)) {
              data.addEndpoint(rq, returnType, produces, httpMethodString, path);
            }
          }
          String template = DocUtil.loadTemplate("/ui/restdocs/template.xhtml");
          docs.append(DocUtil.generate(data, template));
          return null;
        }

        @Override
        public Void none() {
          docs.append("No documentation has been found for ").append(restService.getClass().getSimpleName());
          return null;
        }
      });
    }

    resp.setContentType("text/html");
    resp.getWriter().write(docs.toString());
  }

  private ServiceReference[] getRestEndpointServices() {
    try {
      return bundleContext.getAllServiceReferences(null, SERVICES_FILTER);
    } catch (InvalidSyntaxException e) {
      logger.warn("Unable to query the OSGI service registry for all registered rest endpoints");
      return new ServiceReference[0];
    }
  }

  /** Try to find the RestService annotation starting at <code>endpointClass</code>. */
  public static Option<RestService> findRestAnnotation(Class<?> endpointClass) {
    if (endpointClass == null) {
      return none();
    }
    final RestService rs = endpointClass.getAnnotation(RestService.class);
    if (rs == null) {
      return findRestAnnotation(endpointClass.getSuperclass());
    } else {
      return some(rs);
    }
  }
}
