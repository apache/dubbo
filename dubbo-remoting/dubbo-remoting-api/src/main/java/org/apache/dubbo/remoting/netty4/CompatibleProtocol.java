package org.apache.dubbo.remoting.netty4;

import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.URL;

public class CompatibleProtocol implements WireProtocol {
    private final ProtocolDetector detector = new CompatibleProtocolDetector();

    @Override
    public ProtocolDetector detector() {
        return detector;
    }

    @Override
    public void configServerPipeline(ChannelHandlerContext ctx) {

    }

    @Override
    public void configClientPipeline(ChannelHandlerContext ctx) {

    }
}
