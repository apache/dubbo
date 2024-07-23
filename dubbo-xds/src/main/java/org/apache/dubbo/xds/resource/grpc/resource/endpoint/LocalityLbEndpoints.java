package org.apache.dubbo.xds.resource.grpc.resource.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalityLbEndpoints {

    private final List<LbEndpoint> endpoints;

    private final int localityWeight;

    private final int priority;

    public LocalityLbEndpoints(
            List<LbEndpoint> endpoints,
            int localityWeight,
            int priority) {
        if (endpoints == null) {
            throw new NullPointerException("Null endpoints");
        }
        this.endpoints = Collections.unmodifiableList(new ArrayList<>(endpoints));
        this.localityWeight = localityWeight;
        this.priority = priority;
    }

    List<LbEndpoint> endpoints() {
        return endpoints;
    }

    public int localityWeight() {
        return localityWeight;
    }

    public int priority() {
        return priority;
    }

    public String toString() {
        return "LocalityLbEndpoints{"
                + "endpoints=" + endpoints + ", "
                + "localityWeight=" + localityWeight + ", "
                + "priority=" + priority
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof LocalityLbEndpoints) {
            LocalityLbEndpoints that = (LocalityLbEndpoints) o;
            return this.endpoints.equals(that.endpoints())
                    && this.localityWeight == that.localityWeight()
                    && this.priority == that.priority();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= endpoints.hashCode();
        h$ *= 1000003;
        h$ ^= localityWeight;
        h$ *= 1000003;
        h$ ^= priority;
        return h$;
    }

}
