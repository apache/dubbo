/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.xds.resource_new.listener;

import org.apache.dubbo.xds.resource_new.common.CidrRange;
import org.apache.dubbo.xds.resource_new.listener.security.ConnectionSourceType;

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
        return new FilterChainMatch(
                destinationPort,
                prefixRanges,
                applicationProtocols,
                sourcePrefixRanges,
                connectionSourceType,
                sourcePorts,
                serverNames,
                transportProtocol);
    }
    // Getters

    public int getDestinationPort() {
        return destinationPort;
    }

    public List<CidrRange> getPrefixRanges() {
        return prefixRanges;
    }

    public List<String> getApplicationProtocols() {
        return applicationProtocols;
    }

    public List<CidrRange> getSourcePrefixRanges() {
        return sourcePrefixRanges;
    }

    public ConnectionSourceType getConnectionSourceType() {
        return connectionSourceType;
    }

    public List<Integer> getSourcePorts() {
        return sourcePorts;
    }

    public List<String> getServerNames() {
        return serverNames;
    }

    public String getTransportProtocol() {
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
            return this.destinationPort == that.getDestinationPort()
                    && this.prefixRanges.equals(that.getPrefixRanges())
                    && this.applicationProtocols.equals(that.getApplicationProtocols())
                    && this.sourcePrefixRanges.equals(that.getSourcePrefixRanges())
                    && this.connectionSourceType.equals(that.getConnectionSourceType())
                    && this.sourcePorts.equals(that.getSourcePorts())
                    && this.serverNames.equals(that.getServerNames())
                    && this.transportProtocol.equals(that.getTransportProtocol());
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
