package com.capestartproject.common.emppackage;

import static com.capestartproject.common.util.data.functions.Misc.chuck;

import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.capestartproject.common.util.DateTimeSupport;

/**
 * Convenience implementation that supports serializing and deserializing media packages.
 */
public final class EmployeePackageParser {

  /**
   * Private constructor to prohibit instances of this static utility class.
   */
	private EmployeePackageParser() {
    // Nothing to do
  }

  /**
   * Serializes the media package to a string.
   *
   * @param mediaPackage
   *          the media package
   * @return the serialized media package
   */
	public static String getAsXml(EmployeePackage mediaPackage) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must not be null");
    try {
			Marshaller marshaller = EmployeePackageImpl.context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
      StringWriter writer = new StringWriter();
      marshaller.marshal(mediaPackage, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the media package to a JSON string.
   *
   * @param mediaPackage
   *          the media package
   * @return the serialized media package
   */
	public static String getAsJSON(EmployeePackage employeePackage) {
		if (employeePackage == null) {
      throw new IllegalArgumentException("Mediapackage must not be null");
    }
    try {
			Marshaller marshaller = EmployeePackageImpl.context.createMarshaller();

      Configuration config = new Configuration();
      config.setSupressAtAttributes(true);
      MappedNamespaceConvention con = new MappedNamespaceConvention(config);
      StringWriter writer = new StringWriter();
      XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
        }
      };

			marshaller.marshal(employeePackage, xmlStreamWriter);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /** Serializes a media package to a {@link Document} without any further processing. */
	public static Document getAsXmlDocument(EmployeePackage mp) {
    try {
			final Marshaller marshaller = EmployeePackageImpl.context.createMarshaller();
      final Document doc = newDocument();
      marshaller.marshal(mp, doc);
      return doc;
    } catch (JAXBException e) {
      return chuck(e);
    }
  }

  /** Create a new DOM document. */
  private static Document newDocument() {
    final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);
    try {
      return docBuilderFactory.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      return chuck(e);
    }
  }

  /**
   * Serializes the media package to a {@link org.w3c.dom.Document}.
   * <p/>
   * todo Implementation is currently defective since it misses various properties. See
   * http://opencast.jira.com/browse/MH-9489 Use {@link #getAsXmlDocument(MediaPackage)} instead if you do not need a
   * serializer.
   *
   * @param mediaPackage
   *          the mediapackage
   * @param serializer
   *          the serializer
   * @return the serialized media package
   * @throws MediaPackageException
   *           if serializing fails
   */
	public static Document getAsXml(EmployeePackage employeePackage, EmployeePackageSerializer serializer)
			throws EmployeePackageException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setNamespaceAware(true);

    DocumentBuilder docBuilder = null;
    try {
      docBuilder = docBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e1) {
			throw new EmployeePackageException(e1);
    }

    Document doc = docBuilder.newDocument();

    // Root element "mediapackage"
		Element mpXml = doc.createElement("employeepackage");
    doc.appendChild(mpXml);

    // Handle
		if (employeePackage.getIdentifier() != null)
			mpXml.setAttribute("id", employeePackage.getIdentifier().toString());

    // Start time
		if (employeePackage.getDate() != null && employeePackage.getDate().getTime() > 0)
			mpXml.setAttribute("start", DateTimeSupport.toUTC(employeePackage.getDate().getTime()));

    return mpXml.getOwnerDocument();
  }

  /**
   * Parses the media package and returns its object representation.
   *
   * @param xml
   *          the serialized media package
   * @return the media package instance
   * @throws MediaPackageException
   *           if de-serializing the media package fails
   */
	public static EmployeePackage getFromXml(String xml) throws EmployeePackageException {
		EmployeePackageBuilder builder = EmployeePackageBuilderFactory.newInstance().newEmpPackageBuilder();
    return builder.loadFromXml(xml);
  }

  	/**
	 * Writes an xml representation of this MediaPackage to a stream.
	 *
	 * @param employeePackage
	 *            the employeePackage
	 * @param out
	 *            The output stream
	 * @param format
	 *            Whether to format the output for readability, or not (false
	 *            gives better performance)
	 * @throws MediaPackageException
	 *             if serializing or reading from a serialized media package
	 *             fails
	 */
	public static void getAsXml(EmployeePackage employeePackage, OutputStream out, boolean format)
			throws EmployeePackageException {
    try {
			Marshaller marshaller = EmployeePackageImpl.context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
			marshaller.marshal(employeePackage, out);
    } catch (JAXBException e) {
			throw new EmployeePackageException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
