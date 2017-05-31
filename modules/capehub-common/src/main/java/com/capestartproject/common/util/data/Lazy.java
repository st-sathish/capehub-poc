package com.capestartproject.common.util.data;

/**
 * The value of a lazy is calculated on first access.
 * <h3>Implementation note</h3>
 * The calculation is not synchronized for performance reasons. As a trade off the value may be calculated
 * multiple times if many threads concurrently access the lazy in uninitialized state.
 * Since <code>f</code> is expected to be a <em>pure</em> function this is not a serious problem despite the
 * fact that calculation <em>may</em> happens multiple times.
 */
public final class Lazy<A> {
  private volatile A a;
  private final Function0<A> f;

  /**
   * Create a new lazy value.
   *
   * @param f
   *        a <em>pure</em>, referentially transparent function returning the lazy's value
   */

  public Lazy(Function0<A> f) {
    this.f = f;
  }

  public static <A> Lazy<A> lazy(Function0<A> f) {
    return new Lazy<A>(f);
  }

  public A value() {
    if (a == null) {
      a = f.apply();
    }
    return a;
  }
}
