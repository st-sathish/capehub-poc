package com.capestartproject.common.util.data;

import com.capestartproject.common.util.data.functions.Functions;

import static com.capestartproject.common.util.data.functions.Misc.chuck;

/**
 * Function of arity 0, i.e. a constant function.
 *
 * @see X
 */
public abstract class Function0<A> {
  /** Apply function yielding a constant value. Don't be tempted to become impure! */
  public abstract A apply();

  /** Apply this function, then pass the result to <code>f</code>. */
  public <B> Function0<B> then(final Function<A, B> f) {
    return Functions.then(Function0.this, f);
  }

  /** Apply this function and ignore its result, then apply <code>f</code>. */
  public <B> Function0<B> then(final Function0<B> f) {
    return Functions.then(Function0.this, f);
  }

  /** Turn this function into an effect by discarding its result. */
  public Effect0 toEffect() {
    return Functions.toEffect(this);
  }

  /** Version of {@link Function0} that allows for throwing a checked exception. */
  public abstract static class X<A> extends Function0<A> {
    @Override
    public final A apply() {
      try {
        return xapply();
      } catch (Exception e) {
        return chuck(e);
      }
    }

    /**
     * Apply function to <code>a</code>. Any thrown exception gets "chucked" so that you may
     * catch them as is. See {@link Functions#chuck(Throwable)} for details.
     */
    public abstract A xapply() throws Exception;
  }
}
