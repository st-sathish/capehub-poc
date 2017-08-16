package com.capestartproject.common.emppackage;

import java.io.InputStream;

import org.w3c.dom.Node;

import com.capestartproject.common.emppackage.identifier.Id;

/**
 * A media package builder provides factory methods for the creation of media packages from manifest files, packages,
 * directories or from sratch.
 */
public interface EmployeePackageBuilder {

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   *
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
	EmployeePackage createNew() throws EmployeePackageException;

  /**
   * Creates a new media package in the temporary directory defined by the java runtime property
   * <code>java.io.tmpdir</code>.
   * <p>
   * The name of the media package root folder will be equal to the handle value.
   * </p>
   *
   * @param identifier
   *          the media package identifier
   * @return the new media package
   * @throws MediaPackageException
   *           if creation of the new media package fails
   */
	EmployeePackage createNew(Id identifier) throws EmployeePackageException;

  /**
   * Loads a media package from the manifest.
   *
   * @param is
   *          the media package manifest input stream
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
	EmployeePackage loadFromXml(InputStream is) throws EmployeePackageException;

  /**
   * Loads a media package from the manifest.
   *
   * @param xml
   *          the media package manifest as an xml string
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
	EmployeePackage loadFromXml(String xml) throws EmployeePackageException;

  /**
   * Loads a media package from the manifest.
   *
   * @param xml
   *          the media package manifest as an xml node
   * @return the media package
   * @throws MediaPackageException
   *           if loading of the media package fails
   */
	EmployeePackage loadFromXml(Node xml) throws EmployeePackageException;

  /**
   * Sets the media package serializer that is used to resolve urls and helps in serialization and deserialization of
   * media package elements.
   *
   * @param serializer
   *          the serializer
   */
	void setSerializer(EmployeePackageSerializer serializer);

  /**
   * Returns the currently active serializer. The serializer is used to resolve urls and helps in serialization and
   * deserialization of media package elements.
   *
   * @return the serializer
   * @see #setSerializer(MediaPackageSerializer)
   */
	EmployeePackageSerializer getSerializer();

}
