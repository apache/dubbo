package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.Serialization2;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceRepository;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;

import static org.apache.dubbo.rpc.protocol.tri.TripleUtil.responseErr;

public class ClientStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStream.class);
    private static final String TOO_MANY_REQ = "Too many requests";
    private static final String MISSING_REQ = "Missing request";
    private final ChannelHandlerContext ctx;
    private final Serialization2 serialization2;
    private final Request request;
    private Http2Headers headers;
    private Http2Headers te;
    private ByteBuf pendingData;


    public ClientStream(ChannelHandlerContext ctx, Request request) {
        this.ctx = ctx;
        this.request = request;
        this.serialization2 = ExtensionLoader.getExtensionLoader(Serialization2.class).getExtension("protobuf");
    }

    public void receiveData(ByteBuf buf) {
        if (pendingData != null) {
            if (buf.isReadable()) {
                responseErr(ctx, GrpcStatus.INTERNAL, TOO_MANY_REQ);
            }
            return;
        }

        this.pendingData = buf;
    }

    public void onHeaders(Http2Headers headers) {
        if (this.headers == null) {
            this.headers = headers;
        } else {
            this.te = headers;
        }
    }

    public void onError(TripleRpcException t){
        Response response = new Response(request.getId(), request.getVersion());
        response.setErrorMessage(t.getMessage());
        // TODO map grpc status to response status
        response.setStatus(Response.BAD_REQUEST);
        DefaultFuture2.received(ctx.channel(), response);
    }
    public void halfClose() {
        final int code = HttpResponseStatus.parseLine(headers.status()).code();
        if (HttpResponseStatus.OK.code() != code) {
            final Integer status = headers.getInt(TripleConstant.STATUS_KEY);
            final String errMsg = TripleUtil.percentDecode(headers.get(TripleConstant.MESSAGE_KEY));
            Response response = new Response(request.getId(), request.getVersion());
            response.setErrorMessage(errMsg);
            // TODO map grpc status to response status
            response.setStatus(Response.BAD_REQUEST);
            DefaultFuture2.received(ctx.channel(), response);
            return;
        } else {
            if(te==null){
                this.te=headers;
            }
            final Integer status = te.getInt(TripleConstant.STATUS_KEY);
            if (!GrpcStatus.isOk(status)) {
                final String errMsg = TripleUtil.percentDecode(headers.get(TripleConstant.MESSAGE_KEY));
                Response response = new Response(request.getId(), request.getVersion());
                response.setErrorMessage(errMsg);
                // TODO map grpc status to response status
                response.setStatus(Response.SERVER_ERROR);
            } else {
                if (pendingData == null) {
                    responseErr(ctx, GrpcStatus.INTERNAL, MISSING_REQ);
                    return;
                }
                final Invocation invocation = (Invocation) (request.getData());
                ServiceRepository repo = ApplicationModel.getServiceRepository();
                MethodDescriptor methodDescriptor = repo.lookupMethod(invocation.getServiceName(), invocation.getMethodName());
                try {
                    final Object req = serialization2.deserialize(new ByteBufInputStream(pendingData), methodDescriptor.getReturnClass());
                    pendingData.release();
                    Response response = new Response(request.getId(), request.getVersion());
                    response.setResult(new AppResponse(req));
                    DefaultFuture2.received(ctx.channel(), response);
                } catch (IOException e) {
                    // TODO handle it
                    e.printStackTrace();
                }
            }
        }
    }

}
