package com.capestartproject.common.util.data;

import static com.capestartproject.common.util.data.functions.Functions.chuck;

/**
 * Run a side effect.
 *
 * @see X
 */
public abstract class Effect<A> extends Function<A, Void> {
  @Override
  public final Void apply(A a) {
    run(a);
    return null;
  }

  /** Run the side effect. */
  protected abstract void run(A a);

  /** Return the effect as a function. */
  public Function<A, Void> toFunction() {
    return this;
  }

  /** Run this and the <code>next</code> effect on the given argument. */
  public Effect<A> and(final Effect<? super A> next) {
    return new Effect<A>() {
      @Override
      protected void run(A a) {
        Effect.this.apply(a);
        next.apply(a);
      }
    };
  }

  /** Version of {@link Effect} that allows for throwing a checked exception. */
  public abstract static class X<A> extends Effect<A> {
    @Override
    protected void run(A a) {
      try {
        xrun(a);
      } catch (Exception e) {
        chuck(e);
      }
    }

    /**
     * Run the side effect. Any thrown exception gets "chucked" so that you may catch them as is. See
     * {@link org.opencastproject.util.data.functions.Functions#chuck(Throwable)} for details.
     */
    protected abstract void xrun(A a) throws Exception;
  }
}
