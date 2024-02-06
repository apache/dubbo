package org.apache.dubbo.rpc.cluster.xds.resource;

public class XdsRoute {
    private String name;

    private XdsRouteMatch routeMatch;

    private XdsRouteAction routeAction;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public XdsRouteMatch getRouteMatch() {
        return routeMatch;
    }

    public void setRouteMatch(XdsRouteMatch routeMatch) {
        this.routeMatch = routeMatch;
    }

    public XdsRouteAction getRouteAction() {
        return routeAction;
    }

    public void setRouteAction(XdsRouteAction routeAction) {
        this.routeAction = routeAction;
    }
}
