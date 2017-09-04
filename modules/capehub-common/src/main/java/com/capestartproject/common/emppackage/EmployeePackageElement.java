package com.capestartproject.common.emppackage;

import java.net.URI;
import java.util.Collection;

import javax.activation.MimeType;

import com.capestartproject.common.util.Checksum;

/**
 * All classes that will be part of a media package must implement this interface.
 */
public interface EmployeePackageElement extends ManifestContributor, Comparable<EmployeePackageElement>, Cloneable {

  /**
   * The element type todo is the type definitely needed or can the flavor take its responsibilities?
   */
  enum Type {
    Manifest, Timeline, Track, Catalog, Attachment, Publication, Other
  }

  /**
   * Returns the element identifier.
   *
   * @return the element identifier
   */
  String getIdentifier();

  /**
   * Sets the element identifier.
   *
   * @param id
   *          the new element identifier
   */
  void setIdentifier(String id);

  /**
   * Returns the element's manifest type.
   *
   * @return the manifest type
   */
  Type getElementType();

  /**
   * Returns a human readable name for this media package element. If no name was provided, the filename is returned
   * instead.
   *
   * @return the element name
   */
  String getElementDescription();

  /**
   * Sets the element description of this media package element.
   *
   * @param description
   *          the new element description
   */
  void setElementDescription(String description);

  /**
   * Tags the media package element with the given tag.
   *
   * @param tag
   *          the tag
   */
  void addTag(String tag);

  /**
   * Removes the tag from the media package element.
   *
   * @param tag
   *          the tag
   */
  void removeTag(String tag);

  /**
   * Returns <code>true</code> if the media package element contains the given tag.
   *
   * @param tag
   *          the tag
   * @return <code>true</code> if the element is tagged
   */
  boolean containsTag(String tag);

  /**
   * Returns <code>true</code> if the media package element contains at least one of the given tags. If there are no
   * tags contained in the set, then the element is considered to match as well.
   *
   * @param tags
   *          the set of tag
   * @return <code>true</code> if the element is tagged accordingly
   */
  boolean containsTag(Collection<String> tags);

  /**
   * Returns the tags for this media package element or an empty array if there are no tags.
   *
   * @return the tags
   */
  String[] getTags();

  /** Removes all tags associated with this element */
  void clearTags();

  	/**
	 * Returns the employee package if the element has been added,
	 * <code>null</code> otherwise.
	 *
	 * @return the employee package
	 */
	EmployeePackage getEmployeePackage();


  /**
   * Returns a reference to the element location.
   *
   * @return the element location
   */
  URI getURI();

  /**
   * Sets the elements location.
   *
   * @param uri
   *          the element location
   */
  void setURI(URI uri);

  /**
   * Returns the file's checksum.
   *
   * @return the checksum
   */
  Checksum getChecksum();

  /**
   * Sets the new checksum on this media package element.
   *
   * @param checksum
   *          the checksum
   */
  void setChecksum(Checksum checksum);

  /**
   * Returns the element's mimetype as found in the ISO Mime Type Registrations.
   * <p/>
   * For example, in case of motion jpeg slides, this method will return the mime type for <code>video/mj2</code>.
   *
   * @return the mime type
   */
  MimeType getMimeType();

  /**
   * Sets the mime type on this media package element.
   *
   * @param mimeType
   *          the new mime type
   */
  void setMimeType(MimeType mimeType);

  /**
   * Returns the number of bytes that are occupied by this media package element.
   *
   * @return the size
   */
  long getSize();

  /**
   * Sets the file size in bytes
   *
   * @param size
   */
  void setSize(long size);

  	/**
	 * Verifies the integrity of the media package element.
	 *
	 * @throws EmployeePackageException
	 *             if the media package element is in an incosistant state
	 */
	void verify() throws EmployeePackageException;

  	/**
	 * Adds a reference to the media package <code>mediaPackage</code>.
	 * <p/>
	 * Note that an element can only refer to one object. Therefore, any
	 * existing reference will be replaced.
	 *
	 * @param employeePackage
	 *            the employee package to refere to
	 */
	void referTo(EmployeePackage employeePackage);

  	/**
	 * Adds a reference to the employee package element <code>element</code>.
	 * <p/>
	 * Note that an element can only refere to one object. Therefore, any
	 * existing reference will be replaced. Also note that if this element is
	 * part of a media package, a consistency check will be made making sure the
	 * refered element is also part of the same employee package. If not, a
	 * {@link EmployeePackageException} will be thrown.
	 *
	 * @param element
	 *            the element to refere to
	 */
	void referTo(EmployeePackageElement element);

  /**
   * Removes any reference.
   */
  void clearReference();

  /**
   * Create a deep copy of this object.
   *
   * @return The copy
   */
  Object clone();

}
