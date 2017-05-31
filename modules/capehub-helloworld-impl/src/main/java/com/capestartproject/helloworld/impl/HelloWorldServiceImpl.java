package com.capestartproject.helloworld.impl;


import com.capestartproject.helloworld.api.HelloWorldService;

public class HelloWorldServiceImpl implements HelloWorldService {

	  public String helloWorld() {
	    System.out.println("Hello World");
	    return "Hello World";
	  }

	  public String helloName(String name) {
	    System.out.println("Name is "+name+".");
	    if ("".equals(name)) {
	      return "Hello!";
	    }
	    return "Hello " + name + "!";
	  }
}
