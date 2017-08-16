package com.capestartproject.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.capestartproject.common.util.data.Function;

/**
 * Synchronization utility to concurrently access a variable set of resources.
 */
public class MultiResourceLock {
  private final ConcurrentHashMap<Object, AtomicInteger> lockMap = new ConcurrentHashMap<Object, AtomicInteger>();

  public MultiResourceLock() {
  }

  /**
   * Synchronize access to a given resource. Execute function <code>function</code> only, if currently now other
   * function accesses resource <code>resource</code>.
   * <p/>
   * Implementation note: The given resource is not used in any synchronization primitives, i.e. no monitor of that
   * object are being held.
   */
  public <A, K> A synchronize(final K resource, final Function<K, A> function) {
    final AtomicInteger counter;
    synchronized (lockMap) {
      AtomicInteger newCounter = new AtomicInteger();
      AtomicInteger currentCounter = lockMap.putIfAbsent(resource, newCounter);
      counter = currentCounter != null ? currentCounter : newCounter;
      counter.incrementAndGet();
    }

    final A ap;
    synchronized (counter) {
      ap = function.apply(resource);
    }

    synchronized (lockMap) {
      if (counter.decrementAndGet() == 0)
        lockMap.remove(resource);
    }
    return ap;
  }

  /** For testing purposes only. */
  int getLockMapSize() {
    return lockMap.size();
  }

}
