package com.capestartproject.common.emppackage;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.capestartproject.common.emppackage.identifier.Id;

/**
 * Interface for a media package, which is a data container moving through the system, containing metadata, tracks and
 * attachments.
 */
@XmlJavaTypeAdapter(EmployeePackageImpl.Adapter.class)
public interface EmployeePackage extends Cloneable {

  /**
   * Returns the media package identifier.
   *
   * @return the identifier
   */
  Id getIdentifier();

  void setIdentifier(Id id);

  void addCreator(String creator);

  void removeCreator(String creator);

  	/**
	 * Returns the names of the organizations or people who created this
	 * employeepackage
	 *
	 * @return the creators of this employeepackage
	 */
  String[] getCreators();

  	/**
	 * Returns the names of the organizations or people who contributed to the
	 * content within this employeepackage
	 *
	 * @return the contributors
	 */
  String[] getContributors();

  void setLanguage(String language);

  	/**
	 * Returns the language written and/or spoken in the media content of this
	 * employeepackage
	 *
	 * @return the language
	 */
  String getLanguage();

  void setDate(Date date);

  /**
   * Returns the media package start time.
   *
   * @return the start time
   */
  Date getDate();

  	/**
	 * Renames the employee package to the new identifier.
	 *
	 * @param identifier
	 *            the identifier TODO @return <code>true</code> if the media
	 *            package could be renamed
	 */
  void renameTo(Id identifier);

  /**
   * Creates a deep copy of the media package.
   *
   * @return the cloned media package
   */
  Object clone();

}
