package org.apache.dubbo.remoting.transport.netty4;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.dubbo.common.extension.ExtensionLoader;

@io.netty.channel.ChannelHandler.Sharable
public class ServerProtocolDetector extends SimpleChannelInboundHandler<ByteBuf> {
    private List<WireProtocol> protocols;
    private static final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = new HttpServerUpgradeHandler.UpgradeCodecFactory() {
        @Override
        public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            //            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            //                return new Http2ServerUpgradeCodec(new HelloWorldHttp2HandlerBuilder().build());
            //                return new Http2ServerUpgradeCodec(new Http2ServerHandlerBuilder().build());

            //            } else {
            return null;
            //            }
        }
    };
    public ServerProtocolDetector() {
        this.protocols = ExtensionLoader.getExtensionLoader(WireProtocol.class).getLoadedExtensionInstances();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        for (WireProtocol protocol : protocols) {
            final DetectionResult ret = protocol.accept(in);
            if (ret == DetectionResult.ACCEPTED) {
                final ChannelPipeline pipeline = ctx.pipeline();
                pipeline.remove(ctx.handler());
                protocol.initServerChannel(ctx);
                ReferenceCountUtil.retain(in);
                pipeline.fireChannelRead(in);
                return;
            } else if (ret == DetectionResult.NO_ENOUGH_DATA) {
                return;
            }
        }
        final ChannelPipeline p = ctx.pipeline();
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
        p.addLast(upgradeHandler);
        p.addLast(new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                if (msg.decoderResult().isFailure()) {
                    //log.warn("Failed to parse http message channel={}", ctx.channel(), msg.decoderResult().cause());
                    ctx.close();
                    return;
                }
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
                ChannelPipeline pipeline = ctx.pipeline();
                //                new HttpProtocol().configServer(ctx.name(), ctx.pipeline());
                pipeline.replace(this, null, new HttpObjectAggregator(16 * 1024));
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });
        p.remove(ctx.name());
    }
}
