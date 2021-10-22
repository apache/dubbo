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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TripleUtil {
    // Some exceptions are not very useful and add too much noise to the log
    private static final Set<String> QUIET_EXCEPTIONS = new HashSet<>();
    private static final Set<Class<?>> QUIET_EXCEPTIONS_CLASS = new HashSet<>();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    static {
        QUIET_EXCEPTIONS.add("NativeIoException");
        QUIET_EXCEPTIONS_CLASS.add(IOException.class);
        QUIET_EXCEPTIONS_CLASS.add(SocketException.class);
    }

    public static boolean isQuiteException(Throwable t) {
        if (QUIET_EXCEPTIONS_CLASS.contains(t.getClass())) {
            return true;
        }
        return QUIET_EXCEPTIONS.contains(t.getClass().getSimpleName());
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

    public static String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    public static byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    public static String convertHessianToWrapper(String serializeType) {
        if (TripleConstant.HESSIAN2.equals(serializeType)) {
            return TripleConstant.HESSIAN4;
        }
        return serializeType;
    }

    public static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }

}
