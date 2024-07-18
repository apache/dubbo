package org.apache.dubbo.xds.resource.grpc;

final class AutoValue_GrpcAuthorizationEngine_PolicyMatcher extends GrpcAuthorizationEngine.PolicyMatcher {

  private final String name;

  private final GrpcAuthorizationEngine.OrMatcher permissions;

  private final GrpcAuthorizationEngine.OrMatcher principals;

  AutoValue_GrpcAuthorizationEngine_PolicyMatcher(
      String name,
      GrpcAuthorizationEngine.OrMatcher permissions,
      GrpcAuthorizationEngine.OrMatcher principals) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (permissions == null) {
      throw new NullPointerException("Null permissions");
    }
    this.permissions = permissions;
    if (principals == null) {
      throw new NullPointerException("Null principals");
    }
    this.principals = principals;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public GrpcAuthorizationEngine.OrMatcher permissions() {
    return permissions;
  }

  @Override
  public GrpcAuthorizationEngine.OrMatcher principals() {
    return principals;
  }

  @Override
  public String toString() {
    return "PolicyMatcher{"
        + "name=" + name + ", "
        + "permissions=" + permissions + ", "
        + "principals=" + principals
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof GrpcAuthorizationEngine.PolicyMatcher) {
      GrpcAuthorizationEngine.PolicyMatcher that = (GrpcAuthorizationEngine.PolicyMatcher) o;
      return this.name.equals(that.name())
          && this.permissions.equals(that.permissions())
          && this.principals.equals(that.principals());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= permissions.hashCode();
    h$ *= 1000003;
    h$ ^= principals.hashCode();
    return h$;
  }

}
