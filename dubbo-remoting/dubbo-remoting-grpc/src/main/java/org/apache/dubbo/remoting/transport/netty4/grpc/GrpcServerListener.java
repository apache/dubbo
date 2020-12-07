package org.apache.dubbo.remoting.transport.netty4.grpc;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataBody;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;
import org.apache.dubbo.remoting.transport.netty4.invocation.ServerStreamListener;
import org.apache.dubbo.remoting.transport.netty4.marshaller.Marshaller;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;


public class GrpcServerListener extends Flux<Object> implements ServerStreamListener {
    private  ByteBufAllocator alloc;
    private Marshaller marshaller;
    private CompositeByteBuf readBuf;
    private int lackBytes;
    private volatile Subscriber<Object> subscriber;

    public GrpcServerListener(ByteBufAllocator alloc, Marshaller marshaller) {
        this.alloc = alloc;
        this.marshaller = marshaller;
    }

    @Override
    public int onBody(DataBody body) {
        return 0;
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void subscribe(CoreSubscriber<? super Object> coreSubscriber) {

    }

    @Override
    public void onHeader(DataHeader header) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
