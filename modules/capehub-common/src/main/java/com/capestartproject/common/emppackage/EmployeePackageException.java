package com.capestartproject.common.emppackage;

/**
 * General exception that is raised when problems occur while manipulating
 * employee packages like adding or removing media package elements, creating
 * manifests or moving and copying the media package itself.
 */
public class EmployeePackageException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = -1645569283274593366L;

  	/**
	 * Creates a new employee package exception with the specified message.
	 *
	 * @param msg
	 *            the error message
	 */
	public EmployeePackageException(String msg) {
    super(msg);
  }

  	/**
	 * Creates a new employee package exception caused by Throwable
	 * <code>t</code>.
	 *
	 * @param t
	 *            the original exception
	 */
	public EmployeePackageException(Throwable t) {
    super(t.getMessage(), t);
  }

  	/**
	 * Creates a new employee package exception caused by Throwable
	 * <code>t</code>.
	 *
	 * @param msg
	 *            individual error message
	 * @param t
	 *            the original exception
	 */
	public EmployeePackageException(String msg, Throwable t) {
    super(msg, t);
  }

}
