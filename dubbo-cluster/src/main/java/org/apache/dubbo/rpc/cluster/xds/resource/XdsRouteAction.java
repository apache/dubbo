package org.apache.dubbo.rpc.cluster.xds.resource;

import java.util.List;

public class XdsRouteAction {
    private String cluster;

    private List<XdsClusterWeight> clusterWeights;

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public List<XdsClusterWeight> getClusterWeights() {
        return clusterWeights;
    }

    public void setClusterWeights(List<XdsClusterWeight> clusterWeights) {
        this.clusterWeights = clusterWeights;
    }
}
