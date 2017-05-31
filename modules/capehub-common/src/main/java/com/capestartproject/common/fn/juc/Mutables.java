package com.capestartproject.common.fn.juc;

import com.capestartproject.common.util.data.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/** Constructor functions for mutable data types. */
public final class Mutables {
  private Mutables() {
  }

  public static <A> List<A> list() {
    return new LinkedList<A>();
  }

  public static <A> List<A> list(List<A> as) {
    return new LinkedList<A>(as);
  }

  public static <A> List<A> list(A a, A... as) {
    final LinkedList<A> buf = new LinkedList<A>();
    buf.add(a);
    Collections.addAll(buf, as);
    return buf;
  }

  public static <A> List<A> arrayList() {
    return new ArrayList<A>();
  }

  public static <A> List<A> arrayList(int initialCapacity) {
    return new ArrayList<A>(initialCapacity);
  }

  public static <A, B> Map<A, B> map() {
    return new HashMap<A, B>();
  }

  public static <A, B> Map<A, B> map(Map<A, ? extends B> m, Tuple<A, ? extends B>... as) {
    final Map<A, B> r = new HashMap<A, B>();
    r.putAll(m);
    for (Tuple<A, ? extends B> a : as) {
      r.put(a.getA(), a.getB());
    }
    return r;
  }

  public static <A, B> Map<A, B> hashMap() {
    return new HashMap<A, B>();
  }

  public static <A> Stack<A> stack() {
    return new Stack<A>();
  }

  public static <A> Stack<A> stack(A... as) {
    final Stack<A> s = new Stack<A>();
    Collections.addAll(s, as);
    return s;
  }

  public static <A> Stack<A> stack(Collection<? extends A> as) {
    final Stack<A> s = new Stack<A>();
    for (A a : as) {
      s.push(a);
    }
    return s;
  }
}
