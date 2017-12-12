package com.alibaba.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author weipeng2k
 * @author qinliujie
 */
public class QosProcessHandler extends ByteToMessageDecoder {
    
    private ScheduledFuture<?> welcomeFuture;
    
    private String welcome;

    public static String prompt = "dubbo>";
    
    public QosProcessHandler(String welcome){
        this.welcome = welcome;
    }
    
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        welcomeFuture = ctx.executor().schedule(new Runnable() {
            
            @Override
            public void run() {
                if (welcome != null) {
                    ctx.write(Unpooled.wrappedBuffer(welcome.getBytes()));
                    ctx.writeAndFlush(Unpooled.wrappedBuffer(prompt.getBytes()));
                }
            }
            
        }, 500, TimeUnit.MILLISECONDS);
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 1) {
            return;
        }
        
        // 读入一个byte来猜协议
        final int magic = in.getByte(in.readerIndex());
        
        ChannelPipeline p = ctx.pipeline();
        p.addLast(new LocalHostPermitHandler());
        if (isHttp(magic)) {
            // HTTP协议不做welcome输出
            if (welcomeFuture != null && welcomeFuture.isCancellable()) {
                welcomeFuture.cancel(false);
            }
            p.addLast(new HttpServerCodec());
            p.addLast(new HttpObjectAggregator(1048576));
            p.addLast(new HttpProcessHandler());
            p.remove(this);
        } else {
            p.addLast(new LineBasedFrameDecoder(2048));
            p.addLast(new StringDecoder(CharsetUtil.UTF_8));
            p.addLast(new StringEncoder(CharsetUtil.UTF_8));
            p.addLast(new IdleStateHandler(0,0,5 * 60));
            p.addLast(new TelnetProcessHandler());
            p.remove(this);
        }
    }
    //Http 请求头 GET/POST,就酱紫 QWQ
    private static boolean isHttp(int magic) {
        return magic == 'G' || magic == 'P';
    }
    
}
