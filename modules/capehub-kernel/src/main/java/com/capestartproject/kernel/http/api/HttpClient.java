package com.capestartproject.kernel.http.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/** This interface dictates the required methods for an HttpClient that executes requests. */
public interface HttpClient {

  /** Get the parameters of the http request. */
  HttpParams getParams();

  /** Return the CredentialsProvider that is taking care of this http request. */
  CredentialsProvider getCredentialsProvider();

  /** Executes a http request and returns the response. */
  HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException;

  /** Returns the client connection manager responsible for this request. */
  ClientConnectionManager getConnectionManager();

}
