package com.capehub.helloworld.impl.endpoint;

import com.capehub.helloworld.api.HelloWorldService;
import com.capehub.helloworld.impl.HelloWorldServiceImpl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Test class for Hello World Tutorial
 */
public class HelloWorldRestEndpointTest {

  private HelloWorldRestEndpoint rest;

  /**
   * Setup for the Hello World Rest Service
   */
  @Before
  public void setUp() {
    HelloWorldService service = new HelloWorldServiceImpl();
    rest = new HelloWorldRestEndpoint();
    rest.setHelloWorldService(service);
  }

  @Test
  public void testHelloWorld() throws Exception {
    Response response = rest.helloWorld();
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello World", response.getEntity());
  }

  @Test
  public void testHelloNameEmpty() throws Exception {
    Response response = rest.helloName("");
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello!", response.getEntity());
  }

  @Test
  public void testHelloName() throws Exception {
    Response response = rest.helloName("Capehub");
    Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Assert.assertEquals("Hello Capehub!", response.getEntity());
  }
}
