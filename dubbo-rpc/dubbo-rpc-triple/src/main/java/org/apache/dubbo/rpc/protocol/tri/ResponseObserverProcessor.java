package org.apache.dubbo.rpc.protocol.tri;

import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.stream.StreamObserver;

public class ResponseObserverProcessor implements StreamObserver<Object> {

    private ChannelHandlerContext ctx;
    private volatile StreamObserver<Object> subscriber;
    private ServerStream stream;
    public ResponseObserverProcessor(ChannelHandlerContext ctx, ServerStream stream, StreamObserver<Object> subscriber) {
        this.stream = stream;
        this.ctx = ctx;
        this.subscriber = subscriber;
    }

    public ServerStream getStream() {
        return stream;
    }

    @Override
    public void onNext(Object o) {
        subscriber.onNext(o);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }


}
