package com.capestartproject.common.util.doc.rest;

import javax.ws.rs.core.MediaType;

/**
 * This class extends the javax.ws.rs.core.MediaType. Media types that are returned by our rest endpoints but not
 * present in MediaType is included here.
 */
public class ExtendedMediaType extends MediaType {

  public static final String IMAGE_JPEG = "image/jpeg";

  public static final String TEXT_CALENDAR = "text/calendar";

}
