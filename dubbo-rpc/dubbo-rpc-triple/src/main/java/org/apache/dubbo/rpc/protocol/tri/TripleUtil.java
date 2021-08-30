/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.util.AttributeKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class TripleUtil {

    public static final AttributeKey<AbstractServerStream> SERVER_STREAM_KEY = AttributeKey.newInstance(
            "tri_server_stream");
    public static final AttributeKey<AbstractClientStream> CLIENT_STREAM_KEY = AttributeKey.newInstance(
            "tri_client_stream");

    public static final String LANGUAGE = "java";

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    public static AbstractServerStream getServerStream(ChannelHandlerContext ctx) {
        return ctx.channel().attr(TripleUtil.SERVER_STREAM_KEY).get();
    }

    public static AbstractClientStream getClientStream(ChannelHandlerContext ctx) {
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
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toMessage());
        ctx.writeAndFlush(new DefaultHttp2HeadersFrame(trailers, true));
    }

    public static void responsePlainTextError(ChannelHandlerContext ctx, int code, GrpcStatus status) {
        Http2Headers headers = new DefaultHttp2Headers(true)
            .status("" + code)
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.description)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), "text/plain; encoding=utf-8");
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ByteBuf buf = ByteBufUtil.writeUtf8(ctx.alloc(), status.description);
        ctx.write(new DefaultHttp2DataFrame(buf, true));
    }

    public static Object unwrapResp(URL url, TripleWrapper.TripleResponseWrapper wrap,
                                    MultipleSerialization serialization) {
        String serializeType = convertHessianFromWrapper(wrap.getSerializeType());
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getData().toByteArray());
            final Object ret = serialization.deserialize(url, serializeType, wrap.getType(), bais);
            bais.close();
            return ret;
        } catch (Exception e) {
            throw new RuntimeException("Failed to unwrap resp", e);
        }
    }

    public static Map<Class<?>, Object> tranFromStatusDetails(List<Any> detailList) {
        Map<Class<?>, Object> map = new HashMap<>();
        try {
            for (Any any : detailList) {
                if (any.is(ErrorInfo.class)) {
                    ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                    map.putIfAbsent(ErrorInfo.class, errorInfo);
                } else if (any.is(DebugInfo.class)) {
                    DebugInfo debugInfo = any.unpack(DebugInfo.class);
                    map.putIfAbsent(DebugInfo.class, debugInfo);
                }
                // support others type but now only support this
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static boolean overEachHeaderListSize(String str) {
        return TripleConstant.DEFAULT_HEADER_LIST_SIZE <= str.length();
    }


    public static Object[] unwrapReq(URL url, TripleWrapper.TripleRequestWrapper wrap,
                                     MultipleSerialization multipleSerialization) {
        String serializeType = convertHessianFromWrapper(wrap.getSerializeType());
        try {
            Object[] arguments = new Object[wrap.getArgsCount()];
            for (int i = 0; i < arguments.length; i++) {
                final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getArgs(i).toByteArray());
                Object obj = multipleSerialization.deserialize(url,
                        serializeType, wrap.getArgTypes(i), bais);
                arguments[i] = obj;
            }
            return arguments;
        } catch (Exception e) {
            throw new RuntimeException("Failed to unwrap req: " + e.getMessage(), e);
        }
    }

    public static TripleWrapper.TripleResponseWrapper wrapResp(URL url, String serializeType, Object resp,
                                                               MethodDescriptor desc,
                                                               MultipleSerialization multipleSerialization) {
        try {
            final TripleWrapper.TripleResponseWrapper.Builder builder = TripleWrapper.TripleResponseWrapper.newBuilder()
                    .setType(desc.getReturnClass().getName())
                    .setSerializeType(convertHessianToWrapper(serializeType));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serializeType, desc.getReturnClass().getName(), resp, bos);
            builder.setData(ByteString.copyFrom(bos.toByteArray()));
            bos.close();
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

    public static TripleWrapper.TripleExceptionWrapper wrapException(URL url, Throwable throwable,
                                                                     String serializeType,
                                                                     MultipleSerialization serialization) {
        try {
            final TripleWrapper.TripleExceptionWrapper.Builder builder = TripleWrapper.TripleExceptionWrapper.newBuilder()
                    .setLanguage(LANGUAGE)
                    .setClassName(throwable.getClass().getName())
                    .setSerialization(serializeType);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serialization.serialize(url, serializeType, builder.getClassName(), throwable, bos);
            builder.setData(ByteString.copyFrom(bos.toByteArray()));
            bos.close();
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper exception", e);
        }
    }

    public static Throwable unWrapException(URL url, TripleWrapper.TripleExceptionWrapper wrap,
                                            String serializeType,
                                            MultipleSerialization serialization) {
        if (wrap == null) {
            return null;
        }
        if (!LANGUAGE.equals(wrap.getLanguage())) {
            return null;
        }
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(wrap.getData().toByteArray());
            Object obj = serialization.deserialize(url, serializeType, wrap.getClassName(), bais);
            bais.close();
            return (Throwable) obj;
        } catch (Exception e) {
            // if this null ,can get common exception
            return null;
        }
    }


    public static TripleWrapper.TripleRequestWrapper wrapReq(URL url, String serializeType, Object req,
                                                             String type,
                                                             MultipleSerialization multipleSerialization) {
        try {
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                    .addArgTypes(type)
                    .setSerializeType(convertHessianToWrapper(serializeType));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serializeType, type, req, bos);
            builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            bos.close();
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

    public static TripleWrapper.TripleRequestWrapper wrapReq(URL url, RpcInvocation invocation,
                                                             MultipleSerialization serialization) {
        try {
            String serializationName = (String) invocation.getObjectAttachment(Constants.SERIALIZATION_KEY);
            final TripleWrapper.TripleRequestWrapper.Builder builder = TripleWrapper.TripleRequestWrapper.newBuilder()
                    .setSerializeType(convertHessianToWrapper(serializationName));
            for (int i = 0; i < invocation.getArguments().length; i++) {
                final String clz = invocation.getParameterTypes()[i].getName();
                builder.addArgTypes(clz);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                serialization.serialize(url, serializationName, clz, invocation.getArguments()[i], bos);
                builder.addArgs(ByteString.copyFrom(bos.toByteArray()));
            }
            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack wrapper req", e);
        }
    }

    public static <T> T unpack(byte[] data, Class<T> clz) {
        return unpack(new ByteArrayInputStream(data), clz);
    }

    public static <T> T unpack(InputStream is, Class<T> clz) {
        try {
            final T req = SingleProtobufUtils.deserialize(is, clz);
            is.close();
            return req;
        } catch (IOException e) {
            throw new RuntimeException("Failed to unpack req", e);
        } finally {
            closeQuietly(is);
        }
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignore) {
                // ignored
            }
        }
    }

    public static byte[] pack(Object obj) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            SingleProtobufUtils.serialize(obj, baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to pack protobuf object", e);
        }
        return baos.toByteArray();
    }

    public static String encodeWrapper(URL url, Object obj, String serializeType, MultipleSerialization serialization)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serialization.serialize(url, serializeType, obj.getClass().getName(), obj, bos);
        final TripleWrapper.TripleRequestWrapper wrap = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType(convertHessianToWrapper(serializeType))
                .addArgTypes(obj.getClass().getName())
                .addArgs(ByteString.copyFrom(bos.toByteArray()))
                .build();
        return encodeBase64ASCII(wrap.toByteArray());
    }

    public static String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    public static Object decodeObjFromHeader(URL url, CharSequence value, MultipleSerialization serialization)
            throws InvalidProtocolBufferException {
        final byte[] decode = decodeASCIIByte(value);
        final TripleWrapper.TripleRequestWrapper wrapper = TripleWrapper.TripleRequestWrapper.parseFrom(decode);
        final Object[] objects = TripleUtil.unwrapReq(url, wrapper, serialization);
        return objects[0];
    }

    public static byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    public static String convertHessianToWrapper(String serializeType) {
        if (serializeType.equals("hessian2")) {
            return "hessian4";
        }
        return serializeType;
    }

    public static String convertHessianFromWrapper(String serializeType) {
        if (serializeType.equals("hessian4")) {
            return "hessian2";
        }
        return serializeType;
    }

}
