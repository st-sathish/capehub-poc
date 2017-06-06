package com.capestartproject.runtimeinfo.rest;

import com.capestartproject.common.util.doc.DocData;

import java.util.List;
import java.util.Vector;

@Deprecated
public class RestEndpointHolder {
  private String name;
  private String title;
  private List<RestEndpoint> endpoints;

  public RestEndpointHolder(String name, String title) {
    if (!DocData.isValidName(name)) {
      throw new IllegalArgumentException("name must not be null and must be alphanumeric");
    }
    if (title == null) {
      throw new IllegalArgumentException("title must not be null");
    }
    this.name = name;
    this.title = title;
  }

  public void addEndPoint(RestEndpoint endpoint) {
    if (endpoint != null) {
      if (this.endpoints == null) {
        this.endpoints = new Vector<RestEndpoint>();
      }
      this.endpoints.add(endpoint);
    }
  }

  @Override
  public String toString() {
    return "HOLD:" + name + ":" + endpoints;
  }

  /**
   * @return a copy of this object
   */
  public RestEndpointHolder duplicate() {
    return new RestEndpointHolder(this.name, this.title);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return duplicate();
  }

  public String getName() {
    return name;
  }

  public String getTitle() {
    return title;
  }

  public List<RestEndpoint> getEndpoints() {
    if (endpoints == null) {
      endpoints = new Vector<RestEndpoint>(0);
    }
    return endpoints;
  }

}
