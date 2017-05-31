package com.capestartproject.common.util.data;

/**
 * Element factory.
 *
 * @param <A>
 *         the type of the element
 */
public interface Creator<A> {
  A create();
}

