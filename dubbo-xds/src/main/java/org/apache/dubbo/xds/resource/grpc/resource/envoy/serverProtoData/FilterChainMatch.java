package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class FilterChainMatch {

    private int destinationPort;
    private ImmutableList<CidrRange> prefixRanges;
    private ImmutableList<String> applicationProtocols;
    private ImmutableList<CidrRange> sourcePrefixRanges;
    private ConnectionSourceType connectionSourceType;
    private ImmutableList<Integer> sourcePorts;
    private ImmutableList<String> serverNames;
    private String transportProtocol;

    public FilterChainMatch(
            int destinationPort,
            ImmutableList<CidrRange> prefixRanges,
            ImmutableList<String> applicationProtocols,
            ImmutableList<CidrRange> sourcePrefixRanges,
            ConnectionSourceType connectionSourceType,
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

    public static FilterChainMatch create(
            int destinationPort,
            ImmutableList<CidrRange> prefixRanges,
            ImmutableList<String> applicationProtocols,
            ImmutableList<CidrRange> sourcePrefixRanges,
            ConnectionSourceType connectionSourceType,
            ImmutableList<Integer> sourcePorts,
            ImmutableList<String> serverNames,
            String transportProtocol) {
        return new FilterChainMatch(destinationPort, prefixRanges, applicationProtocols, sourcePrefixRanges,
                connectionSourceType, sourcePorts, serverNames, transportProtocol);
    }
    // Getters

    public int destinationPort() {
        return destinationPort;
    }

    public ImmutableList<CidrRange> prefixRanges() {
        return prefixRanges;
    }

    public ImmutableList<String> applicationProtocols() {
        return applicationProtocols;
    }

    public ImmutableList<CidrRange> sourcePrefixRanges() {
        return sourcePrefixRanges;
    }

    public ConnectionSourceType connectionSourceType() {
        return connectionSourceType;
    }

    public ImmutableList<Integer> sourcePorts() {
        return sourcePorts;
    }

    public ImmutableList<String> serverNames() {
        return serverNames;
    }

    public String transportProtocol() {
        return transportProtocol;
    }

    // Setters
    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    public String toString() {
        return "FilterChainMatch{" + "destinationPort=" + destinationPort + ", " + "prefixRanges=" + prefixRanges + ", "
                + "applicationProtocols=" + applicationProtocols + ", " + "sourcePrefixRanges=" + sourcePrefixRanges
                + ", " + "connectionSourceType=" + connectionSourceType + ", " + "sourcePorts=" + sourcePorts + ", "
                + "serverNames=" + serverNames + ", " + "transportProtocol=" + transportProtocol + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof FilterChainMatch) {
            FilterChainMatch that = (FilterChainMatch) o;
            return this.destinationPort == that.destinationPort() && this.prefixRanges.equals(that.prefixRanges())
                    && this.applicationProtocols.equals(that.applicationProtocols())
                    && this.sourcePrefixRanges.equals(that.sourcePrefixRanges())
                    && this.connectionSourceType.equals(that.connectionSourceType())
                    && this.sourcePorts.equals(that.sourcePorts()) && this.serverNames.equals(that.serverNames())
                    && this.transportProtocol.equals(that.transportProtocol());
        }
        return false;
    }

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
