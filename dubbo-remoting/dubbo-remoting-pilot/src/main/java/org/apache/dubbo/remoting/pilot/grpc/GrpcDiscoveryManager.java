package org.apache.dubbo.remoting.pilot.grpc;

import io.envoyproxy.envoy.api.v2.DiscoveryRequest;
import io.envoyproxy.envoy.api.v2.DiscoveryResponse;
import io.envoyproxy.envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * @author hzj
 * @create 2019/6/26
 */
public class GrpcDiscoveryManager {
    private Logger logger = LoggerFactory.getLogger(GrpcDiscoveryManager.class);
    private volatile StreamObserver<DiscoveryRequest> discoveryStream;
    private volatile ResponseListener responseListener;
    private URL url;
    private final ManagedChannel channel;
    private volatile boolean started = false;

    public GrpcDiscoveryManager(URL url, ManagedChannel channel, ResponseListener responseListener) {
        this.url = url;
        this.channel = channel;
        this.responseListener = responseListener;
    }


    public synchronized void buildDiscoveryStream() {
        if (started) {
            return;
        }
        AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub
                = AggregatedDiscoveryServiceGrpc.newStub(channel);
        ClientResponseObserver<DiscoveryRequest, DiscoveryResponse> clientResponseObserver =
                new ClientResponseObserver<DiscoveryRequest, DiscoveryResponse>() {

                    @Override
                    public void onNext(DiscoveryResponse response) {
                        getResponseListener().receiveResponse(channel, response);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (t.getCause() instanceof CloseStreamException) {
                            logger.warn("Close old pilot discovery stream, url: " + url);
                            return;
                        }
                        logger.error("Receive onError response from pilot server, url: " + url + t.getMessage(), t);
                        CompletableFuture.runAsync(()
                                -> getResponseListener().receiveError(channel, t));
                    }

                    @Override
                    public void onCompleted() {
                        logger.error("Receive onCompleted response from pilot server, url: " + url);
                        CompletableFuture.runAsync(()
                                -> getResponseListener().receiveError(channel, new RuntimeException("Receive onCompleted")));
                    }

                    @Override
                    public void beforeStart(ClientCallStreamObserver<DiscoveryRequest> requestStream) {
                        logger.info("Init grpc discovery stream to pilot server, url:" + url);
                    }
                };
        discoveryStream = stub.streamAggregatedResources(clientResponseObserver);
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    public void sendDiscoveryRequest(DiscoveryRequest request) {
        this.discoveryStream.onNext(request);
    }

    public void closeOldDiscoveryStream() {
        if (discoveryStream != null) {
            this.discoveryStream.onError(closeStreamException);
            this.discoveryStream = null;
            started = false;
        }
    }

    public void closeDiscoveryStream() {
        if (discoveryStream != null) {
            this.discoveryStream.onCompleted();
            this.discoveryStream = null;
        }
    }

    public ResponseListener getResponseListener() {
        return responseListener;
    }

    public interface ResponseListener {
        /**
         * Called when there is a message from pilot
         *
         * @param channel  the channel
         * @param response the response
         */
        void receiveResponse(Channel channel, DiscoveryResponse response);

        /**
         * Called when there is an error response from pilot
         *
         * @param channel the channel
         * @param throwable the error
         */
        void receiveError(Channel channel, Throwable throwable);
    }

    public static class CloseStreamException extends RuntimeException {

        public CloseStreamException(String message) {
            super(message);
        }
    }

    private static CloseStreamException closeStreamException = new CloseStreamException("Client close grpc stream");
}
