package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_FaultConfig extends FaultConfig {

  @Nullable
  private final FaultConfig.FaultDelay faultDelay;

  @Nullable
  private final FaultConfig.FaultAbort faultAbort;

  @Nullable
  private final Integer maxActiveFaults;

  AutoValue_FaultConfig(
      @Nullable FaultConfig.FaultDelay faultDelay,
      @Nullable FaultConfig.FaultAbort faultAbort,
      @Nullable Integer maxActiveFaults) {
    this.faultDelay = faultDelay;
    this.faultAbort = faultAbort;
    this.maxActiveFaults = maxActiveFaults;
  }

  @Nullable
  @Override
  FaultConfig.FaultDelay faultDelay() {
    return faultDelay;
  }

  @Nullable
  @Override
  FaultConfig.FaultAbort faultAbort() {
    return faultAbort;
  }

  @Nullable
  @Override
  Integer maxActiveFaults() {
    return maxActiveFaults;
  }

  @Override
  public String toString() {
    return "FaultConfig{"
        + "faultDelay=" + faultDelay + ", "
        + "faultAbort=" + faultAbort + ", "
        + "maxActiveFaults=" + maxActiveFaults
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FaultConfig) {
      FaultConfig that = (FaultConfig) o;
      return (this.faultDelay == null ? that.faultDelay() == null : this.faultDelay.equals(that.faultDelay()))
          && (this.faultAbort == null ? that.faultAbort() == null : this.faultAbort.equals(that.faultAbort()))
          && (this.maxActiveFaults == null ? that.maxActiveFaults() == null : this.maxActiveFaults.equals(that.maxActiveFaults()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (faultDelay == null) ? 0 : faultDelay.hashCode();
    h$ *= 1000003;
    h$ ^= (faultAbort == null) ? 0 : faultAbort.hashCode();
    h$ *= 1000003;
    h$ ^= (maxActiveFaults == null) ? 0 : maxActiveFaults.hashCode();
    return h$;
  }

}
