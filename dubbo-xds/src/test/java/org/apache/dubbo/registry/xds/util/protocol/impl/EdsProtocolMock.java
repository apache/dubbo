package org.apache.dubbo.registry.xds.util.protocol.impl;

import io.envoyproxy.envoy.config.core.v3.Node;
import org.apache.dubbo.registry.xds.util.XdsChannel;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

public class EdsProtocolMock extends EdsProtocol{

    public EdsProtocolMock(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
        super(xdsChannel, node, pollingTimeout, applicationModel);
    }

    public Map<String, Object> getResourcesMap() {
        return resourcesMap;
    }

    public void setResourcesMap(Map<String, Object> resourcesMap) {
        this.resourcesMap = resourcesMap;
    }
}
