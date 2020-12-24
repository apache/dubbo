package org.apache.dubbo.rpc.protocol.dubbo.grpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AsciiString;
import org.apache.dubbo.remoting.Http2Packet;
import org.apache.dubbo.remoting.netty4.DubboHttp2ConnectionHandler;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

public class GrpcHttp2FrameListener extends Http2FrameAdapter {

    protected Http2Connection.PropertyKey streamKey = null;

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
        throws Http2Exception {
        System.out.println("onDataRead:" + streamId);
        final DubboHttp2ConnectionHandler connectionHandler = ctx.pipeline().get(DubboHttp2ConnectionHandler.class);
        Http2Connection connection = connectionHandler.encoder().connection();
        Http2Stream stream = connection.stream(streamId);
        Http2Request request = stream == null ? null : (Http2Request) stream.getProperty(streamKey);

        if (request == null || request.getStreamId() != streamId) {
            System.out.println("received remote data from streamId:" + streamId + ", but not found payload.");
            int processed = data.readableBytes() + padding;
            return processed;
        }

        request.cumulate(data);

        int processed = data.readableBytes() + padding;
        if (endOfStream) {
            Http2Packet packet = buildHttp2Packet(streamId, request.getHeaders(), request.getData());
            ctx.pipeline().fireChannelRead(packet);
        }
        return processed;
    }

    private Http2Packet buildHttp2Packet(int streamId, Http2Headers http2Headers, ByteBuf data) {

        RpcInvocation inv = new RpcInvocation();
        final String path = http2Headers.path().toString();
        String[] parts = path.split("/");
        String serviceName = parts[1];
        String methodName = parts[2];
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(serviceName, methodName);
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setTargetServiceUniqueName(serviceName);
        Object obj = Marshaller.marshaller.unmarshaller(methodDescriptor.getParameterClasses()[0], data);
        inv.setArguments(new Object[]{obj});
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        Http2Packet packet = new Http2Packet(streamId, inv, null);
        return packet;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
        short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
        onHeadersRead(ctx, streamId, headers, padding, endStream);
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
        boolean endStream) throws Http2Exception {
        System.out.println("onHeadersRead" + streamId);

        final DubboHttp2ConnectionHandler connectionHandler = ctx.pipeline().get(DubboHttp2ConnectionHandler.class);
        if (headers.path() == null) {
            System.out.println("Expected path but is missing");
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            System.out.println("Expected path but is missing1");
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            System.out.println("Expected path but is missing2");
            return;
        }

        if (!GrpcElf.isGrpcContentType(contentType)) {
            System.out.println("Expected path but is missing3");
            return;
        }

        if (!HttpMethod.POST.asciiName().equals(headers.method())) {
            System.out.println("Expected path but is missing4");
            return;
        }

        String marshaller;
        if (AsciiString.contentEquals(contentType, GrpcElf.APPLICATION_GRPC) || AsciiString.contentEquals(contentType, GrpcElf.GRPC_PROTO)) {
            marshaller = "protobuf";
        } else if (AsciiString.contentEquals(contentType, GrpcElf.GRPC_JSON)) {
            marshaller = "protobuf-json";
        } else {
            System.out.println("Expected path but is missing5");
            return;
        }

        Http2Connection connection = connectionHandler.encoder().connection();
        Http2Stream http2Stream = connection.stream(streamId);
        if (streamKey == null) {
            streamKey = connection.newKey();
        }
        Http2Request request = new Http2Request(streamId, http2Stream, headers, streamKey, marshaller,
            ctx.alloc());
        http2Stream.setProperty(streamKey, request);

        if (endStream) {

        }
    }

}
