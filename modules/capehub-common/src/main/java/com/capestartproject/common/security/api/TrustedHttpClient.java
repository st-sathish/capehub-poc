package com.capestartproject.common.security.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import com.capestartproject.common.util.data.Either;
import com.capestartproject.common.util.data.Function;

/**
 * Provides secured http client components to access to protected resources.
 */
public interface TrustedHttpClient {
  /**
   * Create a function that takes a function to handle the response of the given request returning
   * either a result or any occurred exception.
   * @param httpUriRequest
   *          The http request
   * @return <code>HttpUriRequest -> (HttpResponse -> A) -> Either Exception A</code>
   */
  <A> Function<Function<HttpResponse, A>, Either<Exception, A>> run(HttpUriRequest httpUriRequest);

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
   *
   * @param httpUriRequest
   *          The http request
   * @return the http response returned by the server
   */
  HttpResponse execute(HttpUriRequest httpUriRequest) throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
   * timeouts.
   *
   * @param httpUriRequest
   *          The http request
   * @param connectionTimeout
   *          the wait time in milliseconds at which a connection attempt will throw
   * @param socketTimeout
   *          the maximum time in milliseconds allowed between packets before this method will throw
   * @return the http response returned by the server
   */
  HttpResponse execute(HttpUriRequest httpUriRequest, int connectionTimeout, int socketTimeout)
          throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection.
   *
   * @param httpUriRequest
   *          The http request
   * @return the http response returned by the server
   */
  <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler) throws TrustedHttpClientException;

  /**
   * Executes an HttpRequest using a secure, but not necessarily encrypted, http connection, waiting for the specified
   * timeouts.
   *
   * @param httpUriRequest
   *          The http request
   * @param responseHandler
   *          the response handler
   * @param connectionTimeout
   *          the wait time in milliseconds at which a connection attempt will throw
   * @param socketTimeout
   *          the maximum time in milliseconds allowed between packets before this method will throw
   *
   * @return the http response returned by the server
   */
  <T> T execute(HttpUriRequest httpUriRequest, ResponseHandler<T> responseHandler, int connectionTimeout,
          int socketTimeout) throws TrustedHttpClientException;

  /**
   * Closes this response. The caller must call close() once finished reading the response.
   *
   * @param response
   *          The response to close
   */
  void close(HttpResponse response);
}
