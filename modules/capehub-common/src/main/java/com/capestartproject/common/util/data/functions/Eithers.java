package com.capestartproject.common.util.data.functions;

import com.capestartproject.common.util.data.Either;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Option;

/** {@link Either} related functions. */
public final class Eithers {
  private Eithers() {
  }

  /** Like <code>Either#right()#toOption()</code>. */
  public static <A, B> Function<Either<A, B>, Option<B>> toOption() {
    return new Function<Either<A, B>, Option<B>>() {
      @Override public Option<B> apply(Either<A, B> either) {
        return either.right().toOption();
      }
    };
  }

  /** Flatten nested Eithers. */
  public static <A, B> Either<A, B> flatten(Either<A, Either<A, B>> e) {
    return e.right().bind(new Function<Either<A, B>, Either<A, B>>() {
      @Override public Either<A, B> apply(Either<A, B> e) {
        return e.right().either();
      }
    });
  }
}
