package org.apache.dubbo.xds.resource_new.endpoint;

import org.apache.dubbo.common.url.component.URLAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LbEndpoint {

    private final List<URLAddress> addresses;

    private final int loadBalancingWeight;

    private final boolean isHealthy;

    public LbEndpoint(
            List<URLAddress> addresses, int loadBalancingWeight, boolean isHealthy) {
        if (addresses == null) {
            throw new NullPointerException("Null addresses");
        }
        this.addresses = Collections.unmodifiableList(new ArrayList<>(addresses));
        this.loadBalancingWeight = loadBalancingWeight;
        this.isHealthy = isHealthy;
    }

    List<URLAddress> addresses() {
        return addresses;
    }

    int loadBalancingWeight() {
        return loadBalancingWeight;
    }

    boolean isHealthy() {
        return isHealthy;
    }

    @Override
    public String toString() {
        return "LbEndpoint{" + "addresses=" + addresses + ", " + "loadBalancingWeight=" + loadBalancingWeight + ", "
                + "isHealthy=" + isHealthy + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof LbEndpoint) {
            LbEndpoint that = (LbEndpoint) o;
            return this.addresses.equals(that.addresses()) && this.loadBalancingWeight == that.loadBalancingWeight()
                    && this.isHealthy == that.isHealthy();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= addresses.hashCode();
        h$ *= 1000003;
        h$ ^= loadBalancingWeight;
        h$ *= 1000003;
        h$ ^= isHealthy ? 1231 : 1237;
        return h$;
    }

}
