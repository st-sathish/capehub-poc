package com.capestartproject.common.emppackage.identifier;

/**
 * Interface that describes the methods of an id builder.
 */
public interface IdBuilder {

  /**
   * Creates a new identifier. The identifier is supposed to be unique within a running system.
   * <p>
   * The default implementation will return a uuid-style identifier.
   * </p>
   *
   * @return the new identifier
   */
  Id createNew();

  /**
   * This method can be used to determine if <code>id</code> is in fact a vaild identifier as expected by this id
   * builder. If this is not the case, an {@link IllegalArgumentException} is thrown.
   *
   * @return the id
   * @throws IllegalArgumentException
   *           if the identifier is malformed
   */
  Id fromString(String id) throws IllegalArgumentException;

}
