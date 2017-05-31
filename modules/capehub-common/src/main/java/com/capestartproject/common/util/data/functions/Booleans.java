package com.capestartproject.common.util.data.functions;

import com.capestartproject.common.util.EqualsUtil;
import com.capestartproject.common.util.data.Function;
import com.capestartproject.common.util.data.Function0;
import com.capestartproject.common.util.data.Function2;

/** Boolean functions. */
public final class Booleans {
  private Booleans() {
  }

  /** Return a predicate function that always returns true. */
  public static <A> Function<A, Boolean> always() {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return true;
      }
    };
  }

  /** Return a predicate function that always returns false. */
  public static <A> Function<A, Boolean> nothing() {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return true;
      }
    };
  }

  public static <A> Function<A, Boolean> ne(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return EqualsUtil.ne(x, a);
      }
    };
  }

  public static <A> Function<A, Boolean> eq(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return EqualsUtil.eq(x, a);
      }
    };
  }

  public static <A extends Comparable<A>> Function<A, Boolean> lt(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return x.compareTo(a) < 0;
      }
    };
  }

  public static <A extends Comparable<A>> Function<A, Boolean> gt(final A a) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A x) {
        return x.compareTo(a) > 0;
      }
    };
  }

  public static final Function2<Boolean, Boolean, Boolean> and = new Function2<Boolean, Boolean, Boolean>() {
    @Override
    public Boolean apply(Boolean a, Boolean b) {
      return a && b;
    }
  };

  public static final Function2<Boolean, Boolean, Boolean> or = new Function2<Boolean, Boolean, Boolean>() {
    @Override
    public Boolean apply(Boolean a, Boolean b) {
      return a || b;
    }
  };

  public static <A, B> Function2<A, B, Boolean> and2(final Function<A, Boolean> f, final Function<B, Boolean> g) {
    return new Function2<A, B, Boolean>() {
      @Override
      public Boolean apply(A a, B b) {
        return f.apply(a) && g.apply(b);
      }
    };
  }

  public static <A> Function<A, Boolean> and(final Function<A, Boolean> f, final Function<A, Boolean> g) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return f.apply(a) && g.apply(a);
      }
    };
  }

  public static <A> Function<A, Boolean> all(final Function<A, Boolean>... fs) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        for (Function<A, Boolean> f : fs) {
          if (!f.apply(a))
            return false;
        }
        return true;
      }
    };
  }

  public static <A> Function<A, Boolean> one(final Function<A, Boolean>... fs) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        for (Function<A, Boolean> f : fs) {
          if (f.apply(a))
            return true;
        }
        return false;
      }
    };
  }

  /** Apply <em>all</em> functions and return their results concatenated with boolean AND. */
  public static <A> Function<A, Boolean> andNEager(final Function<A, Boolean>... fs) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        boolean r = true;
        for (Function<A, Boolean> f : fs) {
          // application first!
          r = f.apply(a) && r;
        }
        return r;
      }
    };
  }

  /**
   * Apply functions lazily and return their results concatenated with boolean AND, i.e. function application stops
   * after the first function yielding false.
   */
  public static <A> Function<A, Boolean> andN(final Function<A, Boolean>... fs) {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        for (Function<A, Boolean> f : fs) {
          if (!f.apply(a))
            return false;
        }
        return true;
      }
    };
  }

  /** A function that always returns true. */
  public static final Function0<Boolean> yes = new Function0<Boolean>() {
    @Override
    public Boolean apply() {
      return true;
    }
  };

  /** A function that always returns true. */
  public static <A> Function<A, Boolean> yes() {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return true;
      }
    };
  }

  /** A function that always returns false. */
  public static final Function0<Boolean> no = new Function0<Boolean>() {
    @Override
    public Boolean apply() {
      return false;
    }
  };

  /** A function that always returns false. */
  public static <A> Function<A, Boolean> no() {
    return new Function<A, Boolean>() {
      @Override
      public Boolean apply(A a) {
        return false;
      }
    };
  }

  public static <A> Function<A, Boolean> not(Function<A, Boolean> f) {
    return not.o(f);
  }

  public static final Function<Boolean, Boolean> not = new Function<Boolean, Boolean>() {
    @Override
    public Boolean apply(Boolean a) {
      return !a;
    }
  };
}
