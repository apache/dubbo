package org.apache.dubbo.registry.xds.util;

import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DeltaDiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.stub.StreamObserver;

public class AdsDeltaManager {
    private StreamObserver<DeltaDiscoveryRequest> deltaRequestObserver;


    public class DeltaResponseObserver implements StreamObserver<DeltaDiscoveryResponse> {

        @Override
        public void onNext(DeltaDiscoveryResponse value) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {

        }
    }

}
