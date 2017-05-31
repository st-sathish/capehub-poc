
package com.capestartproject.common.util.data;

import static com.capestartproject.common.util.data.functions.Functions.chuck;

/**
 * Run a side effect.
 *
 * @see X
 */
public abstract class Effect0 extends Function0<Void> {
  @Override
  public final Void apply() {
    run();
    return null;
  }

  /** Run the side effect. */
  protected abstract void run();

  /** Return the effect as a function of arity 0. */
  public Function0<Void> toFunction() {
    return this;
  }

  /** Return the effect as a Runnable. */
  public Runnable toRunnable() {
    return new Runnable() {
      @Override public void run() {
        Effect0.this.run();
      }
    };
  }

  /** Version of {@link Effect0} that allows for throwing a checked exception. */
  public abstract static class X extends Effect0 {
    @Override
    protected final void run() {
      try {
        xrun();
      } catch (Exception e) {
        chuck(e);
      }
    }

    /**
     * Run the side effect. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link org.opencastproject.util.data.functions.Functions#chuck(Throwable)} for details.
     */
    protected abstract void xrun() throws Exception;
  }
}
