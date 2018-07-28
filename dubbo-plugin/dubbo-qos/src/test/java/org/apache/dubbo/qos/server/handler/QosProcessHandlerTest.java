package org.apache.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class QosProcessHandlerTest {
    @Test
    public void testDecodeHttp() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {'G'});
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(context.pipeline()).thenReturn(pipeline);
        QosProcessHandler handler = new QosProcessHandler("welcome", false);
        handler.decode(context, buf, Collections.emptyList());
        verify(pipeline).addLast(any(HttpServerCodec.class));
        verify(pipeline).addLast(any(HttpObjectAggregator.class));
        verify(pipeline).addLast(any(HttpProcessHandler.class));
        verify(pipeline).remove(handler);
    }

    @Test
    public void testDecodeTelnet() throws Exception {
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[] {'A'});
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        ChannelPipeline pipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(context.pipeline()).thenReturn(pipeline);
        QosProcessHandler handler = new QosProcessHandler("welcome", false);
        handler.decode(context, buf, Collections.emptyList());
        verify(pipeline).addLast(any(LineBasedFrameDecoder.class));
        verify(pipeline).addLast(any(StringDecoder.class));
        verify(pipeline).addLast(any(StringEncoder.class));
        verify(pipeline).addLast(any(TelnetProcessHandler.class));
        verify(pipeline).remove(handler);
    }


}
