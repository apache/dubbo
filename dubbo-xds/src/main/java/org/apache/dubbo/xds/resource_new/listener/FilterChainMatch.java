package org.apache.dubbo.xds.resource_new.listener;

import org.apache.dubbo.xds.resource_new.listener.security.ConnectionSourceType;
import org.apache.dubbo.xds.resource_new.common.CidrRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilterChainMatch {

    private int destinationPort;
    private List<CidrRange> prefixRanges;
    private List<String> applicationProtocols;
    private List<CidrRange> sourcePrefixRanges;
    private ConnectionSourceType connectionSourceType;
    private List<Integer> sourcePorts;
    private List<String> serverNames;
    private String transportProtocol;

    public FilterChainMatch(
            int destinationPort,
            List<CidrRange> prefixRanges,
            List<String> applicationProtocols,
            List<CidrRange> sourcePrefixRanges,
            ConnectionSourceType connectionSourceType,
            List<Integer> sourcePorts,
            List<String> serverNames,
            String transportProtocol) {
        this.destinationPort = destinationPort;
        if (prefixRanges == null) {
            throw new NullPointerException("Null prefixRanges");
        }
        this.prefixRanges = Collections.unmodifiableList(new ArrayList<>(prefixRanges));
        if (applicationProtocols == null) {
            throw new NullPointerException("Null applicationProtocols");
        }
        this.applicationProtocols = Collections.unmodifiableList(new ArrayList<>(applicationProtocols));
        if (sourcePrefixRanges == null) {
            throw new NullPointerException("Null sourcePrefixRanges");
        }
        this.sourcePrefixRanges = Collections.unmodifiableList(new ArrayList<>(sourcePrefixRanges));
        if (connectionSourceType == null) {
            throw new NullPointerException("Null connectionSourceType");
        }
        this.connectionSourceType = connectionSourceType;
        if (sourcePorts == null) {
            throw new NullPointerException("Null sourcePorts");
        }
        this.sourcePorts = Collections.unmodifiableList(new ArrayList<>(sourcePorts));
        if (serverNames == null) {
            throw new NullPointerException("Null serverNames");
        }
        this.serverNames = Collections.unmodifiableList(new ArrayList<>(serverNames));
        if (transportProtocol == null) {
            throw new NullPointerException("Null transportProtocol");
        }
        this.transportProtocol = transportProtocol;
    }

    public static FilterChainMatch create(
            int destinationPort,
            List<CidrRange> prefixRanges,
            List<String> applicationProtocols,
            List<CidrRange> sourcePrefixRanges,
            ConnectionSourceType connectionSourceType,
            List<Integer> sourcePorts,
            List<String> serverNames,
            String transportProtocol) {
        return new FilterChainMatch(destinationPort, prefixRanges, applicationProtocols, sourcePrefixRanges,
                connectionSourceType, sourcePorts, serverNames, transportProtocol);
    }
    // Getters

    public int destinationPort() {
        return destinationPort;
    }

    public List<CidrRange> prefixRanges() {
        return prefixRanges;
    }

    public List<String> applicationProtocols() {
        return applicationProtocols;
    }

    public List<CidrRange> sourcePrefixRanges() {
        return sourcePrefixRanges;
    }

    public ConnectionSourceType connectionSourceType() {
        return connectionSourceType;
    }

    public List<Integer> sourcePorts() {
        return sourcePorts;
    }

    public List<String> serverNames() {
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
