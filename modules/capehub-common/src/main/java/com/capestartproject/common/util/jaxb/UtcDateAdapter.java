package com.capestartproject.common.util.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * JAXB adapter that formats dates in UTC format YYYY-MM-DD'T'hh:mm:ss'Z', e.g. 1970-01-01T00:00:00Z
 */
public final class UtcDateAdapter extends XmlAdapter<String, Date> {
  @Override
  public String marshal(Date date) throws Exception {
    return ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(new DateTime(date.getTime()));
  }

  @Override
  public Date unmarshal(String date) throws Exception {
    return ISODateTimeFormat.dateTimeParser().parseDateTime(date).toDate();
  }
}
