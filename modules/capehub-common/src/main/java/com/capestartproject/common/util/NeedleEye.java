package com.capestartproject.common.util;

import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.Option.some;

import java.util.concurrent.atomic.AtomicBoolean;

import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Option;

/** Only one function application can be threaded through the needle eye at a time. */
public final class NeedleEye {
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Apply function <code>f</code> only if no other thread currently applies a function using this needle eye. Please
   * note that <code>f</code> must <em>not</em> return null, so please do not use
   * {@link org.opencastproject.util.data.Effect0}.
   * 
   * @return the result of <code>f</code> or none if another function is currently being applied.
   */
  public <A> Option<A> apply(Function0<A> f) {
    if (running.compareAndSet(false, true)) {
      try {
        return some(f.apply());
      } finally {
        running.set(false);
      }
    } else {
      return none();
    }
  }
}
