package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.remoting.netty4.Connection;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;
import org.apache.dubbo.triple.TripleWrapper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2NoMoreStreamIdsException;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ClientStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStream.class);
    private static final GrpcStatus MISSING_RESP = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Missing Response");
    private final Request request;


    public ClientStream(ChannelHandlerContext ctx, Request request) {
        super(ctx,TripleUtil.needWrapper(((RpcInvocation) request.getData()).getParameterTypes()));
        this.request = request;
    }


    @Override
    public void onError(GrpcStatus status) {
        Response response = new Response(request.getId(), request.getVersion());
        if (status.description != null) {
            response.setErrorMessage(status.description);
        } else {
            response.setErrorMessage(status.cause.getMessage());
        }
        // TODO map grpc status to response status
        response.setStatus(Response.BAD_REQUEST);
        DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
    }

    @Override
    public void write(Object obj, ChannelPromise promise) throws IOException {
        Request req = (Request) obj;
        final RpcInvocation invocation = (RpcInvocation) req.getData();
        Http2Headers headers = new DefaultHttp2Headers()
                .method(HttpMethod.POST.asciiName())
                .path("/" + invocation.getServiceName() + "/" + invocation.getMethodName())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        DefaultHttp2HeadersFrame frame = new DefaultHttp2HeadersFrame(headers);
        final TripleHttp2ClientResponseHandler responseHandler = new TripleHttp2ClientResponseHandler();

        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(getCtx().channel());
        final Http2StreamChannel streamChannel = streamChannelBootstrap.open().syncUninterruptibly().getNow();
        TripleUtil.setClientStream(streamChannel, this);
        streamChannel.pipeline().addLast(responseHandler)
                .addLast(new GrpcDataDecoder(Integer.MAX_VALUE))
                .addLast(new TripleClientInboundHandler());
        streamChannel.write(frame).addListener(future -> {
            if (!future.isSuccess()) {
                if (future.cause() instanceof Http2NoMoreStreamIdsException) {
                    getCtx().close();
                }
                promise.setFailure(future.cause());
            }
        });
        final ByteBuf out;
        if (isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper wrap = TripleUtil.wrapReq(invocation);
            out = TripleUtil.pack(getCtx(), wrap);
        } else {
            out = TripleUtil.pack(getCtx(), invocation.getArguments()[0]);
        }
        streamChannel.write(new DefaultHttp2DataFrame(out, true));

    }

    public void halfClose() {
        final int httpCode = HttpResponseStatus.parseLine(getHeaders().status()).code();
        if (HttpResponseStatus.OK.code() != httpCode) {
            final Integer code = getHeaders().getInt(TripleConstant.STATUS_KEY);
            final GrpcStatus status = GrpcStatus.fromCode(code)
                    .withDescription(TripleUtil.percentDecode(getHeaders().get(TripleConstant.MESSAGE_KEY)));
            onError(status);
            return;
        }
        Http2Headers te = getTe();
        if (te == null) {
            te = getHeaders();
        }
        final Integer code = te.getInt(TripleConstant.STATUS_KEY);
        if (!GrpcStatus.Code.isOk(code)) {
            final GrpcStatus status = GrpcStatus.fromCode(code)
                    .withDescription(TripleUtil.percentDecode(getHeaders().get(TripleConstant.MESSAGE_KEY)));
            onError(status);
            return;
        }
        final InputStream data = getData();
        if (data == null) {
            responseErr(getCtx(), MISSING_RESP);
            return;
        }
        final Invocation invocation = (Invocation) (request.getData());
        ServiceRepository repo = ApplicationModel.getServiceRepository();
        MethodDescriptor methodDescriptor = repo.lookupMethod(invocation.getServiceName(), invocation.getMethodName());
        try {
            final Object resp;
            if (isNeedWrap()) {
                final TripleWrapper.TripleResponseWrapper message = TripleUtil.unpack(data, TripleWrapper.TripleResponseWrapper.class);
                resp = TripleUtil.unwrapResp(message, methodDescriptor);
            } else {
                resp = TripleUtil.unpack(data, methodDescriptor.getReturnClass());
            }
            Response response = new Response(request.getId(), request.getVersion());
            response.setResult(new AppResponse(resp));
            DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
        } catch (Exception e) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withCause(e)
                    .withDescription("Failed to deserialize response");
            onError(status);
        }
    }

}
