package com.capestartproject.common.emppackage;

import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.capestartproject.common.emppackage.identifier.Id;

/**
 * Interface for a employee package, which is a data container moving through
 * the system, containing metadata, and attachments.
 */
@XmlJavaTypeAdapter(EmployeePackageImpl.Adapter.class)
public interface EmployeePackage extends Cloneable {

  	/**
	 * Returns the employee package identifier.
	 *
	 * @return the identifier
	 */
  Id getIdentifier();

  void setIdentifier(Id id);

	String getFirstName();

	void setFirstName();

	void setLastName();

	String getLastName();

	String getMobileNumber();

	void setMobileNumber();

	void setPersonalEmail();

	String getPersonalEmail();

  	/**
	 * Returns the names of the organizations or people who created this
	 * employeepackage
	 *
	 * @return the creators of this employeepackage
	 */
  String[] getCreators();

  void setLanguage(String language);

  	/**
	 * Returns the language written and/or spoken in the employee content of
	 * this employeepackage
	 *
	 * @return the language
	 */
  String getLanguage();

  void setDate(Date date);

  	/**
	 * Returns the employee package start time.
	 *
	 * @return the start time
	 */
  Date getDate();

  	/**
	 * Renames the employee package to the new identifier.
	 *
	 * @param identifier
	 *            the identifier TODO @return <code>true</code> if the employee
	 *            package could be renamed
	 */
  void renameTo(Id identifier);

  	/**
	 * Creates a deep copy of the employee package.
	 *
	 * @return the cloned employee package
	 */
  Object clone();

	/**
	 * Returns an iteration of the employee package elements.
	 *
	 * @return the employee package elements
	 */
	Iterable<EmployeePackageElement> elements();

	/**
	 * Returns all of the elements.
	 *
	 * @return the elements
	 */
	EmployeePackageElement[] getElements();

	/**
	 * Returns the element that is identified by the given identifier or
	 * <code>null</code> if no such element exists.
	 *
	 * @param id
	 *            the element identifier
	 * @return the element
	 */
	EmployeePackageElement getElementById(String id);

	/**
	 * Returns the elements that are tagged with the given tag or an empty array
	 * if no such elements are found.
	 *
	 * @param tag
	 *            the tag
	 * @return the elements
	 */
	EmployeePackageElement[] getElementsByTag(String tag);

	/**
	 * Returns the elements that are tagged with any of the given tags or an
	 * empty array if no such elements are found. If any of the tags in the
	 * <code>tags</code> collection start with a '-' character, any elements
	 * matching the tag will be excluded from the returned
	 * EmployeePackageElement[]. If <code>tags</code> is empty or null, all
	 * elements are returned.
	 *
	 * @param tags
	 *            the tags
	 * @return the elements
	 */
	EmployeePackageElement[] getElementsByTags(Collection<String> tags);

	/**
	 * Adds an arbitrary {@link EmployeePackageElement} to this employee
	 * package.
	 *
	 * @param element
	 *            the element
	 */
	void add(EmployeePackageElement element);

	/**
	 * Removes the element with the given identifier from the employeepackage
	 * and returns it.
	 *
	 * @param id
	 *            the element identifier
	 */
	EmployeePackageElement removeElementById(String id);

	/**
	 * Removes an arbitrary employee package element.
	 *
	 * @param element
	 *            the employee package element
	 */
	void remove(EmployeePackageElement element);
}
