package org.apache.dubbo.registry.xds.util.protocol.impl;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.Set;

public class LdsProtocolMock extends LdsProtocol{

    public LdsProtocolMock(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
        super(xdsChannel, node, pollingTimeout, applicationModel);
    }

    public Map<String, Object> getResourcesMap() {
        return resourcesMap;
    }

    public void setResourcesMap(Map<String, Object> resourcesMap) {
        this.resourcesMap = resourcesMap;
    }

    public ResponseObserver getResponseObserve() {
        return new ResponseObserver();
    }

    protected DiscoveryRequest buildDiscoveryRequest(Set<String> resourceNames) {
        return DiscoveryRequest.newBuilder()
            .setNode(node)
            .setTypeUrl(getTypeUrl())
            .addAllResourceNames(resourceNames)
            .build();
    }
}
