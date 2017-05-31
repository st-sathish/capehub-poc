package com.capestartproject.common.security.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default organization.
 */
public class DefaultOrganization extends JaxbOrganization {

  /** The default organization properties */
  public static final Map<String, String> DEFAULT_PROPERTIES;

  /** Servername - port mappings */
  public static final Map<String, Integer> DEFAULT_SERVERS;

  static {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("logo_large", "/admin/img/mh_logos/MatterhornLogo_large.png");
    properties.put("logo_small", "/admin/img/mh_logos/OpencastLogo.png");
    DEFAULT_PROPERTIES = Collections.unmodifiableMap(properties);

    Map<String, Integer> servers = new HashMap<String, Integer>();
    servers.put("http://localhost", 80);
    DEFAULT_SERVERS = Collections.unmodifiableMap(servers);
  }

  /** The default organization identifier */
  public static final String DEFAULT_ORGANIZATION_ID = "mh_default_org";

  /** The default organization name */
  public static final String DEFAULT_ORGANIZATION_NAME = "Opencast Project";

  /** Name of the default organization's local admin role */
  public static final String DEFAULT_ORGANIZATION_ADMIN = "ROLE_ADMIN";

  /** Name of the default organization's local anonymous role */
  public static final String DEFAULT_ORGANIZATION_ANONYMOUS = "ROLE_ANONYMOUS";

  /**
   * No-arg constructor needed by JAXB
   */
  public DefaultOrganization() {
    super(DefaultOrganization.DEFAULT_ORGANIZATION_ID, DefaultOrganization.DEFAULT_ORGANIZATION_NAME, DEFAULT_SERVERS,
            DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS,
            DEFAULT_PROPERTIES);
  }

  /**
   * Creates a default organization for the given servers.
   *
   * @param servers
   *          the server names and ports
   */
  public DefaultOrganization(Map<String, Integer> servers) {
    super(DefaultOrganization.DEFAULT_ORGANIZATION_ID, DefaultOrganization.DEFAULT_ORGANIZATION_NAME, servers,
            DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS,
            DEFAULT_PROPERTIES);
  }

}
