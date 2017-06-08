package com.capestartproject.helloworld.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.helloworld.api.HelloWorldService;

public class HelloWorldServiceImpl implements HelloWorldService {

  /** The module specific logger */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldServiceImpl.class);

	  public String helloWorld() {
    logger.info("Hello World");
	    return "Hello World";
	  }

	  public String helloName(String name) {
    logger.info("Name is {}.", name);
	    if ("".equals(name)) {
	      return "Hello!";
	    }
	    return "Hello " + name + "!";
	  }
}
