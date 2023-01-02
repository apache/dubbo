//package org.apache.dubbo.registry.xds.util.protocol.impl;
//
//import io.envoyproxy.envoy.config.core.v3.Node;
//import org.apache.dubbo.registry.xds.util.XdsChannel;
//import org.apache.dubbo.registry.xds.util.protocol.message.EndpointResult;
//import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Consumer;
//
//public class EdsProtocolMock extends EdsProtocol{
//
//    public EdsProtocolMock(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
//        super(xdsChannel, node, pollingTimeout, applicationModel);
//    }
//
//    public Map<String, Object> getResourcesMap() {
//        return resourcesMap;
//    }
//
//    public void setResourcesMap(Map<String, Object> resourcesMap) {
//        this.resourcesMap = resourcesMap;
//    }
//
//    public void setConsumerObserveMap(Map<Set<String>, List<Consumer<EndpointResult>>> consumerObserveMap) {
//        this.consumerObserveMap = consumerObserveMap;
//    }
//
//    public ResponseObserverMock getResponseObserve() {
//        return new ResponseObserverMock();
//    }
//
//    public void setObserveResourcesName(Set<String> observeResourcesName) {
//        this.observeResourcesName = observeResourcesName;
//    }
//    class ResponseObserverMock extends ResponseObserver {
//
//    }
//}
