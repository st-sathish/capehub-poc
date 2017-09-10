package com.capestartproject.workingfilerepository.api;

/**
 * Extension to the working file repository that allows to map a file url to a file system path.
 */
public interface PathMappable {

  /**
   * Returns the prefix to the working file repository file system root directory.
   *
   * @return the root path on the file system
   */
  String getPathPrefix();

  /**
   * Returns the repository's base url.
   *
   * @return the base url
   */
  String getUrlPrefix();

}
