package com.capestartproject.common.fn;

import com.capestartproject.common.util.data.Function;

/** Functions. */
public final class Fns {
  private Fns() {
  }

  /** The identity function. */
  public static <A> Function<A, A> id() {
    return new Function<A, A>() {
      @Override
      public A apply(A a) {
        return a;
      }
    };
  }
}
