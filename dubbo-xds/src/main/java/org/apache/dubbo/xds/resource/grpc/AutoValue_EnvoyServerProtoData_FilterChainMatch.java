package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.CidrRange;

import com.google.common.collect.ImmutableList;

final class AutoValue_EnvoyServerProtoData_FilterChainMatch extends EnvoyServerProtoData.FilterChainMatch {

  private final int destinationPort;

  private final ImmutableList<CidrRange> prefixRanges;

  private final ImmutableList<String> applicationProtocols;

  private final ImmutableList<EnvoyServerProtoData.CidrRange> sourcePrefixRanges;

  private final EnvoyServerProtoData.ConnectionSourceType connectionSourceType;

  private final ImmutableList<Integer> sourcePorts;

  private final ImmutableList<String> serverNames;

  private final String transportProtocol;

  AutoValue_EnvoyServerProtoData_FilterChainMatch(
      int destinationPort,
      ImmutableList<EnvoyServerProtoData.CidrRange> prefixRanges,
      ImmutableList<String> applicationProtocols,
      ImmutableList<EnvoyServerProtoData.CidrRange> sourcePrefixRanges,
      EnvoyServerProtoData.ConnectionSourceType connectionSourceType,
      ImmutableList<Integer> sourcePorts,
      ImmutableList<String> serverNames,
      String transportProtocol) {
    this.destinationPort = destinationPort;
    if (prefixRanges == null) {
      throw new NullPointerException("Null prefixRanges");
    }
    this.prefixRanges = prefixRanges;
    if (applicationProtocols == null) {
      throw new NullPointerException("Null applicationProtocols");
    }
    this.applicationProtocols = applicationProtocols;
    if (sourcePrefixRanges == null) {
      throw new NullPointerException("Null sourcePrefixRanges");
    }
    this.sourcePrefixRanges = sourcePrefixRanges;
    if (connectionSourceType == null) {
      throw new NullPointerException("Null connectionSourceType");
    }
    this.connectionSourceType = connectionSourceType;
    if (sourcePorts == null) {
      throw new NullPointerException("Null sourcePorts");
    }
    this.sourcePorts = sourcePorts;
    if (serverNames == null) {
      throw new NullPointerException("Null serverNames");
    }
    this.serverNames = serverNames;
    if (transportProtocol == null) {
      throw new NullPointerException("Null transportProtocol");
    }
    this.transportProtocol = transportProtocol;
  }

  @Override
  int destinationPort() {
    return destinationPort;
  }

  @Override
  ImmutableList<EnvoyServerProtoData.CidrRange> prefixRanges() {
    return prefixRanges;
  }

  @Override
  ImmutableList<String> applicationProtocols() {
    return applicationProtocols;
  }

  @Override
  ImmutableList<EnvoyServerProtoData.CidrRange> sourcePrefixRanges() {
    return sourcePrefixRanges;
  }

  @Override
  EnvoyServerProtoData.ConnectionSourceType connectionSourceType() {
    return connectionSourceType;
  }

  @Override
  ImmutableList<Integer> sourcePorts() {
    return sourcePorts;
  }

  @Override
  ImmutableList<String> serverNames() {
    return serverNames;
  }

  @Override
  String transportProtocol() {
    return transportProtocol;
  }

  @Override
  public String toString() {
    return "FilterChainMatch{"
        + "destinationPort=" + destinationPort + ", "
        + "prefixRanges=" + prefixRanges + ", "
        + "applicationProtocols=" + applicationProtocols + ", "
        + "sourcePrefixRanges=" + sourcePrefixRanges + ", "
        + "connectionSourceType=" + connectionSourceType + ", "
        + "sourcePorts=" + sourcePorts + ", "
        + "serverNames=" + serverNames + ", "
        + "transportProtocol=" + transportProtocol
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof EnvoyServerProtoData.FilterChainMatch) {
      EnvoyServerProtoData.FilterChainMatch that = (EnvoyServerProtoData.FilterChainMatch) o;
      return this.destinationPort == that.destinationPort()
          && this.prefixRanges.equals(that.prefixRanges())
          && this.applicationProtocols.equals(that.applicationProtocols())
          && this.sourcePrefixRanges.equals(that.sourcePrefixRanges())
          && this.connectionSourceType.equals(that.connectionSourceType())
          && this.sourcePorts.equals(that.sourcePorts())
          && this.serverNames.equals(that.serverNames())
          && this.transportProtocol.equals(that.transportProtocol());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= destinationPort;
    h$ *= 1000003;
    h$ ^= prefixRanges.hashCode();
    h$ *= 1000003;
    h$ ^= applicationProtocols.hashCode();
    h$ *= 1000003;
    h$ ^= sourcePrefixRanges.hashCode();
    h$ *= 1000003;
    h$ ^= connectionSourceType.hashCode();
    h$ *= 1000003;
    h$ ^= sourcePorts.hashCode();
    h$ *= 1000003;
    h$ ^= serverNames.hashCode();
    h$ *= 1000003;
    h$ ^= transportProtocol.hashCode();
    return h$;
  }

}
