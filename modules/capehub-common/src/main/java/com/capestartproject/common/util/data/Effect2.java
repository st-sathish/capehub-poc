package com.capestartproject.common.util.data;

import static com.capestartproject.common.util.data.functions.Functions.chuck;

/**
 * Run a side effect.
 *
 * @see X
 */
public abstract class Effect2<A, B> extends Function2<A, B, Void> {
  @Override
  public Void apply(A a, B b) {
    run(a, b);
    return null;
  }

  /** Run the side effect. */
  protected abstract void run(A a, B b);

  /** Return the effect as a function of arity 2. */
  public Function2<A, B, Void> toFunction() {
    return this;
  }

  /** Version of {@link Effect0} that allows for throwing a checked exception. */
  public abstract static class X<A, B> extends Effect2<A, B> {
    @Override
    protected final void run(A a, B b) {
      try {
        xrun(a, b);
      } catch (Exception e) {
        chuck(e);
      }
    }

    /**
     * Run the side effect. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link org.opencastproject.util.data.functions.Functions#chuck(Throwable)} for details.
     */
    protected abstract void xrun(A a, B b) throws Exception;
  }
}
