package com.imranzahid.backup.util;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public final class Stopwatch {
  private boolean isRunning;
  private long elapsedNanos;
  private long startTick;

  public static Stopwatch createStarted() {
    return new Stopwatch().start();
  }

  Stopwatch() {}

  public Stopwatch start() {
    if (isRunning) {
      throw new RuntimeException("This stopwatch is already running.");
    }
    isRunning = true;
    startTick = System.nanoTime();
    return this;
  }

  public Stopwatch stop() {
    long tick = System.nanoTime();
    if (!isRunning) {
      throw new RuntimeException("This stopwatch is already stopped.");
    }
    isRunning = false;
    elapsedNanos += tick - startTick;
    return this;
  }

  private long elapsedNanos() {
    return isRunning ? System.nanoTime() - startTick + elapsedNanos : elapsedNanos;
  }

  public long elapsed(TimeUnit desiredUnit) {
    return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
  }
}
