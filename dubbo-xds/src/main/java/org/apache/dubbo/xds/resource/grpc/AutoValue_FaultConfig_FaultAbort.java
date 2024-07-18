package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

import io.grpc.Status;

final class AutoValue_FaultConfig_FaultAbort extends FaultConfig.FaultAbort {

  @Nullable
  private final Status status;

  private final boolean headerAbort;

  private final FaultConfig.FractionalPercent percent;

  AutoValue_FaultConfig_FaultAbort(
      @Nullable Status status,
      boolean headerAbort,
      FaultConfig.FractionalPercent percent) {
    this.status = status;
    this.headerAbort = headerAbort;
    if (percent == null) {
      throw new NullPointerException("Null percent");
    }
    this.percent = percent;
  }

  @Nullable
  @Override
  Status status() {
    return status;
  }

  @Override
  boolean headerAbort() {
    return headerAbort;
  }

  @Override
  FaultConfig.FractionalPercent percent() {
    return percent;
  }

  @Override
  public String toString() {
    return "FaultAbort{"
        + "status=" + status + ", "
        + "headerAbort=" + headerAbort + ", "
        + "percent=" + percent
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof FaultConfig.FaultAbort) {
      FaultConfig.FaultAbort that = (FaultConfig.FaultAbort) o;
      return (this.status == null ? that.status() == null : this.status.equals(that.status()))
          && this.headerAbort == that.headerAbort()
          && this.percent.equals(that.percent());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (status == null) ? 0 : status.hashCode();
    h$ *= 1000003;
    h$ ^= headerAbort ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= percent.hashCode();
    return h$;
  }

}
