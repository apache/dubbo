/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.Filter.FilterConfig;

import com.google.auto.value.AutoValue;
import io.grpc.Status;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** Fault injection configurations. */
abstract class FaultConfig implements FilterConfig {
  @Nullable
  abstract FaultDelay faultDelay();

  @Nullable
  abstract FaultAbort faultAbort();

  @Nullable
  abstract Integer maxActiveFaults();

  @Override
  public final String typeUrl() {
    return FaultFilter.TYPE_URL;
  }

  static FaultConfig create(
      @Nullable FaultDelay faultDelay, @Nullable FaultAbort faultAbort,
      @Nullable Integer maxActiveFaults) {
    return new AutoValue_FaultConfig(faultDelay, faultAbort, maxActiveFaults);
  }

  /** Fault configurations for aborting requests. */
  @AutoValue
  abstract static class FaultDelay {
    @Nullable
    abstract Long delayNanos();

    abstract boolean headerDelay();

    abstract FractionalPercent percent();

    static FaultDelay forFixedDelay(long delayNanos, FractionalPercent percent) {
      return FaultDelay.create(delayNanos, false, percent);
    }

    static FaultDelay forHeader(FractionalPercent percentage) {
      return FaultDelay.create(null, true, percentage);
    }

    private static FaultDelay create(
        @Nullable Long delayNanos, boolean headerDelay, FractionalPercent percent) {
      return new AutoValue_FaultConfig_FaultDelay(delayNanos, headerDelay, percent);
    }
  }

  /** Fault configurations for delaying requests. */
  @AutoValue
  abstract static class FaultAbort {
    @Nullable
    abstract Status status();

    abstract boolean headerAbort();

    abstract FractionalPercent percent();

    static FaultAbort forStatus(Status status, FractionalPercent percent) {
      checkNotNull(status, "status");
      return FaultAbort.create(status, false, percent);
    }

    static FaultAbort forHeader(FractionalPercent percent) {
      return FaultAbort.create(null, true, percent);
    }

    private static FaultAbort create(
        @Nullable Status status, boolean headerAbort, FractionalPercent percent) {
      return new AutoValue_FaultConfig_FaultAbort(status, headerAbort, percent);
    }
  }

  @AutoValue
  abstract static class FractionalPercent {
    enum DenominatorType {
      HUNDRED, TEN_THOUSAND, MILLION
    }

    abstract int numerator();

    abstract DenominatorType denominatorType();

    static FractionalPercent perHundred(int numerator) {
      return FractionalPercent.create(numerator, DenominatorType.HUNDRED);
    }

    static FractionalPercent perTenThousand(int numerator) {
      return FractionalPercent.create(numerator, DenominatorType.TEN_THOUSAND);
    }

    static FractionalPercent perMillion(int numerator) {
      return FractionalPercent.create(numerator, DenominatorType.MILLION);
    }

    static FractionalPercent create(
        int numerator, DenominatorType denominatorType) {
      return new AutoValue_FaultConfig_FractionalPercent(numerator, denominatorType);
    }
  }
}
