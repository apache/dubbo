package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleUtil {

    public static final AttributeKey<ServerStream> SERVER_STREAM_KEY = AttributeKey.newInstance("tri_server_stream");
    public static final AttributeKey<ClientStream> CLIENT_STREAM_KEY = AttributeKey.newInstance("tri_client_stream");
    private static final SingleProtobufSerialization pbSerialization = new SingleProtobufSerialization();


    public static ServerStream getServerStream(ChannelHandlerContext ctx) {
        return ctx.channel().attr(TripleUtil.SERVER_STREAM_KEY).get();
    }

    public static void setClientStream(Channel channel, ClientStream clientStream) {
        channel.attr(TripleUtil.CLIENT_STREAM_KEY).set(clientStream);
    }

    public static void setClientStream(ChannelHandlerContext ctx, ClientStream clientStream) {
        setClientStream(ctx.channel(), clientStream);
    }

    public static ClientStream getClientStream(ChannelHandlerContext ctx) {
        return ctx.channel().attr(TripleUtil.CLIENT_STREAM_KEY).get();
    }

    /**
     * must starts from application/grpc
     */
    public static boolean supportContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith(TripleConstant.APPLICATION_GRPC);
    }

    public static void responseErr(ChannelHandlerContext ctx, GrpcStatus status) {
        Http2Headers trailers = new DefaultHttp2Headers()
                .status(OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
                .setInt(TripleConstant.STATUS_KEY, status.code.code)
                .set(TripleConstant.MESSAGE_KEY, getErrorMsg(status));
        ctx.write(new DefaultHttp2HeadersFrame(trailers, true));
    }

    public static String getErrorMsg(GrpcStatus status) {
        if (status.description != null) {
            return percentEncode(status.description);
        } else {
            return percentEncode(status.cause.getMessage());
        }
    }

    public static String percentDecode(CharSequence corpus) {
        if (corpus == null) {
            return "";
        }
        QueryStringDecoder decoder = new QueryStringDecoder("?=" + corpus);
        for (Map.Entry<String, List<String>> e : decoder.parameters().entrySet()) {
            return e.getKey();
        }
        return "";
    }

    public static String percentEncode(String corpus) {
        if (corpus == null) {
            return "";
        }
        QueryStringEncoder encoder = new QueryStringEncoder("");
        encoder.addParam("", corpus);
        // ?=
        return encoder.toString().substring(2);
    }

    public static void responsePlainTextError(ChannelHandlerContext ctx, int code, GrpcStatus status) {
        Http2Headers headers = new DefaultHttp2Headers(true)
                .status("" + code)
                .setInt(TripleConstant.STATUS_KEY, status.code.code)
                .set(TripleConstant.MESSAGE_KEY, status.description)
                .set(TripleConstant.CONTENT_TYPE_KEY, "text/plain; encoding=utf-8");
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), status.description);
        ctx.write(new DefaultHttp2DataFrame(buf, true));
    }

    public static boolean needWrapper(Class<?>[] parameterTypes) {
        if (parameterTypes.length != 1) {
            return true;
        }
        return !Message.class.isAssignableFrom(parameterTypes[0]);
    }

    public static Object unwrapResp(TripleWrapper.TripleResponseWrapper wrap, MethodDescriptor desc) throws IOException, ClassNotFoundException {
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(wrap.getSerializeType());
        final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getData().toByteArray());
        final ObjectInput in = serialization.deserialize(null, bais);
        return in.readObject(desc.getReturnClass());
    }

    public static Object[] unwrapReq(TripleWrapper.TripleRequestWrapper wrap, MethodDescriptor desc) throws IOException, ClassNotFoundException {
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(wrap.getSerializeType());
        Object[] arguments = new Object[wrap.getArgsCount()];
        for (int i = 0; i < arguments.length; i++) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getArgs(i).toByteArray());
            final ObjectInput in = serialization.deserialize(null, bais);
            arguments[i] = in.readObject(desc.getParameterClasses()[i]);
        }
        return arguments;
    }

    public static TripleWrapper.TripleResponseWrapper wrapResp(String serializeType, Object resp, MethodDescriptor desc) throws IOException {
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serializeType);
        final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
                .setType(desc.getReturnClass().getName())
                .setSerializeType(serializeType);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutput serialize = serialization.serialize(null, bos);
        serialize.writeObject(resp);
        serialize.flushBuffer();
        builder.setData(ByteString.copyFrom(bos.toByteArray()));
        return builder.build();
    }

    public static <T> T unpack(InputStream is, Class<T> clz) throws IOException {
        final T req = (T) pbSerialization.deserialize(is, clz);
        is.close();
        return req;
    }

    public static ByteBuf pack(ChannelHandlerContext ctx, Object obj) throws IOException {
        final ByteBuf buf = ctx.alloc().buffer();
        buf.writeByte(0);
        buf.writeInt(0);
        final ByteBufOutputStream bos = new ByteBufOutputStream(buf);
        final int size = pbSerialization.serialize(obj, bos);
        buf.setInt(1, size);
        return buf;
    }

    public static TripleWrapper.TripleRequestWrapper wrapReq(RpcInvocation invocation) throws IOException {
        String serializationName = (String) invocation.getObjectAttachment(Constants.SERIALIZATION_KEY);
        final Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(serializationName);
        final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType(serializationName);
        for (int i = 0; i < invocation.getArguments().length; i++) {
            builder.addArgTypes(invocation.getParameterTypes()[i].getName());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutput serialize = serialization.serialize(null, bos);
            serialize.writeObject(invocation.getArguments()[i]);
            serialize.flushBuffer();
            builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
        }
        return builder.build();
    }


}
