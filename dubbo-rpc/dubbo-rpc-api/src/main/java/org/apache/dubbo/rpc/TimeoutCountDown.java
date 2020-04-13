/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc;

import java.util.concurrent.TimeUnit;

public final class TimeoutCountDown implements Comparable<TimeoutCountDown> {

  public static TimeoutCountDown newCountDown(long timeout, TimeUnit unit) {
    return new TimeoutCountDown(timeout, unit);
  }

  private final long timeoutInMillis;
  private final long deadlineInNanos;
  private volatile boolean expired;

  private TimeoutCountDown(long timeout, TimeUnit unit) {
    timeoutInMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);
    deadlineInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
  }

  public long getTimeoutInMilli() {
    return timeoutInMillis;
  }

  public boolean isExpired() {
    if (!expired) {
      if (deadlineInNanos - System.nanoTime() <= 0) {
        expired = true;
      } else {
        return false;
      }
    }
    return true;
  }

  public long timeRemaining(TimeUnit unit) {
    final long currentNanos = System.nanoTime();
    if (!expired && deadlineInNanos - currentNanos <= 0) {
      expired = true;
    }
    return unit.convert(deadlineInNanos - currentNanos, TimeUnit.NANOSECONDS);
  }

  public long elapsedMillis() {
    if (isExpired()) {
      return timeoutInMillis + TimeUnit.MILLISECONDS.convert(System.nanoTime() - deadlineInNanos, TimeUnit.NANOSECONDS);
    } else {
      return TimeUnit.MILLISECONDS.convert(deadlineInNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }
  }

  @Override
  public String toString() {
    long timeoutMillis = TimeUnit.MILLISECONDS.convert(deadlineInNanos, TimeUnit.NANOSECONDS);
    long remainingMillis = timeRemaining(TimeUnit.MILLISECONDS);

    StringBuilder buf = new StringBuilder();
    buf.append("Total timeout value - ");
    buf.append(timeoutMillis);
    buf.append(", times remaining - ");
    buf.append(remainingMillis);
    return buf.toString();
  }

  @Override
  public int compareTo(TimeoutCountDown another) {
    long delta = this.deadlineInNanos - another.deadlineInNanos;
    if (delta < 0) {
      return -1;
    } else if (delta > 0) {
      return 1;
    }
    return 0;
  }
}
