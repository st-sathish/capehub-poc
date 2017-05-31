package com.capestartproject.common.util.data;

import static com.capestartproject.common.util.data.Option.some;

import java.lang.reflect.Array;
import java.util.List;

public final class Arrays {
  private Arrays() {
  }

  /** Return the head of array <code>as</code> or <code>none</code>. */
  public static <A> Option<A> head(A[] as) {
    if (as.length > 0) {
      return some(as[0]);
    } else {
      return Option.none();
    }
  }

  /**
   * Sort array <code>as</code> according to the natural ordering. Note that <code>as</code> gets
   * mutated!
   *
   * @return <code>as</code>
   * @see java.util.Arrays#sort(Object[])
   */
  public static <A> A[] sort(A[] as) {
    java.util.Arrays.sort(as);
    return as;
  }

  /** Create a new array by prepending <code>a</code> to <code>as</code>: <code>[a, as0, as1, .. asn]</code> */
  public static <A> A[] cons(Class<A> type, A a, A[] as) {
    @SuppressWarnings("unchecked")
    final A[] x = (A[]) Array.newInstance(type, as.length + 1);
    x[0] = a;
    System.arraycopy(as, 0, x, 1, as.length);
    return x;
  }

  /** Create a new array by appending <code>a</code> to <code>as</code>: <code>[as0, as1, .. asn, a]</code>. */
  public static <A> A[] append(Class<A> type, A[] as, A a) {
    @SuppressWarnings("unchecked")
    final A[] x = (A[]) Array.newInstance(type, as.length + 1);
    System.arraycopy(as, 0, x, 0, as.length);
    x[as.length] = a;
    return x;
  }

  /** Create an array from the vararg parameter list. */
  public static <A> A[] array(A... as) {
    return as;
  }

  public static <A> Function<A[], List<A>> toList() {
    return new Function<A[], List<A>>() {
      @Override
      public List<A> apply(A[] as) {
        if (as != null) {
          return Collections.list(as);
        } else {
          return Collections.nil();
        }
      }
    };
  }

  /** Turn a value into a single element array. */
  public static <A> Function<A, A[]> singleton(final Class<A> type) {
    return new Function<A, A[]>() {
      @Override
      public A[] apply(A a) {
        @SuppressWarnings("unchecked")
        final A[] as = (A[]) Array.newInstance(type, 1);
        as[0] = a;
        return as;
      }
    };
  }

  /** Functional version of {@link #head(Object[])}. */
  public static <A> Function<A[], Option<A>> head() {
    return new Function<A[], Option<A>>() {
      @Override
      public Option<A> apply(A[] as) {
        return head(as);
      }
    };
  }

  /** Functional version of {@link #sort}. */
  public static <A> Function<A[], A[]> sort() {
    return new Function<A[], A[]>() {
      @Override
      public A[] apply(A[] as) {
        return sort(as);
      }
    };
  }

  /** Make a string from a collection separating each element by <code>sep</code>. */
  public static <A> String mkString(A[] as, String sep) {
    final StringBuilder b = new StringBuilder();
    for (Object a : as) b.append(a).append(sep);
    return b.substring(0, Math.max(b.length() - sep.length(), 0));
  }
}
