package com.capestartproject.common.emppackage.identifier;

import java.net.URL;

/**
 * This interface identifies a CNRI handle. Handles are a special type of <code>digital object identifier</code> that
 * allow for permanent localization of digital resources.
 */
public interface Handle extends Id {

  /** The handle protocol identifier */
  String PROTOCOL = "hdl";

  /** The handle prefix */
  String PREFIX = "10.";

  /**
   * Returns the naming authority (or prefix) for the handle.
   *
   * @return the naming authority
   */
  String getNamingAuthority();

  /**
   * Returs the handle local name.
   *
   * @return the handle local name
   */
  String getLocalName();

  /**
   * Returns the full name consisting of authority and local name, e.g.
   * <code>10.3930/ETHZ/AV-0bf9cb23-5535-4c5c-940e-c1a7d91e3191</code>
   */
  String getFullName();

  /**
   * Resolves this handle to the target url by using the configured handle server. If the server cannot be reached, or
   * resolving fails, a {@link HandleException} is thrown.
   *
   * @return the handle target
   */
  URL resolve() throws HandleException;

  /**
   * Updates the value of this handle to the new target url by using the configured handle server. If the server cannot
   * be reached, or updating fails, a {@link HandleException} is thrown.
   */
  void update(URL value) throws HandleException;

}
