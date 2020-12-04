package org.apache.dubbo.registry.xds.util.protocol;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;

import java.util.Set;

public interface XdsProtocol {

    DiscoveryRequest buildDiscoveryRequest(Node node);

    DiscoveryRequest buildDiscoveryRequest(Node node, DiscoveryResponse response);
}
