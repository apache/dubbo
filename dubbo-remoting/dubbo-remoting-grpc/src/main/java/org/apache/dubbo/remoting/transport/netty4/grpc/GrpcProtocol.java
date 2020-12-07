package org.apache.dubbo.remoting.transport.netty4.grpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AsciiString;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.transport.netty4.Status;
import org.apache.dubbo.remoting.transport.netty4.WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.h2.Http2WireProtocol;
import org.apache.dubbo.remoting.transport.netty4.invocation.DataHeader;
import org.apache.dubbo.remoting.transport.netty4.invocation.StreamInboundListener;
import org.apache.dubbo.remoting.transport.netty4.marshaller.Marshaller;
import org.apache.dubbo.remoting.transport.netty4.stream.StreamWriter;
import org.apache.dubbo.remoting.transport.netty4.stub.MethodContainer;
import org.apache.dubbo.remoting.transport.netty4.stub.ServerMethodModel;
import org.reactivestreams.Subscriber;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class GrpcProtocol extends Http2WireProtocol {

    private MethodContainer container = ExtensionLoader.getExtensionLoader(MethodContainer.class).getDefaultExtension();

    @Override
    public StreamInboundListener createServerListener(DataHeader dataHeader, ChannelHandlerContext ctx) {

        final int streamId = dataHeader.streamId;
        Http2Headers headers = (Http2Headers) dataHeader.header;
        if (headers.path() == null) {
            respondWithHttpError(HttpResponseStatus.NOT_FOUND.codeAsText(), Status.UNIMPLEMENTED, "Expected path but is missing", streamId, ctx);
            return null;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            respondWithHttpError(HttpResponseStatus.NOT_FOUND.codeAsText(), Status.UNIMPLEMENTED,
                String.format("Expected path to start with /: %s", path), streamId, ctx);
            return null;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            respondWithHttpError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.codeAsText(), Status.INTERNAL,
                "Content-Type is missing from the request", streamId, ctx);
            return null;
        }

        if (!GrpcElf.isGrpcContentType(contentType)) {
            respondWithHttpError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.codeAsText(), Status.INTERNAL,
                String.format("Content-Type '%s' is not supported", contentType), streamId, ctx);
            return null;
        }

        if (!HttpMethod.POST.asciiName().equals(headers.method())) {
            respondWithHttpError(HttpResponseStatus.METHOD_NOT_ALLOWED.codeAsText(),
                Status.INTERNAL, String.format("Method '%s' is not supported", headers.method()), streamId, ctx);
            return null;
        }

        String marshaller;
        if (AsciiString.contentEquals(contentType, GrpcElf.APPLICATION_GRPC) || AsciiString.contentEquals(contentType, GrpcElf.GRPC_PROTO)) {
            marshaller = "protobuf";
        } else if (AsciiString.contentEquals(contentType, GrpcElf.GRPC_JSON)) {
            marshaller = "protobuf-json";
        } else {
            respondWithHttpError(HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.codeAsText(), Status.INTERNAL,
                String.format("Content-Type '%s' is not supported", contentType), streamId, ctx);
            return null;
        }

        final ServerMethodModel method = container.lookup(path);

        if (method == null) {
            //if (log.isWarnEnabled()) {
            //    log.warn("Error={}  meta={}", "Path not found", headers);
            //}
            final Status status = Status.UNIMPLEMENTED.withDescription("Path not found:" + headers.path());
            ctx.channel().writeAndFlush(new DataHeader(createResponseTrailers(status), streamId, true));
            return null;
        }

        final Marshaller respParser = method.getRespMarshaller(marshaller);
        final Marshaller reqParser = method.getReqMarshaller(marshaller);

        GRpcServerSubscriber subscriber = new GRpcServerSubscriber(new StreamWriter(ctx.channel(), streamId), ctx.alloc(), respParser);
        GrpcServerListener inboundHandler = new GrpcServerListener(ctx.alloc(), reqParser);

        final Subscriber<Object> resp = method.streamInvoke(subscriber);
        inboundHandler.subscribe(resp);

        return inboundHandler;
    }

    public Http2Headers createResponseTrailers(Status status) {
        //if (status.isOk()) {
        //    return new DefaultHttp2Headers()
        //        .setInt(GrpcElf.GRPC_STATUS, Status.Code.OK.value());
        //}
        final Http2Headers meta = new DefaultHttp2Headers()
            .status(OK.codeAsText())
            .setInt(GrpcElf.GRPC_STATUS, Status.Code.INTERNAL.value());
        //if (status.getDescription() != null) {
        //    meta.set(GrpcElf.GRPC_MESSAGE, STATUS_MARSHALLER.toAsciiString(status.getDescription()));
        //} else {
        //    meta.set(GrpcElf.GRPC_MESSAGE, STATUS_MARSHALLER.toAsciiString(status.getCause().toString()));
        //}
        return meta;
    }

}
