package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_GrpcAuthorizationEngine_AuthDecision extends GrpcAuthorizationEngine.AuthDecision {

  private final GrpcAuthorizationEngine.Action decision;

  @Nullable
  private final String matchingPolicyName;

  AutoValue_GrpcAuthorizationEngine_AuthDecision(
      GrpcAuthorizationEngine.Action decision,
      @Nullable String matchingPolicyName) {
    if (decision == null) {
      throw new NullPointerException("Null decision");
    }
    this.decision = decision;
    this.matchingPolicyName = matchingPolicyName;
  }

  @Override
  public GrpcAuthorizationEngine.Action decision() {
    return decision;
  }

  @Nullable
  @Override
  public String matchingPolicyName() {
    return matchingPolicyName;
  }

  @Override
  public String toString() {
    return "AuthDecision{"
        + "decision=" + decision + ", "
        + "matchingPolicyName=" + matchingPolicyName
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.AuthDecision) {
      GrpcAuthorizationEngine.AuthDecision that = (GrpcAuthorizationEngine.AuthDecision) o;
      return this.decision.equals(that.decision())
          && (this.matchingPolicyName == null ? that.matchingPolicyName() == null : this.matchingPolicyName.equals(that.matchingPolicyName()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= decision.hashCode();
    h$ *= 1000003;
    h$ ^= (matchingPolicyName == null) ? 0 : matchingPolicyName.hashCode();
    return h$;
  }

}
