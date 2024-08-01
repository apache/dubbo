package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.FilterChain;

import com.google.common.collect.ImmutableList;

class AutoValue_EnvoyServerProtoData_Listener extends EnvoyServerProtoData.Listener {

  private final String name;

  @Nullable
  private final String address;

  private final ImmutableList<FilterChain> filterChains;

  @Nullable
  private final EnvoyServerProtoData.FilterChain defaultFilterChain;

  AutoValue_EnvoyServerProtoData_Listener(
      String name,
      @Nullable String address,
      ImmutableList<EnvoyServerProtoData.FilterChain> filterChains,
      @Nullable EnvoyServerProtoData.FilterChain defaultFilterChain) {
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    this.address = address;
    if (filterChains == null) {
      throw new NullPointerException("Null filterChains");
    }
    this.filterChains = filterChains;
    this.defaultFilterChain = defaultFilterChain;
  }

  @Override
  String name() {
    return name;
  }

  @Nullable
  @Override
  String address() {
    return address;
  }

  @Override
  ImmutableList<EnvoyServerProtoData.FilterChain> filterChains() {
    return filterChains;
  }

  @Nullable
  @Override
  EnvoyServerProtoData.FilterChain defaultFilterChain() {
    return defaultFilterChain;
  }

  @Override
  public String toString() {
    return "Listener{"
        + "name=" + name + ", "
        + "address=" + address + ", "
        + "filterChains=" + filterChains + ", "
        + "defaultFilterChain=" + defaultFilterChain
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EnvoyServerProtoData.Listener) {
      EnvoyServerProtoData.Listener that = (EnvoyServerProtoData.Listener) o;
      return this.name.equals(that.name())
          && (this.address == null ? that.address() == null : this.address.equals(that.address()))
          && this.filterChains.equals(that.filterChains())
          && (this.defaultFilterChain == null ? that.defaultFilterChain() == null : this.defaultFilterChain.equals(that.defaultFilterChain()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= (address == null) ? 0 : address.hashCode();
    h$ *= 1000003;
    h$ ^= filterChains.hashCode();
    h$ *= 1000003;
    h$ ^= (defaultFilterChain == null) ? 0 : defaultFilterChain.hashCode();
    return h$;
  }

}
