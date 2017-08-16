package com.capestartproject.common.emppackage.identifier;

import com.capestartproject.common.util.ConfigurationException;

/**
 * This class is used to create instances of a handle builder. To specify your own implementation of the handle builder,
 * you simply have to provide the class name of the desired implementation by setting the system property
 * <code>opencast.handlebuilder</code> accordingly.
 */
public final class HandleBuilderFactory {

  /** Class name for the default handle builder */
	private static final String BUILDER_CLASS = "com.capestartproject.employeepackage.identifier.HandleBuilderImpl";

  /** Name of the system property */
	public static final String PROPERTY_NAME = "capehub.handlebuilder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
  private static final HandleBuilderFactory factory = new HandleBuilderFactory();

  /** The default builder implementation */
  private HandleBuilder builder = null;

  /**
   * Private method to create a new handle builder factory.
   */
  private HandleBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a HandleBuilderFactory.
   *
   * @return the handle builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
  public static HandleBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  /**
   * Factory method that returns an instance of a handle builder.
   * <p>
   * It uses the following ordered lookup procedure to determine which implementation of the {@link HandleBuilder}
   * interface to use:
   * <ul>
   * <li>Implementation specified using the <code>opencast.handlebuilder</code> system property</li>
   * <li>Platform default implementation</li>
   * </ul>
   *
   * @return the handle builder
   * @throws ConfigurationException
   *           If the builder cannot be instantiated
   */
  public HandleBuilder newHandleBuilder() throws ConfigurationException {
    if (builder == null) {
      try {
        Class<?> builderClass = Class.forName(builderClassName);
        builder = (HandleBuilder) builderClass.newInstance();
      } catch (ClassNotFoundException e) {
        throw new ConfigurationException("Class not found while creating handle builder: " + e.getMessage(), e);
      } catch (InstantiationException e) {
        throw new ConfigurationException("Instantiation exception while creating handle builder: " + e.getMessage(), e);
      } catch (IllegalAccessException e) {
        throw new ConfigurationException("Access exception while creating handle builder: " + e.getMessage(), e);
      }
    }
    return builder;
  }

}
