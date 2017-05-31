package com.capestartproject.common.util.data;

/**
 * <em>Formerly</em> used by {@link Function#apply(Object)} to wrap a checked exception.
 *
 * @deprecated Functions do not use the exception anymore. However it is still here to give client code
 *   the time to remove any uses.
 */
public class FunctionException extends RuntimeException {

  public FunctionException(Throwable throwable) {
    super(throwable);
  }
}
