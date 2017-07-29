package com.capestartproject.common.util;

import static com.capestartproject.common.util.data.Monadics.mlist;
import static com.capestartproject.common.util.data.Option.option;
import static com.capestartproject.common.util.data.Tuple.tuple;
import static com.capestartproject.common.util.data.functions.Strings.split;
import static com.capestartproject.common.util.data.functions.Strings.trimToNil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.component.ComponentContext;

import com.capestartproject.common.job.api.JaxbJob;
import com.capestartproject.common.job.api.Job;
import com.capestartproject.common.rest.RestConstants;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Monadics;
import com.capestartproject.common.util.data.Option;
import com.capestartproject.common.util.data.Tuple;

/** Utility functions for REST endpoints. */
public final class RestUtil {
  private RestUtil() {
  }

  /**
   * Return the endpoint's server URL and the service path by extracting the relevant parameters from the
   * ComponentContext.
   *
   * @return (serverUrl, servicePath)
   * @throws Error
   *           if the service path is not configured for this component
   */
  public static Tuple<String, String> getEndpointUrl(ComponentContext cc) {
    final String serverUrl = option(cc.getBundleContext().getProperty("org.opencastproject.server.url")).getOrElse(
            UrlSupport.DEFAULT_BASE_URL);
    final String servicePath = option((String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY)).getOrElse(
            Option.<String> error(RestConstants.SERVICE_PATH_PROPERTY + " property not configured"));
    return tuple(serverUrl, servicePath);
  }

  /** Create a file response. */
  public static Response.ResponseBuilder fileResponse(File f, String contentType, Option<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(f).header("Content-Type", contentType)
            .header("Content-Length", f.length());
    for (String fn : fileName)
      b.header("Content-Disposition", "attachment; filename=" + fn);
    return b;
  }

  /**
   * create a partial file response
   *
   * @param f
   *          the requested file
   * @param contentType
   *          the contentType to send
   * @param fileName
   *          the filename to send
   * @param rangeHeader
   *          the range header
   * @return the Responsebuilder
   * @throws IOException
   *           if something goes wrong
   */
  public static Response.ResponseBuilder partialFileResponse(File f, String contentType, Option<String> fileName,
          String rangeHeader) throws IOException {

    String rangeValue = rangeHeader.trim().substring("bytes=".length());
    long fileLength = f.length();
    long start;
    long end;
    if (rangeValue.startsWith("-")) {
      end = fileLength - 1;
      start = fileLength - 1 - Long.parseLong(rangeValue.substring("-".length()));
    } else {
      String[] range = rangeValue.split("-");
      start = Long.parseLong(range[0]);
      end = range.length > 1 ? Long.parseLong(range[1]) : fileLength - 1;
    }
    if (end > fileLength - 1) {
      end = fileLength - 1;
    }

    // send partial response status code
    Response.ResponseBuilder response = Response.status(206);

    if (start <= end) {
      long contentLength = end - start + 1;
      response.header("Accept-Ranges", "bytes");
      response.header("Connection", "Close");
      response.header("Content-Length", contentLength + "");
      response.header("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
      response.header("Content-Type", contentType);
      response.entity(new ChunkedFileInputStream(f, start, end));
    }

    return response;
  }

  /** Create a stream response. */
  public static Response.ResponseBuilder streamResponse(InputStream in, String contentType, Option<Long> streamLength,
          Option<String> fileName) {
    final Response.ResponseBuilder b = Response.ok(in).header("Content-Type", contentType);
    for (Long l : streamLength)
      b.header("Content-Length", l);
    for (String fn : fileName)
      b.header("Content-Disposition", "attachment; filename=" + fn);
    return b;
  }

  /**
   * Return JSON if <code>format</code> == json, XML else.
   *
   * @deprecated use {@link #getResponseType(String)}
   */
  public static MediaType getResponseFormat(String format) {
    return "json".equalsIgnoreCase(format) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
  }

  /** Return JSON if <code>type</code> == json, XML else. */
  public static MediaType getResponseType(String type) {
    return "json".equalsIgnoreCase(type) ? MediaType.APPLICATION_JSON_TYPE : MediaType.APPLICATION_XML_TYPE;
  }

  private static final Function<String, String[]> CSV_SPLIT = split(Pattern.compile(","));

  /**
   * Split a comma separated request param into a list of trimmed strings discarding any blank parts.
   * <p/>
   * x=comma,separated,,%20value -&gt; ["comma", "separated", "value"]
   */
  public static Monadics.ListMonadic<String> splitCommaSeparatedParam(Option<String> param) {
    for (String p : param)
      return mlist(CSV_SPLIT.apply(p)).bind(trimToNil);
    return mlist();
  }

  /** Response builder functions. */
  public static final class R {
    private R() {
    }

    public static Response ok() {
      return Response.ok().build();
    }

    public static Response ok(Object entity) {
      return Response.ok().entity(entity).build();
    }

    public static Response ok(boolean entity) {
      return Response.ok().entity(Boolean.toString(entity)).build();
    }

    public static Response ok(Jsons.Obj json) {
      return Response.ok().entity(json.toJson()).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public static Response ok(Job job) {
      return Response.ok().entity(new JaxbJob(job)).build();
    }

    public static Response ok(MediaType type, Object entity) {
      return Response.ok(entity, type).build();
    }

    public static Response notFound() {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response notFound(Object entity) {
      return Response.status(Response.Status.NOT_FOUND).entity(entity).build();
    }

    public static Response notFound(Object entity, MediaType type) {
      return Response.status(Response.Status.NOT_FOUND).entity(entity).type(type).build();
    }

    public static Response serverError() {
      return Response.serverError().build();
    }

    public static Response conflict() {
      return Response.status(Response.Status.CONFLICT).build();
    }

    public static Response noContent() {
      return Response.noContent().build();
    }

    public static Response badRequest() {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    public static Response badRequest(String msg) {
      return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
    }
  }
}
