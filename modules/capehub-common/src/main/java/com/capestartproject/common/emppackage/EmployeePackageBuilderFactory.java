package com.capestartproject.common.emppackage;

import com.capestartproject.common.util.ConfigurationException;

/**
 * Factory to retrieve instances of a media package builder. Use the static method {@link #newInstance()} to obtain a
 * reference to a concrete implementation of a <code>MediaPackageBuilderFactory</code>. This instance can then be used
 * to create or load media packages.
 * <p>
 * The factory can be configured by specifying the concrete implementation class through the system property
 * <code>org.opencastproject.mediapackage.builder</code>.
 * </p>
 */
public final class EmployeePackageBuilderFactory {

  /** Class name for the default media package builder */
	private static final String BUILDER_CLASS = "com.capestartproject.employeepackage.EmployeePackageBuilderImpl";

  /** Name of the system property */
	public static final String PROPERTY_NAME = "com.capestartproject.employeepackage.builder";

  /** The implementation class name */
  private static String builderClassName = BUILDER_CLASS;

  /** The singleton instance of this factory */
	private static final EmployeePackageBuilderFactory factory = new EmployeePackageBuilderFactory();

  	/**
	 * Private method to create a new employee package builder factory.
	 */
	private EmployeePackageBuilderFactory() {
    String className = System.getProperty(PROPERTY_NAME);
    if (className != null) {
      builderClassName = className;
    }
  }

  /**
   * Returns an instance of a MediaPackageBuilderFactory.
   *
   * @return the media package builder factory
   * @throws ConfigurationException
   *           if the factory cannot be instantiated
   */
	public static EmployeePackageBuilderFactory newInstance() throws ConfigurationException {
    return factory;
  }

  	/**
	 * Factory method that returns an instance of a employee package builder.
	 * <p>
	 * It uses the following ordered lookup procedure to determine which
	 * implementation of the {@link MediaPackageBuilder} interface to use:
	 * <ul>
	 * <li>Implementation specified using the
	 * <code>org.opencastproject.mediapackage.builder</code> system
	 * property</li>
	 * <li>Platform default implementation</li>
	 * </ul>
	 *
	 * @return the media package builder
	 * @throws ConfigurationException
	 *             If the builder cannot be instantiated
	 */
	public EmployeePackageBuilder newEmpPackageBuilder() throws ConfigurationException {
		EmployeePackageBuilder builder = null;
    try {
      Class<?> builderClass = Class.forName(builderClassName);
			builder = (EmployeePackageBuilder) builderClass.newInstance();
    } catch (ClassNotFoundException e) {
			throw new ConfigurationException(
					"Class not found while creating employee package builder: " + e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new ConfigurationException("Instantiation exception while creating media package builder: "
              + e.getMessage(), e);
    } catch (IllegalAccessException e) {
			throw new ConfigurationException(
					"Access exception while creating employee package builder: " + e.getMessage(), e);
    }
    return builder;
  }

}
