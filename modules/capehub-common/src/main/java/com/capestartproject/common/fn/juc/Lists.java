package com.capestartproject.common.fn.juc;

import java.util.ArrayList;
import java.util.List;

/** Functions for {@link List}s. */
public final class Lists {
  private Lists() {
  }

  /**
   * Create a new immutable list prepending <code>h</code> to list <code>t</code>.
   * <p/>
   * Since the implementation is based on the Java Collection classes it cannot make any assumptions about the
   * immutability of <code>t</code> and therefore has to copy it into a new list. Use with care.
   */
  public static <A> List<A> cons(A h, List<? extends A> t) {
    final List<A> a = new ArrayList<A>(t.size() + 1);
    a.add(h);
    a.addAll(t);
    return Immutables.mk(a);
  }
}
