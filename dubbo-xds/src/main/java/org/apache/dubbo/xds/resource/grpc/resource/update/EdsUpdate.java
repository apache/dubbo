package org.apache.dubbo.xds.resource.grpc.resource.update;

import com.google.common.base.MoreObjects;

import org.apache.dubbo.xds.resource.grpc.resource.endpoint.DropOverload;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.Locality;
import org.apache.dubbo.xds.resource.grpc.resource.endpoint.LocalityLbEndpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class EdsUpdate implements ResourceUpdate {
    final String clusterName;
    final Map<Locality, LocalityLbEndpoints> localityLbEndpointsMap;
    final List<DropOverload> dropPolicies;

    public EdsUpdate(String clusterName, Map<Locality, LocalityLbEndpoints> localityLbEndpoints,
              List<DropOverload> dropPolicies) {
        this.clusterName = checkNotNull(clusterName, "clusterName");
        this.localityLbEndpointsMap = Collections.unmodifiableMap(
                new LinkedHashMap<>(checkNotNull(localityLbEndpoints, "localityLbEndpoints")));
        this.dropPolicies = Collections.unmodifiableList(
                new ArrayList<>(checkNotNull(dropPolicies, "dropPolicies")));
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
        return
                MoreObjects
                        .toStringHelper(this)
                        .add("clusterName", clusterName)
                        .add("localityLbEndpointsMap", localityLbEndpointsMap)
                        .add("dropPolicies", dropPolicies)
                        .toString();
    }
}
