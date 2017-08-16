package com.capestartproject.common.emppackage;

import java.net.URI;
import java.net.URISyntaxException;

public interface EmployeePackageSerializer {

  /**
   * This method is called every time a url is being written to a media package manifest. By implementing this method,
   * serializers are able to store package elements in directories relative to some common root folder, thereby making
   * it movable.
   *
   * @param uri
   *          the url to encode
   * @return the encoded path
   */
  String encodeURI(URI uri);

  /**
   * This method is called every time a url is being read from a media package manifest. By implementing this method,
   * serializers are able to redirect urls to local caches which might make sense when it comes to dealing with huge
   * media files.
   *
   * @param path
   *          the original path from the manifest
   * @return the resolved url
   * @throws URISyntaxException
   *           if the path cannot be converted into a url
   */
  URI resolvePath(String path) throws URISyntaxException;

}
