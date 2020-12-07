package org.apache.dubbo.remoting.transport.netty4;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;
import org.apache.dubbo.remoting.transport.netty4.invocation.StreamInboundListener;

@SPI
public interface WireProtocol {
    List<WireProtocol> ALL = ExtensionLoader.getExtensionLoader(WireProtocol.class).getLoadedExtensionInstances();


    int id();
    //
    DetectionResult accept(ByteBuf in);
    //
    //void unaryInvoke(TransportClient client, ClientMethodModel model, Object arg, SPromise<Object> promise);
    //
    //Subscriber<Object> streamInvoke(TransportClient client, ClientMethodModel model, Subscriber<Object> respSub);
    //
    //Packer packer();
    //
    StreamInboundListener createServerListener(DataHeader headers, ChannelHandlerContext ctx);
    //
    //ChannelInitializer<SocketChannel> clientInitializer();
    //
    void initServerChannel(ChannelHandlerContext ctx);

}
