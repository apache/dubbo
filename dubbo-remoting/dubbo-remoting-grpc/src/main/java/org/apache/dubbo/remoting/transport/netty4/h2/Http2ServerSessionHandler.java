package org.apache.dubbo.remoting.transport.netty4.h2;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Connection.PropertyKey;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.dubbo.remoting.transport.netty4.ConnectionSession;
import org.apache.dubbo.remoting.transport.netty4.SessionHandler;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataBody;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;

public class Http2ServerSessionHandler extends ChannelDuplexHandler implements SessionHandler {

    private Http2ServerConnectionHandler handler;
    private ChannelHandlerContext ctx;
    private Http2ConnectionEncoder encoder;
    private Http2Connection.PropertyKey inboundKey;
    private Http2Connection.PropertyKey outboundKey;
    private Http2Connection connection;

    public Http2ServerConnectionHandler getHandler() {
        return handler;
    }

    public Http2Connection getConnection() {
        return connection;
    }

    public PropertyKey getInboundKey() {
        return inboundKey;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HttpServerUpgradeHandler.UpgradeEvent || evt instanceof CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent) {
            this.ctx = ctx;
            this.handler = ctx.pipeline().get(Http2ServerConnectionHandler.class);
            encoder = handler.encoder();
            connection = handler.connection();
            inboundKey = connection.newKey();
            outboundKey = connection.newKey();
            ConnectionSession.add(this);
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // todo
        super.channelInactive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DataHeader) {
            DataHeader command = (DataHeader) msg;
            //if (msg instanceof ServerDataHeader) {
                //connection.stream(command.streamId).setProperty(outboundKey, ((ServerDataHeader) command).subscriber);
            //}
            encoder.writeHeaders(ctx, command.streamId, (Http2Headers) command.header, 0, command.endStream,
                promise);
        } else if (msg instanceof DataBody) {
            DataBody cmd = (DataBody) msg;
            encoder.writeData(ctx, cmd.getStreamId(), cmd.content(), 0, cmd.endStream, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void close() {
        if (ctx.channel().isOpen()) {
            try {
                handler.close(ctx, ctx.voidPromise());
            } catch (Exception e) {
                throw new RuntimeException("Closing error:", e);
            }
            //            try {
            //                connection.forEachActiveStream(stream -> {
            //                    handler.resetStream(ctx, stream.id(), Http2Error.CANCEL.code(), ctx.voidPromise());
            //                    stream.close();
            //                    return true;
            //                });
            //            } catch (Http2Exception e) {
            //                throw new RuntimeException(e);
            //            }
        }
    }
}
