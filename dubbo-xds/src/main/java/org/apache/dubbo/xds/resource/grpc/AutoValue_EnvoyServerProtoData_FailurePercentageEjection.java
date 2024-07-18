package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_EnvoyServerProtoData_FailurePercentageEjection extends EnvoyServerProtoData.FailurePercentageEjection {

  @Nullable
  private final Integer threshold;

  @Nullable
  private final Integer enforcementPercentage;

  @Nullable
  private final Integer minimumHosts;

  @Nullable
  private final Integer requestVolume;

  AutoValue_EnvoyServerProtoData_FailurePercentageEjection(
      @Nullable Integer threshold,
      @Nullable Integer enforcementPercentage,
      @Nullable Integer minimumHosts,
      @Nullable Integer requestVolume) {
    this.threshold = threshold;
    this.enforcementPercentage = enforcementPercentage;
    this.minimumHosts = minimumHosts;
    this.requestVolume = requestVolume;
  }

  @Nullable
  @Override
  Integer threshold() {
    return threshold;
  }

  @Nullable
  @Override
  Integer enforcementPercentage() {
    return enforcementPercentage;
  }

  @Nullable
  @Override
  Integer minimumHosts() {
    return minimumHosts;
  }

  @Nullable
  @Override
  Integer requestVolume() {
    return requestVolume;
  }

  @Override
  public String toString() {
    return "FailurePercentageEjection{"
        + "threshold=" + threshold + ", "
        + "enforcementPercentage=" + enforcementPercentage + ", "
        + "minimumHosts=" + minimumHosts + ", "
        + "requestVolume=" + requestVolume
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EnvoyServerProtoData.FailurePercentageEjection) {
      EnvoyServerProtoData.FailurePercentageEjection that = (EnvoyServerProtoData.FailurePercentageEjection) o;
      return (this.threshold == null ? that.threshold() == null : this.threshold.equals(that.threshold()))
          && (this.enforcementPercentage == null ? that.enforcementPercentage() == null : this.enforcementPercentage.equals(that.enforcementPercentage()))
          && (this.minimumHosts == null ? that.minimumHosts() == null : this.minimumHosts.equals(that.minimumHosts()))
          && (this.requestVolume == null ? that.requestVolume() == null : this.requestVolume.equals(that.requestVolume()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (threshold == null) ? 0 : threshold.hashCode();
    h$ *= 1000003;
    h$ ^= (enforcementPercentage == null) ? 0 : enforcementPercentage.hashCode();
    h$ *= 1000003;
    h$ ^= (minimumHosts == null) ? 0 : minimumHosts.hashCode();
    h$ *= 1000003;
    h$ ^= (requestVolume == null) ? 0 : requestVolume.hashCode();
    return h$;
  }

}
