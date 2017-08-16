package com.capestartproject.common.job.api;

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
 * Marshals and unmarshals {@link Job}s.
 */
public final class JobParser {
  private static final JAXBContext jaxbContext;

  /** Disallow constructing this utility class */
  private JobParser() {
  }

  static {
    StringBuilder sb = new StringBuilder();
		sb.append("com.capestartproject.job.api:com.capestartproject.serviceregistry.api");
    try {
      jaxbContext = JAXBContext.newInstance(sb.toString(), JobParser.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Parses an xml string representing a {@link Job}
   *
   * @param serializedForm
   *          The serialized data
   * @return The job
   */
  public static Job parseJob(String serializedForm) throws IOException {
    return parseJob(IOUtils.toInputStream(serializedForm, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link Job}
   *
   * @param in
   *          The serialized data
   * @param format
   *          the serialization format
   * @return The job
   */
  public static Job parseJob(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbJob.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Serializes the job into a string representation.
   *
   * @param job
   *          the job
   * @return the job's serialized form
   * @throws IOException
   *           if parsing fails
   */
  public static String toXml(Job job) throws IOException {
    try {
      Marshaller marshaller = jaxbContext.createMarshaller();
      Writer writer = new StringWriter();
      marshaller.marshal(job, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }

  /**
   * Parses an xml string representing a {@link JaxbJobList}
   *
   * @param serializedForm
   *          The serialized data
   * @return The job list
   */
  public static JaxbJobList parseJobList(String serializedForm) throws IOException {
    return parseJobList(IOUtils.toInputStream(serializedForm, "UTF-8"));
  }

  /**
   * Parses a stream representing a {@link JaxbJobList}
   *
   * @param content
   *          the serialized data
   * @return the job list
   */
  public static JaxbJobList parseJobList(InputStream in) throws IOException {
    Unmarshaller unmarshaller;
    try {
      unmarshaller = jaxbContext.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(in), JaxbJobList.class).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

}
