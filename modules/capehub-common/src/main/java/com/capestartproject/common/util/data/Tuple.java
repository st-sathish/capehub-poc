package com.capestartproject.common.util.data;

import static com.capestartproject.common.util.EqualsUtil.eqClasses;
import static com.capestartproject.common.util.EqualsUtil.hash;

/** A pair. */
public final class Tuple<A, B> {

  private final A a;
  private final B b;

  public Tuple(A a, B b) {
    this.a = a;
    this.b = b;
  }

  /** Get the first element. */
  public A getA() {
    return a;
  }

  /** Get the second element. */
  public B getB() {
    return b;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that)
      return true;
    if (!eqClasses(this, that))
      return false;
    Tuple thatc = (Tuple) that;
    return a.equals(thatc.a) && b.equals(thatc.b);
  }

  @Override
  public int hashCode() {
    return hash(a, b);
  }

  /** Create a new tuple with two elements <code>a</code> and <code>b</code>. */
  public static <A, B> Tuple<A, B> tuple(A a, B b) {
    return new Tuple<A, B>(a, b);
  }

  @Override
  public String toString() {
    return "(" + a + "," + b + ")";
  }
}
