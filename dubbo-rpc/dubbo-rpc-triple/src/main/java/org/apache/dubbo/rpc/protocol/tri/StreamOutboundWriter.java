package org.apache.dubbo.rpc.protocol.tri;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.dubbo.common.stream.StreamObserver;

public class StreamOutboundWriter implements StreamObserver<Object> {

    private final AbstractStream stream;
    private final AtomicBoolean canceled = new AtomicBoolean();

    public StreamOutboundWriter(AbstractStream stream) {
        this.stream = stream;
    }

    @Override
    public void onNext(Object o) {

        try {
            stream.write(o, null);
        } catch (Exception e) {
            // todo error
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable t) {
        doCancel();
    }

    @Override
    public void onComplete() {
        stream.halfClose();
    }

    public void doCancel() {
        if (canceled.compareAndSet(false, true)) {
            stream.halfClose();
        }
    }
}
