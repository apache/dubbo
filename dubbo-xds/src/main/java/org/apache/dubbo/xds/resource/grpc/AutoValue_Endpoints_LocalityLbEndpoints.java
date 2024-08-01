package org.apache.dubbo.xds.resource.grpc;

import com.google.common.collect.ImmutableList;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Endpoints_LocalityLbEndpoints extends Endpoints.LocalityLbEndpoints {

  private final ImmutableList<Endpoints.LbEndpoint> endpoints;

  private final int localityWeight;

  private final int priority;

  AutoValue_Endpoints_LocalityLbEndpoints(
      ImmutableList<Endpoints.LbEndpoint> endpoints,
      int localityWeight,
      int priority) {
    if (endpoints == null) {
      throw new NullPointerException("Null endpoints");
    }
    this.endpoints = endpoints;
    this.localityWeight = localityWeight;
    this.priority = priority;
  }

  @Override
  ImmutableList<Endpoints.LbEndpoint> endpoints() {
    return endpoints;
  }

  @Override
  int localityWeight() {
    return localityWeight;
  }

  @Override
  int priority() {
    return priority;
  }

  @Override
  public String toString() {
    return "LocalityLbEndpoints{"
        + "endpoints=" + endpoints + ", "
        + "localityWeight=" + localityWeight + ", "
        + "priority=" + priority
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Endpoints.LocalityLbEndpoints) {
      Endpoints.LocalityLbEndpoints that = (Endpoints.LocalityLbEndpoints) o;
      return this.endpoints.equals(that.endpoints())
          && this.localityWeight == that.localityWeight()
          && this.priority == that.priority();
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= endpoints.hashCode();
    h$ *= 1000003;
    h$ ^= localityWeight;
    h$ *= 1000003;
    h$ ^= priority;
    return h$;
  }

}
