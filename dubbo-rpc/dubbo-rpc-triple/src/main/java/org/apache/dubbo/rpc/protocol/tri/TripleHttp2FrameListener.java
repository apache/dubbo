package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2FrameWriter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleHttp2FrameListener extends Http2FrameAdapter {
    private static final long GRACEFUL_SHUTDOWN_PING = 0x97ACEF001L;
    private static final InvokerResolver serviceContainer = ExtensionLoader.getExtensionLoader(InvokerResolver.class).getDefaultExtension();
    private final Http2Connection connection;
    private final Http2FrameWriter frameWriter;
    private final Http2ConnectionEncoder encoder;
    protected Http2Connection.PropertyKey streamKey = null;

    public TripleHttp2FrameListener(Http2Connection connection, Http2FrameWriter frameWriter, Http2ConnectionEncoder encoder) {
        this.connection = connection;
        this.frameWriter = frameWriter;
        this.encoder = encoder;
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
        System.out.println("on ping ack read data:" + data);
        if (data == GRACEFUL_SHUTDOWN_PING) {
            System.out.println("on ping ack read data shutdown");
        }
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
            throws Http2Exception {
        Http2Stream stream = connection.stream(streamId);
        Http2Request request = stream == null ? null : (Http2Request) stream.getProperty(streamKey);

        if (request == null || request.getStreamId() != streamId) {
            System.out.println("received remote data from streamId:" + streamId + ", but not found payload.");
            return data.readableBytes() + padding;
        }

        request.appendData(data);

        int processed = data.readableBytes() + padding;
        final ByteBuf trunk = request.getAvailableTrunk();
        if(trunk!=null){
            Invocation invocation = buildInvocation(request.getHeaders(), trunk);
            // TODO add version/group support
            //TODO add method not found / service not found err
            Result result = serviceContainer.resolve("io.grpc.examples.helloworld.IGreeter").invoke(invocation);
            CompletionStage<Object> future = result.thenApply(Function.identity());

            future.whenComplete((appResult, t) -> {
                try {
                    if (t != null) {
                        // TODO add exception response
                        return;
                    }
                    AppResponse response = (AppResponse) appResult;
                    if (!response.hasException()) {
                        Http2Headers http2Headers = new DefaultHttp2Headers()
                                .status(OK.codeAsText())
                                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
                        encoder.writeHeaders(ctx, streamId, http2Headers, 0, false, ctx.newPromise());
                        ByteBuf byteBuf = Marshaller.marshaller.marshaller(ctx.alloc(), response.getValue());
                        encoder.writeData(ctx, streamId, byteBuf, 0, false, ctx.newPromise());
                        final Http2Headers trailers = new DefaultHttp2Headers()
                                .setInt(TripleConstant.STATUS_KEY, GrpcStatus.OK.code);
                        encoder.writeHeaders(ctx, streamId, trailers, 0, true, ctx.newPromise());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return processed;
    }

    private Invocation buildInvocation(Http2Headers http2Headers, ByteBuf data) {

        RpcInvocation inv = new RpcInvocation();
        final String path = http2Headers.path().toString();
        String[] parts = path.split("/");
        // todo
        String serviceName = "io.grpc.examples.helloworld.IGreeter";
        String methodName = "sayHello";
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(serviceName, methodName);
        Object obj = Marshaller.marshaller.unmarshaller(methodDescriptor.getParameterClasses()[0], data);
        inv.setMethodName(methodName);
        inv.setServiceName(serviceName);
        inv.setTargetServiceUniqueName(serviceName);
        inv.setParameterTypes(methodDescriptor.getParameterClasses());
        inv.setArguments(new Object[]{obj});
        inv.setReturnTypes(methodDescriptor.getReturnTypes());

        return inv;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                              short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
        onHeadersRead(ctx, streamId, headers, padding, endStream);
    }

    private void responsePlainTextError(ChannelHandlerContext ctx, int streamId, int code, int statusCode, String msg) {
        Http2Headers headers = new DefaultHttp2Headers(true)
                .status("" + code)
                .setInt(TripleConstant.STATUS_KEY, statusCode)
                .set(TripleConstant.MESSAGE_KEY, msg)
                .set(TripleConstant.CONTENT_TYPE_KEY, "text/plain; encoding=utf-8");
        encoder.writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
        ByteBuf msgBuf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
        encoder.writeData(ctx, streamId, msgBuf, 0, true, ctx.newPromise());
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                              boolean endStream) throws Http2Exception {
        if (!HttpMethod.POST.asciiName().contentEquals(headers.method())) {
            responsePlainTextError(ctx, streamId, HttpResponseStatus.METHOD_NOT_ALLOWED.code(), GrpcStatus.INTERNAL.code,
                    String.format("Method '%s' is not supported", headers.method()));
            return;
        }

        if (headers.path() == null) {
            responsePlainTextError(ctx, streamId, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.UNIMPLEMENTED.code, "Expected path but is missing");
            return;
        }

        final String path = headers.path().toString();
        if (path.charAt(0) != '/') {
            responsePlainTextError(ctx, streamId, HttpResponseStatus.NOT_FOUND.code(), GrpcStatus.UNIMPLEMENTED.code,
                    String.format("Expected path to start with /: %s", path));
            return;
        }

        final CharSequence contentType = HttpUtil.getMimeType(headers.get(HttpHeaderNames.CONTENT_TYPE));
        if (contentType == null) {
            responsePlainTextError(ctx, streamId, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.INTERNAL.code,
                    "Content-Type is missing from the request");
            return;
        }

        final String contentString = contentType.toString();
        if (!TripleUtil.supportContentType(contentString)) {
            responsePlainTextError(ctx, streamId, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE.code(), GrpcStatus.INTERNAL.code,
                    String.format("Content-Type '%s' is not supported", contentString));
            return;
        }


        Http2Stream http2Stream = connection.stream(streamId);
        if (streamKey == null) {
            streamKey = connection.newKey();
        }
        Http2Request request = new Http2Request(streamId, path, http2Stream, headers, streamKey, ctx.alloc());
        http2Stream.setProperty(streamKey, request);
    }

}
