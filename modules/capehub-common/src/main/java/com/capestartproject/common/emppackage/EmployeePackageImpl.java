package com.capestartproject.common.emppackage;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capestartproject.common.emppackage.identifier.Id;
import com.capestartproject.common.emppackage.identifier.IdBuilder;
import com.capestartproject.common.emppackage.identifier.UUIDIdBuilderImpl;

/**
 * Default implementation for a media media package.
 */
@XmlType(name = "employeepackage", namespace = "http://employeepackage.capestartproject.com", propOrder = { "name",
		"attachments", "creators" })
@XmlRootElement(name = "employeepackage", namespace = "http://employeepackage.capestartproject.com")
@XmlAccessorType(XmlAccessType.NONE)
public final class EmployeePackageImpl implements EmployeePackage {

  /** the logging facility provided by log4j */
	private static final Logger logger = LoggerFactory.getLogger(EmployeePackageImpl.class.getName());

  /**
   * The prefix indicating that a tag should be excluded from a search for elements using
   * {@link #getElementsByTags(Collection)}
   */
  public static final String NEGATE_TAG_PREFIX = "-";

  /** Context for serializing and deserializing */
  static final JAXBContext context;

	@XmlElement(name = "name")
	private String name = null;

  @XmlElementWrapper(name = "creators")
  @XmlElement(name = "creator")
  private Set<String> creators = null;

  /** id builder, for internal use only */
  private static final IdBuilder idBuilder = new UUIDIdBuilderImpl();

	/** The employee package's identifier */
  private Id identifier = null;

  /** The start date and time */
  private long startTime = 0L;

	/** The employee package duration */
  private Long duration = null;

  /** Number of attachments */
  private int attachments = 0;

  /** Numer of unclassified elements */
  private int others = 0;

  static {
    try {
			context = JAXBContext.newInstance("com.capestartproject.employeepackage",
					EmployeePackageImpl.class.getClassLoader());
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a media package object.
   */
	EmployeePackageImpl() {
    this(idBuilder.createNew());
  }

  /**
   * Creates a media package object with the media package identifier.
   *
   * @param id
   *          the media package identifier
   */
	EmployeePackageImpl(Id id) {
    this.identifier = id;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.common.employeepackage.EmployeePackage#getIdentifier()
	 */
  @XmlAttribute(name = "id")
  @Override
  public Id getIdentifier() {
    return identifier;
  }

  	/**
	 * {@inheritDoc}
	 *
	 * @see com.capestartproject.common.employeepackage.EmployeePackage#setIdentifier(com.capestartproject.common.employeepackage.EmployeePackage.identifier.Id)
	 */
  @Override
  public void setIdentifier(Id identifier) {
    this.identifier = identifier;
  }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.emppackage.EmployeePackage#addCreator(java.
	 * lang.String)
	 */
	@Override
	public void addCreator(String creator) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.emppackage.EmployeePackage#removeCreator(java
	 * .lang.String)
	 */
	@Override
	public void removeCreator(String creator) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.emppackage.EmployeePackage#getCreators()
	 */
	@Override
	public String[] getCreators() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.emppackage.EmployeePackage#getContributors()
	 */
	@Override
	public String[] getContributors() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.emppackage.EmployeePackage#setLanguage(java.
	 * lang.String)
	 */
	@Override
	public void setLanguage(String language) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.emppackage.EmployeePackage#getLanguage()
	 */
	@Override
	public String getLanguage() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.capestartproject.common.emppackage.EmployeePackage#setDate(java.util.
	 * Date)
	 */
	@Override
	public void setDate(Date date) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.emppackage.EmployeePackage#getDate()
	 */
	@Override
	public Date getDate() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.capestartproject.common.emppackage.EmployeePackage#renameTo(com.
	 * capestartproject.common.emppackage.identifier.Id)
	 */
	@Override
	public void renameTo(Id identifier) {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		try {
			String xml = EmployeePackageParser.getAsXml(this);
			return EmployeePackageBuilderFactory.newInstance().newEmpPackageBuilder().loadFromXml(xml);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (identifier != null)
			return identifier.toString();
		else
			return "Unknown media package";
	}

	/**
	 * A JAXB adapter that allows the {@link MediaPackage} interface to be
	 * un/marshalled
	 */
	static class Adapter extends XmlAdapter<EmployeePackageImpl, EmployeePackage> {
		@Override
		public EmployeePackageImpl marshal(EmployeePackage ep) throws Exception {
			return (EmployeePackageImpl) ep;
		}

		@Override
		public EmployeePackage unmarshal(EmployeePackageImpl ep) throws Exception {
			return ep;
		}
	}
}
