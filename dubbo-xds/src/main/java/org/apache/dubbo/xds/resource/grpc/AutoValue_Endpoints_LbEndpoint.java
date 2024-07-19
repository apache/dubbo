package org.apache.dubbo.xds.resource.grpc;

import io.grpc.EquivalentAddressGroup;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Endpoints_LbEndpoint extends Endpoints.LbEndpoint {

  private final EquivalentAddressGroup eag;

  private final int loadBalancingWeight;

  private final boolean isHealthy;

  AutoValue_Endpoints_LbEndpoint(
      EquivalentAddressGroup eag,
      int loadBalancingWeight,
      boolean isHealthy) {
    if (eag == null) {
      throw new NullPointerException("Null eag");
    }
    this.eag = eag;
    this.loadBalancingWeight = loadBalancingWeight;
    this.isHealthy = isHealthy;
  }

  @Override
  EquivalentAddressGroup eag() {
    return eag;
  }

  @Override
  int loadBalancingWeight() {
    return loadBalancingWeight;
  }

  @Override
  boolean isHealthy() {
    return isHealthy;
  }

  @Override
  public String toString() {
    return "LbEndpoint{"
        + "eag=" + eag + ", "
        + "loadBalancingWeight=" + loadBalancingWeight + ", "
        + "isHealthy=" + isHealthy
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Endpoints.LbEndpoint) {
      Endpoints.LbEndpoint that = (Endpoints.LbEndpoint) o;
      return this.eag.equals(that.eag())
          && this.loadBalancingWeight == that.loadBalancingWeight()
          && this.isHealthy == that.isHealthy();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= eag.hashCode();
    h$ *= 1000003;
    h$ ^= loadBalancingWeight;
    h$ *= 1000003;
    h$ ^= isHealthy ? 1231 : 1237;
    return h$;
  }

}
