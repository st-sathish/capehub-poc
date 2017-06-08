package com.capestartproject.common.serviceregistry.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;

/**
 * Marshals and unmarshals {@link ServiceRegistration}s.
 */
public final class ServiceRegistrationParser {

  /** The jaxb context to use when creating marshallers and unmarshallers */
  private static final JAXBContext jaxbContext;

  /** Static initializer to setup the jaxb context */
  static {
    try {
      jaxbContext = JAXBContext.newInstance("org.opencastproject.serviceregistry.api:org.opencastproject.job.api",
              ServiceRegistrationParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Disallow construction of this utility class */
  private ServiceRegistrationParser() {
  }

  /**
   * Parses an xml string representing a {@link ServiceRegistration}
   *
   * @param xml
   *          The serialized data
   * @return The ServiceRegistration
   */
  public static ServiceRegistration parseXml(String xml) throws IOException {
    return parse(IOUtils.toInputStream(xml, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link ServiceRegistration}
   *
   * @param in
   *          The serialized data
   * @param format
   *          the serialization format
   * @return The ServiceRegistration
   */
  public static ServiceRegistration parse(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbServiceRegistration.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Gets a serialized representation of a {@link ServiceRegistration}
   *
   * @param registration
   *          The ServiceRegistration to marshal
   * @return the serialized ServiceRegistration
   */
  public static InputStream toXmlStream(ServiceRegistration registration) throws IOException {
    return IOUtils.toInputStream(toXml(registration), "UTF-8");
  }

  /**
   * Gets an xml representation of a {@link ServiceRegistration}
   *
   * @param registration
   *          The service registration to marshal
   * @return the serialized registration
   */
  public static String toXml(ServiceRegistration registration) throws IOException {
    Marshaller marshaller;
    try {
      marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(registration, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  public static JaxbServiceStatisticsList parseStatistics(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbServiceStatisticsList.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static JaxbServiceRegistrationList parseRegistrations(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbServiceRegistrationList.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
