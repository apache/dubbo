package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_RbacConfig extends RbacConfig {

  @Nullable
  private final GrpcAuthorizationEngine.AuthConfig authConfig;

  AutoValue_RbacConfig(
      @Nullable GrpcAuthorizationEngine.AuthConfig authConfig) {
    this.authConfig = authConfig;
  }

  @Nullable
  @Override
  GrpcAuthorizationEngine.AuthConfig authConfig() {
    return authConfig;
  }

  @Override
  public String toString() {
    return "RbacConfig{"
        + "authConfig=" + authConfig
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RbacConfig) {
      RbacConfig that = (RbacConfig) o;
      return (this.authConfig == null ? that.authConfig() == null : this.authConfig.equals(that.authConfig()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (authConfig == null) ? 0 : authConfig.hashCode();
    return h$;
  }

}
