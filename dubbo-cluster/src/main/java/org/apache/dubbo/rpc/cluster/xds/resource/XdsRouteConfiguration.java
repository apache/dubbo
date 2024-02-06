package org.apache.dubbo.rpc.cluster.xds.resource;

import java.util.Map;

public class XdsRouteConfiguration {
    private String name;

    private Map<String, XdsVirtualHost> virtualHosts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, XdsVirtualHost> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(Map<String, XdsVirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }
}
