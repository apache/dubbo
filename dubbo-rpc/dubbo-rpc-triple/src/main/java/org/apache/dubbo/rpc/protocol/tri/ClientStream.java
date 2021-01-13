package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.remoting.netty4.Connection;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ClientStream extends AbstractStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStream.class);
    private static final GrpcStatus MISSING_RESP = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("Missing Response");
    private final Request request;


    public ClientStream(ChannelHandlerContext ctx, Request request) {
        super(ctx);
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
            final Object req = getSerialization2().deserialize(data, methodDescriptor.getReturnClass());
            data.close();
            Response response = new Response(request.getId(), request.getVersion());
            response.setResult(new AppResponse(req));
            DefaultFuture2.received(Connection.getConnectionFromChannel(getCtx().channel()), response);
        } catch (IOException e) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withCause(e)
                    .withDescription("Failed to deserialize response");
            onError(status);
        }
    }

}
