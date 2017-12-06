/**
 *
 */
package com.alibaba.qos.server.handler;


import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.qos.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

/**
 * @author weipeng2k 2015年9月6日 下午5:52:26
 */
public class LocalHostPermitHandler extends ChannelHandlerAdapter {

    // true表示能够远端连接
    private static boolean acceptForeignIp = Boolean.valueOf(ConfigUtils.getProperty(Constants.ACCEPT_FOREIGN_IP, "true"));

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (!acceptForeignIp) {
            if (!((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().isLoopbackAddress()) {
                ByteBuf cb = Unpooled.wrappedBuffer((Constants.BR_STR + "Foreign Ip Not Permitted."
                        + Constants.BR_STR).getBytes());
                ctx.writeAndFlush(cb).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
