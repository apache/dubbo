package org.apache.dubbo.rpc.cluster.xds.resource;

public class XdsClusterWeight {

    private final String name;

    private final int weight;

    public XdsClusterWeight(String name, int weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }
}
