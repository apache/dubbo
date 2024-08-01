package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_EnvoyServerProtoData_SuccessRateEjection extends EnvoyServerProtoData.SuccessRateEjection {

  @Nullable
  private final Integer stdevFactor;

  @Nullable
  private final Integer enforcementPercentage;

  @Nullable
  private final Integer minimumHosts;

  @Nullable
  private final Integer requestVolume;

  AutoValue_EnvoyServerProtoData_SuccessRateEjection(
      @Nullable Integer stdevFactor,
      @Nullable Integer enforcementPercentage,
      @Nullable Integer minimumHosts,
      @Nullable Integer requestVolume) {
    this.stdevFactor = stdevFactor;
    this.enforcementPercentage = enforcementPercentage;
    this.minimumHosts = minimumHosts;
    this.requestVolume = requestVolume;
  }

  @Nullable
  @Override
  Integer stdevFactor() {
    return stdevFactor;
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
    return "SuccessRateEjection{"
        + "stdevFactor=" + stdevFactor + ", "
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
    if (o instanceof EnvoyServerProtoData.SuccessRateEjection) {
      EnvoyServerProtoData.SuccessRateEjection that = (EnvoyServerProtoData.SuccessRateEjection) o;
      return (this.stdevFactor == null ? that.stdevFactor() == null : this.stdevFactor.equals(that.stdevFactor()))
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
    h$ ^= (stdevFactor == null) ? 0 : stdevFactor.hashCode();
    h$ *= 1000003;
    h$ ^= (enforcementPercentage == null) ? 0 : enforcementPercentage.hashCode();
    h$ *= 1000003;
    h$ ^= (minimumHosts == null) ? 0 : minimumHosts.hashCode();
    h$ *= 1000003;
    h$ ^= (requestVolume == null) ? 0 : requestVolume.hashCode();
    return h$;
  }

}
