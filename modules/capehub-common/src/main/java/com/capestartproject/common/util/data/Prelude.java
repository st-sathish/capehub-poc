package com.capestartproject.common.util.data;

/**
 * The prelude contains general purpose functions.
 */
public final class Prelude {
  private Prelude() {
  }

  /**
   * Java is not able to determine the exhaustiveness of a match. Use this function to throw a defined error and to
   * improve readability.
   */
  public static <A> A unexhaustiveMatch() {
    throw new Error("Unexhaustive match");
  }

  /**
   * Java is not able to determine the exhaustiveness of a match. Use this function to throw a defined error and to
   * improve readability.
   */
  public static Error unexhaustiveMatchError() {
    return new Error("Unexhaustive match");
  }

  public static <A> A notYetImplemented() {
    throw new Error("not yet implemented");
  }

  /** Sleep for a while. Returns false if interrupted. */
  public static boolean sleep(long ms) {
    try {
      Thread.sleep(ms);
      return true;
    } catch (InterruptedException ignore) {
      return false;
    }
  }
}
