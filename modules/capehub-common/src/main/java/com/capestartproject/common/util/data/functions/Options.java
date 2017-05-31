package com.capestartproject.common.util.data.functions;

import static com.capestartproject.common.util.data.Monadics.mlist;
import static com.capestartproject.common.util.data.Option.none;
import static com.capestartproject.common.util.data.Option.some;

import com.capestartproject.common.util.data.Effect;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Function2;
import com.capestartproject.common.util.data.Option;

import java.util.ArrayList;
import java.util.List;

/** {@link Option} related functions. */
public final class Options {
  private Options() {
  }

  /** m (m a) -> m a */
  public static <A> Option<A> join(Option<Option<A>> a) {
    return a.bind(Functions.<Option<A>> identity());
  }

  public static <A> Function<Option<A>, List<A>> asList() {
    return new Function<Option<A>, List<A>>() {
      @Override
      public List<A> apply(Option<A> a) {
        return a.list();
      }
    };
  }

  public static <A, B> Function<A, Option<B>> never() {
    return new Function<A, Option<B>>() {
      @Override
      public Option<B> apply(A a) {
        return none();
      }
    };
  }

  public static <A> Function0<Option<A>> never2() {
    return new Function0<Option<A>>() {
      @Override
      public Option<A> apply() {
        return none();
      }
    };
  }

  /** Function that turns <code>true</code> into <code>some(true)</code> and false into <code>none</code>. */
  public static final Function<Boolean, Option<Boolean>> toOption = new Function<Boolean, Option<Boolean>>() {
    @Override
    public Option<Boolean> apply(Boolean a) {
      return a ? some(true) : Option.<Boolean> none();
    }
  };

  /** Returns some(message) if predicate is false, none otherwise. */
  public static Option<String> toOption(boolean predicate, String message) {
    return predicate ? Option.<String> none() : some(message);
  }

  /** Sequence a list of options. [Option a] -&gt; Option [a] */
  public static <A> Option<List<A>> sequenceOpt(List<Option<A>> as) {
    final List<A> seq = mlist(as).foldl(new ArrayList<A>(), new Function2<List<A>, Option<A>, List<A>>() {
      @Override
      public List<A> apply(List<A> sum, Option<A> o) {
        for (A a : o) {
          sum.add(a);
          return sum;
        }
        return sum;
      }
    });
    return some(seq);
  }

  /** Map <code>g</code> over the result of <code>f</code>. */
  public static <A, B, C> Function<A, Option<C>> map(final Function<? super A, ? extends Option<? extends B>> f,
          final Function<? super B, ? extends C> g) {
    return new Function<A, Option<C>>() {
      @Override
      public Option<C> apply(A a) {
        return f.apply(a).map(g);
      }
    };
  }

  /** Apply effect <code>e</code> to the result of <code>f</code> which is then returned. */
  public static <A, B> Function<A, Option<B>> foreach(final Function<? super A, ? extends Option<? extends B>> f,
          final Effect<? super B> e) {
    return new Function<A, Option<B>>() {
      @Override
      public Option<B> apply(A a) {
        return (Option<B>) f.apply(a).foreach(e);
      }
    };
  }

  /** {@link org.opencastproject.util.data.Option#isNone()} as a function. */
  public static <A> Function<Option<A>, Boolean> isNone() {
    return new Function<Option<A>, Boolean>() {
      @Override
      public Boolean apply(Option<A> a) {
        return a.isNone();
      }
    };
  }

  /** {@link org.opencastproject.util.data.Option#isSome()} as a function. */
  public static <A> Function<Option<A>, Boolean> isSome() {
    return new Function<Option<A>, Boolean>() {
      @Override
      public Boolean apply(Option<A> a) {
        return a.isSome();
      }
    };
  }

  public static <A> Function<Option<A>, A> getOrElse(final A none) {
    return new Function<Option<A>, A>() {
      @Override public A apply(Option<A> as) {
        return as.getOrElse(none);
      }
    };
  }

  public static <A> Function<Option<A>, A> getF() {
    return new Function<Option<A>, A>() {
      @Override public A apply(Option<A> as) {
        return as.get();
      }
    };
  }
}
