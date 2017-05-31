package com.capestartproject.common.util.data;

public abstract class Cell<A> {
  public abstract A get();
  public abstract <B> Cell<B> lift(Function<A, B> f);
  protected abstract Tuple<A, Object> change();
}
