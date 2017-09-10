package com.capestartproject.common.util;

import static com.capestartproject.common.util.data.Monadics.mlist;
import static com.capestartproject.common.util.data.functions.Misc.cast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.capestartproject.common.util.data.Function;

public final class JsonArr implements Iterable<JsonVal> {
  private final List<Object> val;

  public JsonArr(List arr) {
    this.val = new ArrayList<Object>(arr);
  }

  public JsonVal val(int index) {
    return new JsonVal(val.get(index));
  }

  public JsonObj obj(int index) {
    return new JsonObj((Map) val.get(index));
  }

  public JsonArr arr(int index) {
    return new JsonArr((List) val.get(index));
  }

  public <A> List<A> as(Function<Object, A> converter) {
    return mlist(val).map(converter).value();
  }

  public List<JsonVal> get() {
    return mlist(val).map(JsonVal.asJsonVal).value();
  }

  @Override
  public Iterator<JsonVal> iterator() {
    return mlist(val).map(JsonVal.asJsonVal).iterator();
  }

  private static <A> Function<Object, A> caster(final Class<A> ev) {
    return new Function<Object, A>() {
      @Override public A apply(Object o) {
        return cast(o, ev);
      }
    };
  }
}
