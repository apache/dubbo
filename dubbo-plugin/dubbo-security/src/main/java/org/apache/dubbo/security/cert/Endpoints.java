package org.apache.dubbo.security.cert;

public class Endpoints {
    private final Endpoint localEndpoint;
    private final Endpoint peerEndpoint;

    public Endpoints(Endpoint localEndpoint, Endpoint peerEndpoint) {
        this.localEndpoint = localEndpoint;
        this.peerEndpoint = peerEndpoint;
    }

    public Endpoint getLocalEndpoint() {
        return localEndpoint;
    }

    public Endpoint getPeerEndpoint() {
        return peerEndpoint;
    }
}
