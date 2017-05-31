package com.capestartproject.common.fn.juc;

import java.util.Iterator;

/** Functions for {@link Iterable}s. */
public final class Iterables {
  private Iterables() {
  }

  public static String mkString(Iterable<?> as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Iterator<?> i = as.iterator(); i.hasNext();) {
      b.append(i.next());
      if (i.hasNext()) {
        b.append(sep);
      }
    }
    return b.toString();
  }

  /**
   * Make a string from an iterable separating each element by <code>sep</code>. The string is surrounded by
   * <code>pre</code> and <code>post</code>.
   */
  public static String mkString(Iterable<?> as, String sep, String pre, String post) {
    return pre + mkString(as, sep) + post;
  }

  /** Return an iterator as an iterable to make it usable in a for comprehension. */
  public static <A> Iterable<A> asIterable(final Iterator<A> i) {
    return new Iterable<A>() {
      @Override
      public Iterator<A> iterator() {
        return i;
      }
    };
  }
}
