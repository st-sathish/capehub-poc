package com.capestartproject.common.util.jaxb;

import static com.capestartproject.common.util.data.functions.Misc.chuck;

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

/** Base class for JAXB parser classes. */
public abstract class JaxbParser {
  private final JAXBContext ctx;

  /**
   * Create a new parser.
   *
   * @param contextPath see {@link javax.xml.bind.JAXBContext#newInstance(String, ClassLoader)}
   */
  protected JaxbParser(String contextPath) {
    this.ctx = init(contextPath);
  }

  private JAXBContext init(String contextPath) {
    try {
      return JAXBContext.newInstance(contextPath, this.getClass().getClassLoader());
    } catch (JAXBException e) {
      return chuck(e);
    }
  }

  public JAXBContext getCtx() {
    return ctx;
  }

  /** Unmarshal an instance of class <code>dtoClass</code> from <code>source</code> and close it. */
  public <A> A unmarshal(Class<A> dtoClass, InputStream source) throws IOException {
    try {
      final Unmarshaller unmarshaller = ctx.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(source), dtoClass).getValue();
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      IOUtils.closeQuietly(source);
    }
  }

  /**
   * Marshal an object into a string.
   */
  public String marshal(Object o) throws IOException {
    try {
      final Marshaller marshaller = ctx.createMarshaller();
      final Writer writer = new StringWriter();
      marshaller.marshal(o, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }
}
