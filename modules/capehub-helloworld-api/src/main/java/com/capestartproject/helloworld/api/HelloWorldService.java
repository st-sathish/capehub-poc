package com.capestartproject.helloworld.api;

public interface HelloWorldService {

	/**
	   * Outputs "Hello World!"
	   *
	   * @return String with the text
	   */
	  String helloWorld();

	  /**
	   * Outputs "Hello!" or "Hello " + name
	   *
	   * @param name
	   *          name of the person to greet
	   * @return String with the text
	   */
	  String helloName(String name);
}
