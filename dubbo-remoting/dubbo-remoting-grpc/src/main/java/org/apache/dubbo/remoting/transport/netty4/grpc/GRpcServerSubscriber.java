package org.apache.dubbo.remoting.transport.netty4.grpc;

import io.netty.buffer.ByteBufAllocator;
import org.apache.dubbo.remoting.transport.netty4.marshaller.Marshaller;
import org.apache.dubbo.remoting.transport.netty4.stream.StreamWriter;
import reactor.core.publisher.BaseSubscriber;

public class GRpcServerSubscriber extends BaseSubscriber<Object> {

    private StreamWriter writer;
    private ByteBufAllocator alloc;
    private Marshaller responseMarshaller;

    public GRpcServerSubscriber(StreamWriter writer, ByteBufAllocator alloc,
        Marshaller responseMarshaller) {
        this.writer = writer;
        this.alloc = alloc;
        this.responseMarshaller = responseMarshaller;
    }
}
