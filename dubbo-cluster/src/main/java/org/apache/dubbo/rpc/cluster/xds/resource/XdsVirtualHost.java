package org.apache.dubbo.rpc.cluster.xds.resource;

import java.util.List;

public class XdsVirtualHost {

    private String name;

    private List<String> domains;

    private List<XdsRoute> routes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public List<XdsRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<XdsRoute> routes) {
        this.routes = routes;
    }
}
