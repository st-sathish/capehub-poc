package com.capestartproject.workspace.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import com.capestartproject.common.storage.StorageUsage;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.common.util.data.Option;

/**
 * Provides efficient access java.io.File objects from potentially remote URIs. This helper service prevents different
 * service implementations running in the same osgi container from downloading remote files multiple times.
 *
 * Additionally, when the system is configured to use shared storage, this performance gain is also achieved across
 * distributed osgi containers. The methods from WorkingFileRepository are also available as a convenience to clients.
 */
public interface Workspace extends StorageUsage {

  /**
   * Gets a locally cached {@link File} for the given URI.
   *
   * @param uri
   * @return The locally cached file
   * @throws NotFoundException
   *           if the file does not exist
   * @throws IOException
   *           if reading the file from the workspace fails
   */
  File get(URI uri) throws NotFoundException, IOException;

  /**
   * Gets the base URI for files stored using this service.
   *
   * @return The base URI
   */
  URI getBaseUri();

  	/**
	 * Store the data stream under the given employee package and element IDs,
	 * specifying a filename.
	 *
	 * @param empPackageID
	 * @param empPackageElementID
	 * @param fileName
	 * @param in
	 * @throws IOException
	 *             if writing the data to the workspace fails
	 * @throws IllegalArgumentException
	 *             if a URI cannot be created using the arguments provided
	 */
	URI put(String empPackageID, String empPackageElementID, String fileName, InputStream in) throws IOException,
          IllegalArgumentException;

  /**
   * Stores the data stream in the given collection, overwriting any data with the same collection id and file name.
   *
   * @param collectionId
   *          The collection to use for storing this data
   * @param fileName
   *          the filename to use in the collection.
   * @param in
   *          the inputstream
   * @return the URI of the stored data
   * @throws IOException
   *           if writing the data to the workspace fails
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException,
          IllegalArgumentException;

  /**
   * Gets the URIs of the members of this collection
   *
   * @param collectionId
   *          the collection identifier
   * @return the URIs for each member of the collection
   * @throws NotFoundException
   *           if the collection cannot be found
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI[] getCollectionContents(String collectionId) throws NotFoundException, IllegalArgumentException;

  /**
   * Delete the file stored at the given uri.
   *
   * @param uri
   *          the uri
   * @throws NotFoundException
   *           if there was not file stored under this combination of mediapackage and element IDs.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void delete(URI uri) throws NotFoundException, IOException;

  	/**
	 * Delete the file stored at the given employee package and element IDs.
	 *
	 * @param empPackageID
	 * @param empPackageElementID
	 * @throws NotFoundException
	 *             if there was not file stored under this combination of
	 *             mediapackage and element IDs.
	 * @throws IOException
	 *             if deleting the data from the workspace fails
	 */
	void delete(String empPackageID, String empPackageElementID) throws NotFoundException, IOException;

  /**
   * Removes a file from a collection
   *
   * @param collectionId
   *          the collection identifier
   * @param fileName
   *          the filename to remove
   * @throws NotFoundException
   *           if there was not file stored under this combination of mediapackage and element IDs.
   * @throws IOException
   *           if deleting the data from the workspace fails
   */
  void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException;

  	/**
	 * Get the URL for a file stored under the given media package and element
	 * IDs. MediaPackages may reference elements that are not yet stored in the
	 * working file repository, so this method will return a URI even if the
	 * file is not yet stored.
	 *
	 * @deprecated Please use {@link #getURI(String, String, String)} instead
	 * @param mediaPackageID
	 *            the emppackage identifier
	 * @param empPackageElementID
	 *            the element identifier
	 * @return the URI to the file
	 * @throws IllegalArgumentException
	 *             if a URI cannot be created using the arguments provided
	 */
	URI getURI(String empPackageID, String empPackageElementID) throws IllegalArgumentException;

  	/**
	 * Get the URL for a file stored under the given media package and element
	 * IDs. MediaPackages may reference elements that are not yet stored in the
	 * working file repository, so this method will return a URI even if the
	 * file is not yet stored.
	 *
	 * @param empPackageID
	 *            the emppackage identifier
	 * @param mediaPackageElementID
	 *            the element identifier
	 * @param filename
	 *            the filename
	 * @return the URI to the file
	 * @throws IllegalArgumentException
	 *             if a URI cannot be created using the arguments provided
	 */
	URI getURI(String empPackageID, String empPackageElementID, String filename) throws IllegalArgumentException;

  /**
   * Get the URL for a file stored under the given collection.
   *
   * @param collectionID
   *          the collection id
   * @param fileName
   *          the file name
   * @return the file's uri
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  URI getCollectionURI(String collectionID, String fileName) throws IllegalArgumentException;

  	/**
	 * Moves a file from a collection into a mediapackage
	 *
	 * @param collectionURI
	 *            the uri pointing to a workspace collection
	 * @param toEmployeePackage
	 *            The employee package ID to move the file into
	 * @param toEmployeePackageElement
	 *            the employee package element ID of the file
	 * @param toFileName
	 *            the name of the resulting file
	 * @return the URI pointing to the file's new location
	 * @throws NotFoundException
	 *             if the element identified by <code>collectionURI</code>
	 *             cannot be found
	 * @throws IOException
	 *             if either the original element cannot be read or it cannot be
	 *             moved to the new location
	 * @throws IllegalArgumentException
	 *             if a URI cannot be created using the arguments provided
	 */
	URI moveTo(URI collectionURI, String toEmployeePackage, String toEmployeePackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException;

  	/**
	 * Copies a file from a collection into a employeepackage
	 *
	 * @param collectionURI
	 *            The uri pointing to a workspace collection
	 * @param toEmployeePackage
	 *            The employee package ID to copy the file into
	 * @param toEmployeePackageElement
	 *            the employee package element ID of the file
	 * @param toFileName
	 *            the name of the resulting file
	 * @return the URI pointing to the file's new location
	 * @throws NotFoundException
	 *             if the element identified by <code>collectionURI</code>
	 *             cannot be found
	 * @throws IOException
	 *             if either the original element cannot be read or the copy
	 *             cannot be written to the new location
	 * @throws IllegalArgumentException
	 *             if a URI cannot be created using the arguments provided
	 */
	URI copyTo(URI collectionURI, String toEmployeePackage, String toEmployeePackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException;

  	/**
	 * Cleans up files not belonging to a employeepackage or a collection. If
	 * the optional maxAge parameter is set, only files older than the maxAge
	 * are deleted.
	 *
	 * @param maxAge
	 *            the maximal age in seconds of a file before deletion is
	 *            performed
	 */
  void cleanup(Option<Integer> maxAge);

}
