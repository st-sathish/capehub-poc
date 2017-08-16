package com.capestartproject.common.emppackage.identifier;

import java.util.UUID;

/**
 * Default implementation of an id builder. This implementation yields for a distributed id generator that will create
 * unique ids for the system.
 */
public class UUIDIdBuilderImpl implements IdBuilder {

  /**
   * Creates a new id builder.
   */
  public UUIDIdBuilderImpl() {
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#createNew()
   */
  public Id createNew() {
    return new IdImpl(UUID.randomUUID().toString());
  }

  /**
   * @see org.opencastproject.mediapackage.identifier.IdBuilder#fromString(String)
   */
  public Id fromString(String id) throws IllegalArgumentException {
    if (id == null)
      throw new IllegalArgumentException("Argument 'id' is null");
    try {
      UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw e;
    }
    return new IdImpl(id);
  }

}
