package de.tudresden.inf.st.mquat.utils;

import java.util.concurrent.TimeUnit;

public class StopWatch {
  private long starts;

  /**
   * Starts a measurement.
   * @return a new StopWatch
   */
  public static StopWatch start() {
    return new StopWatch();
  }

  private StopWatch() {
    reset();
  }

  /**
   * Restarts the measurement.
   */
  public StopWatch reset() {
    starts = System.nanoTime();
    return this;
  }

  /**
   * @return elapsed time in nanoseconds
   */
  public long time() {
    long ends = System.nanoTime();
    return ends - starts;
  }

  /**
   * @return elapsed time in the given TimeUnit
   */
  public long time(TimeUnit unit) {
    return unit.convert(time(), TimeUnit.NANOSECONDS);
  }
}
