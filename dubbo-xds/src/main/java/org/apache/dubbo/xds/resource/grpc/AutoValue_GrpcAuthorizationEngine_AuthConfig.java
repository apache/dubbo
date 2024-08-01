package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.GrpcAuthorizationEngine.PolicyMatcher;

import com.google.common.collect.ImmutableList;

final class AutoValue_GrpcAuthorizationEngine_AuthConfig extends GrpcAuthorizationEngine.AuthConfig {

  private final ImmutableList<PolicyMatcher> policies;

  private final GrpcAuthorizationEngine.Action action;

  AutoValue_GrpcAuthorizationEngine_AuthConfig(
      ImmutableList<GrpcAuthorizationEngine.PolicyMatcher> policies,
      GrpcAuthorizationEngine.Action action) {
    if (policies == null) {
      throw new NullPointerException("Null policies");
    }
    this.policies = policies;
    if (action == null) {
      throw new NullPointerException("Null action");
    }
    this.action = action;
  }

  @Override
  public ImmutableList<GrpcAuthorizationEngine.PolicyMatcher> policies() {
    return policies;
  }

  @Override
  public GrpcAuthorizationEngine.Action action() {
    return action;
  }

  @Override
  public String toString() {
    return "AuthConfig{"
        + "policies=" + policies + ", "
        + "action=" + action
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AuthConfig) {
      GrpcAuthorizationEngine.AuthConfig that = (GrpcAuthorizationEngine.AuthConfig) o;
      return this.policies.equals(that.policies())
          && this.action.equals(that.action());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= policies.hashCode();
    h$ *= 1000003;
    h$ ^= action.hashCode();
    return h$;
  }

}
