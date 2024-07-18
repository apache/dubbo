package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_EnvoyServerProtoData_OutlierDetection extends EnvoyServerProtoData.OutlierDetection {

  @Nullable
  private final Long intervalNanos;

  @Nullable
  private final Long baseEjectionTimeNanos;

  @Nullable
  private final Long maxEjectionTimeNanos;

  @Nullable
  private final Integer maxEjectionPercent;

  @Nullable
  private final EnvoyServerProtoData.SuccessRateEjection successRateEjection;

  @Nullable
  private final EnvoyServerProtoData.FailurePercentageEjection failurePercentageEjection;

  AutoValue_EnvoyServerProtoData_OutlierDetection(
      @Nullable Long intervalNanos,
      @Nullable Long baseEjectionTimeNanos,
      @Nullable Long maxEjectionTimeNanos,
      @Nullable Integer maxEjectionPercent,
      @Nullable EnvoyServerProtoData.SuccessRateEjection successRateEjection,
      @Nullable EnvoyServerProtoData.FailurePercentageEjection failurePercentageEjection) {
    this.intervalNanos = intervalNanos;
    this.baseEjectionTimeNanos = baseEjectionTimeNanos;
    this.maxEjectionTimeNanos = maxEjectionTimeNanos;
    this.maxEjectionPercent = maxEjectionPercent;
    this.successRateEjection = successRateEjection;
    this.failurePercentageEjection = failurePercentageEjection;
  }

  @Nullable
  @Override
  Long intervalNanos() {
    return intervalNanos;
  }

  @Nullable
  @Override
  Long baseEjectionTimeNanos() {
    return baseEjectionTimeNanos;
  }

  @Nullable
  @Override
  Long maxEjectionTimeNanos() {
    return maxEjectionTimeNanos;
  }

  @Nullable
  @Override
  Integer maxEjectionPercent() {
    return maxEjectionPercent;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.SuccessRateEjection successRateEjection() {
    return successRateEjection;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.FailurePercentageEjection failurePercentageEjection() {
    return failurePercentageEjection;
  }

  @Override
  public String toString() {
    return "OutlierDetection{"
        + "intervalNanos=" + intervalNanos + ", "
        + "baseEjectionTimeNanos=" + baseEjectionTimeNanos + ", "
        + "maxEjectionTimeNanos=" + maxEjectionTimeNanos + ", "
        + "maxEjectionPercent=" + maxEjectionPercent + ", "
        + "successRateEjection=" + successRateEjection + ", "
        + "failurePercentageEjection=" + failurePercentageEjection
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EnvoyServerProtoData.OutlierDetection) {
      EnvoyServerProtoData.OutlierDetection that = (EnvoyServerProtoData.OutlierDetection) o;
      return (this.intervalNanos == null ? that.intervalNanos() == null : this.intervalNanos.equals(that.intervalNanos()))
          && (this.baseEjectionTimeNanos == null ? that.baseEjectionTimeNanos() == null : this.baseEjectionTimeNanos.equals(that.baseEjectionTimeNanos()))
          && (this.maxEjectionTimeNanos == null ? that.maxEjectionTimeNanos() == null : this.maxEjectionTimeNanos.equals(that.maxEjectionTimeNanos()))
          && (this.maxEjectionPercent == null ? that.maxEjectionPercent() == null : this.maxEjectionPercent.equals(that.maxEjectionPercent()))
          && (this.successRateEjection == null ? that.successRateEjection() == null : this.successRateEjection.equals(that.successRateEjection()))
          && (this.failurePercentageEjection == null ? that.failurePercentageEjection() == null : this.failurePercentageEjection.equals(that.failurePercentageEjection()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (intervalNanos == null) ? 0 : intervalNanos.hashCode();
    h$ *= 1000003;
    h$ ^= (baseEjectionTimeNanos == null) ? 0 : baseEjectionTimeNanos.hashCode();
    h$ *= 1000003;
    h$ ^= (maxEjectionTimeNanos == null) ? 0 : maxEjectionTimeNanos.hashCode();
    h$ *= 1000003;
    h$ ^= (maxEjectionPercent == null) ? 0 : maxEjectionPercent.hashCode();
    h$ *= 1000003;
    h$ ^= (successRateEjection == null) ? 0 : successRateEjection.hashCode();
    h$ *= 1000003;
    h$ ^= (failurePercentageEjection == null) ? 0 : failurePercentageEjection.hashCode();
    return h$;
  }

}
