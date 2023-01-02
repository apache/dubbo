//package org.apache.dubbo.registry.xds.util.protocol.impl;
//
//import io.envoyproxy.envoy.config.core.v3.Node;
//import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
//import io.grpc.stub.StreamObserver;
//import org.apache.dubbo.registry.xds.util.XdsChannel;
//import org.apache.dubbo.registry.xds.util.protocol.message.ListenerResult;
//import org.apache.dubbo.rpc.model.ApplicationModel;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Consumer;
//
//public class LdsProtocolMock extends LdsProtocol{
//
//    public LdsProtocolMock(XdsChannel xdsChannel, Node node, int pollingTimeout, ApplicationModel applicationModel) {
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
//    public StreamObserver<DiscoveryRequest> getRequestObserver() {
//        return requestObserver;
//    }
//
//    public void setRequestObserver(StreamObserver<DiscoveryRequest> requestObserver) {
//        this.requestObserver = requestObserver;
//    }
//
//    public ResponseObserverMock getResponseObserve() {
//        return new ResponseObserverMock();
//    }
//
//    protected DiscoveryRequest buildDiscoveryRequest(Set<String> resourceNames) {
//        return DiscoveryRequest.newBuilder()
//            .setNode(node)
//            .setTypeUrl(getTypeUrl())
//            .addAllResourceNames(resourceNames)
//            .build();
//    }
//
//    public Set<String> getObserveResourcesName() {
//        return observeResourcesName;
//    }
//
//    public void setObserveResourcesName(Set<String> observeResourcesName) {
//        this.observeResourcesName = observeResourcesName;
//    }
//
//    public Map<Set<String>, List<Consumer<ListenerResult>>> getConsumerObserveMap() {
//        return consumerObserveMap;
//    }
//
//    public void setConsumerObserveMap(Map<Set<String>, List<Consumer<ListenerResult>>> consumerObserveMap) {
//        this.consumerObserveMap = consumerObserveMap;
//    }
//    class ResponseObserverMock extends ResponseObserver {
//
//    }
//}
