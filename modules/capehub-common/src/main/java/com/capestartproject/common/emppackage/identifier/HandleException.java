package com.capestartproject.common.emppackage.identifier;

/**
 * General exception that is raised when problems occur while retreiving or dealing with handles.
 */
public class HandleException extends Exception {

  /** Serial version uid */
  private static final long serialVersionUID = 1485516511882283397L;

  /**
   * Creates a new handle exception with the specified message.
   *
   * @param msg
   *          the error message
   */
  public HandleException(String msg) {
    super(msg);
  }

  /**
   * Creates a new handle exception caused by Throwable <code>t</code>.
   *
   * @param t
   *          the original exception
   */
  public HandleException(Throwable t) {
    super(t.getMessage(), t);
  }

  /**
   * Creates a new handle exception caused by Throwable <code>t</code>.
   *
   * @param msg
   *          individual error message
   * @param t
   *          the original exception
   */
  public HandleException(String msg, Throwable t) {
    super(msg, t);
  }

}
