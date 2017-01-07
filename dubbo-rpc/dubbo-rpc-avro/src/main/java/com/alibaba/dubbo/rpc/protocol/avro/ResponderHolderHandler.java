package com.alibaba.dubbo.rpc.protocol.avro;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Created by wuyu on 2016/6/15.
 */
public class ResponderHolderHandler extends SimpleChannelUpstreamHandler {
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ResponderHolder.setResponder(null);
        super.channelOpen(ctx, e);
    }
}
