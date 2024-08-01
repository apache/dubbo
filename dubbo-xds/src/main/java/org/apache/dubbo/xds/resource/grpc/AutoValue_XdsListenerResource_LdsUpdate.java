package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;

final class AutoValue_XdsListenerResource_LdsUpdate extends XdsListenerResource.LdsUpdate {

  @Nullable
  private final HttpConnectionManager httpConnectionManager;

  @Nullable
  private final EnvoyServerProtoData.Listener listener;

  AutoValue_XdsListenerResource_LdsUpdate(
      @Nullable HttpConnectionManager httpConnectionManager,
      @Nullable EnvoyServerProtoData.Listener listener) {
    this.httpConnectionManager = httpConnectionManager;
    this.listener = listener;
  }

  @Nullable
  @Override
  HttpConnectionManager httpConnectionManager() {
    return httpConnectionManager;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.Listener listener() {
    return listener;
  }

  @Override
  public String toString() {
    return "LdsUpdate{"
        + "httpConnectionManager=" + httpConnectionManager + ", "
        + "listener=" + listener
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof XdsListenerResource.LdsUpdate) {
      XdsListenerResource.LdsUpdate that = (XdsListenerResource.LdsUpdate) o;
      return (this.httpConnectionManager == null ? that.httpConnectionManager() == null : this.httpConnectionManager.equals(that.httpConnectionManager()))
          && (this.listener == null ? that.listener() == null : this.listener.equals(that.listener()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (httpConnectionManager == null) ? 0 : httpConnectionManager.hashCode();
    h$ *= 1000003;
    h$ ^= (listener == null) ? 0 : listener.hashCode();
    return h$;
  }

}
