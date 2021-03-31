package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.stream.StreamObserver;

public class StreamOutboundWriter implements StreamObserver<Object> {

    private final AbstractStream stream;

    public StreamOutboundWriter(AbstractStream stream) {
        this.stream = stream;
    }

    @Override
    public void onNext(Object o) {
        stream.onNext(o);
    }

    @Override
    public void onError(Throwable t) {
        stream.onError(t);
    }

    @Override
    public void onCompleted() {
        stream.onCompleted();
    }
}
