package com.capestartproject.common.util.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.capestartproject.common.util.data.Option;

/**
 * JAXB Adapter for the {@link com.capestartproject.util.data.Option Option}
 * type
 *
 * @param <T>
 */
public class OptionAdapter<T> extends XmlAdapter<T, Option<T>> {

  @Override
  public T marshal(Option<T> option) throws Exception {
    return option.getOrElse((T) null);
  }

  @Override
  public Option<T> unmarshal(T o) throws Exception {
    return Option.option(o);
  }

}
