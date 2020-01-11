package org.apache.dubbo.remoting.pilot.grpc;

import io.envoyproxy.envoy.api.v2.DiscoveryResponse;
import io.grpc.Channel;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.pilot.AbstractPilotClient;
import org.apache.dubbo.remoting.pilot.Constants;
import org.apache.dubbo.remoting.pilot.StateListener;
import org.apache.dubbo.remoting.pilot.option.DiscoveryUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * grpc support for pilot client
 *
 * @author hzj
 * @date 2019/03/20
 */
public class GrpcClient extends AbstractPilotClient {

    private final GrpcConnection connectionManager;
    private final GrpcDiscoveryManager discoveryManager;
    private final RuntimeException failed;

    public GrpcClient(URL url) {
        super(url);
        connectionManager = new GrpcConnection(url);
        connectionManager.setConnectionStateListener((channel, state) -> {
            if (state == StateListener.CONNECTED) {
                this.stateChanged(StateListener.CONNECTED);
            } else if (state == StateListener.DISCONNECTED) {
                this.stateChanged(StateListener.DISCONNECTED);
            }
        });
        connectionManager.start();
        this.failed = new IllegalStateException("Pilot is not connected, url:" + this.url);

        requiredNotNull(connectionManager.getChannel(), failed);
        //build watch stream before subscribe
        discoveryManager = new GrpcDiscoveryManager(this.url, connectionManager.getChannel(),
                new GrpcDiscoveryManager.ResponseListener() {
                    @Override
                    public void receiveResponse(Channel channel, DiscoveryResponse response) {
                        Map<String, List<URL>> responses = new ConcurrentHashMap<>();
                        try {
                            responses = DiscoveryUtil.discoveryResponseToUrls(response);
                        } catch (Exception e) {
                            logger.error("Failed to resolve response: " + response + " ,caused: " + e.getMessage(), e);
                            return;
                        }
                        responses.forEach((path, urls) -> responseReceived(path, urls));
                    }

                    @Override
                    public void receiveError(Channel channel, Throwable throwable) {
                        try {
                            Thread.sleep(Constants.DEFAULT_REGISTRY_RECONNECT_PERIOD * 5);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                        if (!connectionManager.isConnected()) {
                            logger.info("Disconnected from pilot server after receive Error: " + throwable
                                    + " wait pilot connected to recover grpc stream");
                            return;
                        }
                        logger.info("Recover grpc stream after receive Error: " + throwable);
                        stateChanged(StateListener.CONNECTED);
                    }
                });
        discoveryManager.buildDiscoveryStream();
        CompletableFuture.runAsync(DiscoveryUtil::init);
    }

    @Override
    public void doDiscoveryService(URL url) {
        requiredNotNull(connectionManager.getChannel(), failed);
        //stream was closed
        if (!discoveryManager.isStarted()) {
            discoveryManager.buildDiscoveryStream();
        }
        discoveryManager.sendDiscoveryRequest(DiscoveryUtil.urlToDiscoveryRequest(url));
    }

    @Override
    public void doClose() {
        if (discoveryManager != null) {
            discoveryManager.closeDiscoveryStream();
        }
        connectionManager.close();
    }

    @Override
    public void doCloseOldStream() {
        if (discoveryManager != null) {
            discoveryManager.closeOldDiscoveryStream();
        }
    }

    @Override
    public boolean isConnected() {
        return connectionManager.isConnected();
    }


    private static void requiredNotNull(Object obj, RuntimeException exception) {
        if (obj == null) {
            throw exception;
        }
    }
}
