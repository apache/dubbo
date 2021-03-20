package org.apache.dubbo.rpc.protocol.tri;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.dubbo.common.stream.StreamObserver;

public class StreamOutboundWriter implements StreamObserver<Object> {

    private StreamServerStream stream;
    private final AtomicBoolean canceled = new AtomicBoolean();

    public StreamOutboundWriter(StreamServerStream stream) {
        this.stream = stream;
    }

    @Override
    public void onNext(Object o) throws Exception {

        stream.write(o, null);
    }

    @Override
    public void onError(Throwable t) {
        doCancel();
    }

    @Override
    public void onComplete() {
        stream.onComplete();
    }

    public void doCancel() {
        if (canceled.compareAndSet(false, true)) {
            stream.onComplete();
        }
    }
}
