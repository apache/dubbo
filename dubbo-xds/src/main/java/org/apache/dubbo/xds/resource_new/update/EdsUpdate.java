package org.apache.dubbo.xds.resource_new.update;

import org.apache.dubbo.xds.resource_new.endpoint.DropOverload;
import org.apache.dubbo.xds.resource_new.common.Locality;
import org.apache.dubbo.xds.resource_new.endpoint.LocalityLbEndpoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EdsUpdate implements ResourceUpdate {
    final String clusterName;
    final Map<Locality, LocalityLbEndpoints> localityLbEndpointsMap;
    final List<DropOverload> dropPolicies;

    public EdsUpdate(
            String clusterName,
            Map<Locality, LocalityLbEndpoints> localityLbEndpoints,
            List<DropOverload> dropPolicies) {
        List<String> nullArgs = new ArrayList<>();
        if (clusterName == null) {
            nullArgs.add("clusterName");
        }
        if (localityLbEndpoints == null) {
            nullArgs.add("localityLbEndpoints");
        }
        if (dropPolicies == null) {
            nullArgs.add("dropPolicies");
        }
        if (!nullArgs.isEmpty()) {
            throw new IllegalArgumentException("Null argument for EdsUpdate: " + String.join(", ", nullArgs));
        }
        this.clusterName = clusterName;
        this.localityLbEndpointsMap = localityLbEndpoints;
        this.dropPolicies = dropPolicies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EdsUpdate that = (EdsUpdate) o;
        return Objects.equals(clusterName, that.clusterName)
                && Objects.equals(localityLbEndpointsMap, that.localityLbEndpointsMap)
                && Objects.equals(dropPolicies, that.dropPolicies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, localityLbEndpointsMap, dropPolicies);
    }

    @Override
    public String toString() {
        return "EdsUpdate{" + "clusterName='" + clusterName + '\'' + ", localityLbEndpointsMap="
                + localityLbEndpointsMap + ", dropPolicies=" + dropPolicies + '}';
    }
}
