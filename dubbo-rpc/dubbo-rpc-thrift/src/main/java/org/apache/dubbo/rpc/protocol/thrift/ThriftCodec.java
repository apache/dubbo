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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBufferInputStream;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.thrift.io.RandomAccessByteArrayOutputStream;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;

/**
 * Thrift framed protocol codec.
 *
 * <pre>
 * |<-                                  message header                                  ->|<- message body ->|
 * +----------------+----------------------+------------------+---------------------------+------------------+
 * | magic (2 bytes)|message size (4 bytes)|head size(2 bytes)| version (1 byte) | header |   message body   |
 * +----------------+----------------------+------------------+---------------------------+------------------+
 * |<-                                               message size                                          ->|
 * </pre>
 *
 * <p>
 * <b>header fields in version 1</b>
 * <ol>
 * <li>string - service name</li>
 * <li>long   - dubbo request id</li>
 * </ol>
 * </p>
 */

/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public class ThriftCodec implements Codec2 {

    public static final int MESSAGE_LENGTH_INDEX = 2;
    public static final int MESSAGE_HEADER_LENGTH_INDEX = 6;
    public static final int MESSAGE_SHORTEST_LENGTH = 10;
    public static final String NAME = "thrift";
    public static final String PARAMETER_CLASS_NAME_GENERATOR = "class.name.generator";
    public static final byte VERSION = (byte) 1;
    public static final short MAGIC = (short) 0xdabc;
    static final ConcurrentMap<Long, RequestData> CACHED_REQUEST =
            new ConcurrentHashMap<>();
    private static final AtomicInteger THRIFT_SEQ_ID = new AtomicInteger(0);
    private static final ConcurrentMap<String, Class<?>> CACHED_CLASS =
            new ConcurrentHashMap<>();

    private static int nextSeqId() {
        return THRIFT_SEQ_ID.incrementAndGet();
    }

    // just for test
    static int getSeqId() {
        return THRIFT_SEQ_ID.get();
    }

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message)
            throws IOException {

        if (message instanceof Request) {
            encodeRequest(channel, buffer, (Request) message);
        } else if (message instanceof Response) {
            encodeResponse(channel, buffer, (Response) message);
        } else {
            throw new UnsupportedOperationException("Thrift codec only support encode "
                    + Request.class.getName() + " and " + Response.class.getName());
        }

    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {

        int available = buffer.readableBytes();

        if (available < MESSAGE_SHORTEST_LENGTH) {

            return DecodeResult.NEED_MORE_INPUT;

        } else {

            TIOStreamTransport transport = new TIOStreamTransport(new ChannelBufferInputStream(buffer));

            TBinaryProtocol protocol = new TBinaryProtocol(transport);

            short magic;
            int messageLength;

            try {
//                protocol.readI32(); // skip the first message length
                byte[] bytes = new byte[4];
                transport.read(bytes, 0, 4);
                magic = protocol.readI16();
                messageLength = protocol.readI32();

            } catch (TException e) {
                throw new IOException(e.getMessage(), e);
            }

            if (MAGIC != magic) {
                throw new IOException("Unknown magic code " + magic);
            }

            if (available < messageLength) {
                return DecodeResult.NEED_MORE_INPUT;
            }

            return decode(protocol);

        }

    }

    private Object decode(TProtocol protocol)
            throws IOException {

        // version
        String serviceName;
        String path;
        long id;

        TMessage message;

        try {
            protocol.readI16();
            protocol.readByte();
            serviceName = protocol.readString();
            path = protocol.readString();
            id = protocol.readI64();
            message = protocol.readMessageBegin();
        } catch (TException e) {
            throw new IOException(e.getMessage(), e);
        }

        if (message.type == TMessageType.CALL) {

            RpcInvocation result = new RpcInvocation();
            result.setAttachment(INTERFACE_KEY, serviceName);
            result.setAttachment(PATH_KEY, path);
            result.setMethodName(message.name);

            String argsClassName = ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                    .getExtension(ThriftClassNameGenerator.NAME).generateArgsClassName(serviceName, message.name);

            if (StringUtils.isEmpty(argsClassName)) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                        "The specified interface name incorrect.");
            }

            Class clazz = CACHED_CLASS.get(argsClassName);

            if (clazz == null) {
                try {

                    clazz = ClassUtils.forNameWithThreadContextClassLoader(argsClassName);

                    CACHED_CLASS.putIfAbsent(argsClassName, clazz);

                } catch (ClassNotFoundException e) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }
            }

            TBase args;

            try {
                args = (TBase) clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

            try {
                args.read(protocol);
                protocol.readMessageEnd();
            } catch (TException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

            List<Object> parameters = new ArrayList<>();
            List<Class<?>> parameterTypes = new ArrayList<>();
            int index = 1;

            while (true) {

                TFieldIdEnum fieldIdEnum = args.fieldForId(index++);

                if (fieldIdEnum == null) {
                    break;
                }

                String fieldName = fieldIdEnum.getFieldName();

                String getMethodName = ThriftUtils.generateGetMethodName(fieldName);

                Method getMethod;

                try {
                    getMethod = clazz.getMethod(getMethodName);
                } catch (NoSuchMethodException e) {
                    throw new RpcException(
                            RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }

                parameterTypes.add(getMethod.getReturnType());
                try {
                    parameters.add(getMethod.invoke(args));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RpcException(
                            RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }

            }

            result.setArguments(parameters.toArray());
            result.setParameterTypes(parameterTypes.toArray(new Class[0]));

            Request request = new Request(id);
            request.setData(result);

            CACHED_REQUEST.putIfAbsent(id,
                    RequestData.create(message.seqid, serviceName, message.name));

            return request;

        } else if (message.type == TMessageType.EXCEPTION) {

            TApplicationException exception;

            try {
                exception = TApplicationException.readFrom(protocol);
                protocol.readMessageEnd();
            } catch (TException e) {
                throw new IOException(e.getMessage(), e);
            }

            AppResponse result = new AppResponse();

            result.setException(new RpcException(exception.getMessage()));

            Response response = new Response();

            response.setResult(result);

            response.setId(id);

            return response;

        } else if (message.type == TMessageType.REPLY) {

            String resultClassName = ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                    .getExtension(ThriftClassNameGenerator.NAME).generateResultClassName(serviceName, message.name);

            if (StringUtils.isEmpty(resultClassName)) {
                throw new IllegalArgumentException("Could not infer service result class name from service name "
                        + serviceName + ", the service name you specified may not generated by thrift idl compiler");
            }

            Class<?> clazz = CACHED_CLASS.get(resultClassName);

            if (clazz == null) {

                try {

                    clazz = ClassUtils.forNameWithThreadContextClassLoader(resultClassName);

                    CACHED_CLASS.putIfAbsent(resultClassName, clazz);

                } catch (ClassNotFoundException e) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }

            }

            TBase<?, ? extends TFieldIdEnum> result;
            try {
                result = (TBase<?, ?>) clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

            try {
                result.read(protocol);
                protocol.readMessageEnd();
            } catch (TException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

            Object realResult = null;

            int index = 0;

            while (true) {

                TFieldIdEnum fieldIdEnum = result.fieldForId(index++);

                if (fieldIdEnum == null) {
                    break;
                }

                Field field;

                try {
                    field = clazz.getDeclaredField(fieldIdEnum.getFieldName());
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }

                try {
                    realResult = field.get(result);
                } catch (IllegalAccessException e) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }

                if (realResult != null) {
                    break;
                }

            }

            Response response = new Response();

            response.setId(id);

            AppResponse appResponse = new AppResponse();

            if (realResult instanceof Throwable) {
                appResponse.setException((Throwable) realResult);
            } else {
                appResponse.setValue(realResult);
            }

            response.setResult(appResponse);

            return response;

        } else {
            // Impossible
            throw new IOException();
        }

    }

    private void encodeRequest(Channel channel, ChannelBuffer buffer, Request request)
            throws IOException {

        RpcInvocation inv = (RpcInvocation) request.getData();

        int seqId = nextSeqId();

        String serviceName = inv.getAttachment(INTERFACE_KEY);

        if (StringUtils.isEmpty(serviceName)) {
            throw new IllegalArgumentException("Could not find service name in attachment with key "
                    + INTERFACE_KEY);
        }

        TMessage message = new TMessage(
                inv.getMethodName(),
                TMessageType.CALL,
                seqId);

        String methodArgs = ExtensionLoader.getExtensionLoader(ClassNameGenerator.class)
                .getExtension(channel.getUrl().getParameter(ThriftConstants.CLASS_NAME_GENERATOR_KEY, ThriftClassNameGenerator.NAME))
                .generateArgsClassName(serviceName, inv.getMethodName());

        if (StringUtils.isEmpty(methodArgs)) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                    "Could not encode request, the specified interface may be incorrect.");
        }

        Class<?> clazz = CACHED_CLASS.get(methodArgs);

        if (clazz == null) {

            try {

                clazz = ClassUtils.forNameWithThreadContextClassLoader(methodArgs);

                CACHED_CLASS.putIfAbsent(methodArgs, clazz);

            } catch (ClassNotFoundException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

        }

        TBase args;

        try {
            args = (TBase) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
        }

        for (int i = 0; i < inv.getArguments().length; i++) {

            Object obj = inv.getArguments()[i];

            if (obj == null) {
                continue;
            }

            TFieldIdEnum field = args.fieldForId(i + 1);

            String setMethodName = ThriftUtils.generateSetMethodName(field.getFieldName());

            Method method;

            try {
                method = clazz.getMethod(setMethodName, inv.getParameterTypes()[i]);
            } catch (NoSuchMethodException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

            try {
                method.invoke(args, obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

        }

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(1024);

        TIOStreamTransport transport = new TIOStreamTransport(bos);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        int headerLength, messageLength;

        byte[] bytes = new byte[4];
        try {
            // magic
            protocol.writeI16(MAGIC);
            // message length placeholder
            protocol.writeI32(Integer.MAX_VALUE);
            // message header length placeholder
            protocol.writeI16(Short.MAX_VALUE);
            // version
            protocol.writeByte(VERSION);
            // service name
            protocol.writeString(serviceName);
            // path
            protocol.writeString(inv.getAttachment(PATH_KEY));
            // dubbo request id
            protocol.writeI64(request.getId());
            protocol.getTransport().flush();
            // header size
            headerLength = bos.size();

            // message body
            protocol.writeMessageBegin(message);
            args.write(protocol);
            protocol.writeMessageEnd();
            protocol.getTransport().flush();
            int oldIndex = messageLength = bos.size();

            // fill in message length and header length
            try {
                TFramedTransport.encodeFrameSize(messageLength, bytes);
                bos.setWriteIndex(MESSAGE_LENGTH_INDEX);
                protocol.writeI32(messageLength);
                bos.setWriteIndex(MESSAGE_HEADER_LENGTH_INDEX);
                protocol.writeI16((short) (0xffff & headerLength));
            } finally {
                bos.setWriteIndex(oldIndex);
            }

        } catch (TException e) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
        }

        buffer.writeBytes(bytes);
        buffer.writeBytes(bos.toByteArray());

    }

    private void encodeResponse(Channel channel, ChannelBuffer buffer, Response response)
            throws IOException {

        AppResponse result = (AppResponse) response.getResult();

        RequestData rd = CACHED_REQUEST.get(response.getId());

        String resultClassName = ExtensionLoader.getExtensionLoader(ClassNameGenerator.class).getExtension(
                channel.getUrl().getParameter(ThriftConstants.CLASS_NAME_GENERATOR_KEY, ThriftClassNameGenerator.NAME))
                .generateResultClassName(rd.serviceName, rd.methodName);

        if (StringUtils.isEmpty(resultClassName)) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION,
                    "Could not encode response, the specified interface may be incorrect.");
        }

        Class clazz = CACHED_CLASS.get(resultClassName);

        if (clazz == null) {

            try {
                clazz = ClassUtils.forNameWithThreadContextClassLoader(resultClassName);
                CACHED_CLASS.putIfAbsent(resultClassName, clazz);
            } catch (ClassNotFoundException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

        }

        TBase resultObj;

        try {
            resultObj = (TBase) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
        }

        TApplicationException applicationException = null;
        TMessage message;

        if (result.hasException()) {
            Throwable throwable = result.getException();
            int index = 1;
            boolean found = false;
            while (true) {
                TFieldIdEnum fieldIdEnum = resultObj.fieldForId(index++);
                if (fieldIdEnum == null) {
                    break;
                }
                String fieldName = fieldIdEnum.getFieldName();
                String getMethodName = ThriftUtils.generateGetMethodName(fieldName);
                String setMethodName = ThriftUtils.generateSetMethodName(fieldName);
                Method getMethod;
                Method setMethod;
                try {
                    getMethod = clazz.getMethod(getMethodName);
                    if (getMethod.getReturnType().equals(throwable.getClass())) {
                        found = true;
                        setMethod = clazz.getMethod(setMethodName, throwable.getClass());
                        setMethod.invoke(resultObj, throwable);
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
                }
            }

            if (!found) {
                applicationException = new TApplicationException(throwable.getMessage());
            }

        } else {
            Object realResult = result.getValue();
            // result field id is 0
            String fieldName = resultObj.fieldForId(0).getFieldName();
            String setMethodName = ThriftUtils.generateSetMethodName(fieldName);
            String getMethodName = ThriftUtils.generateGetMethodName(fieldName);
            Method getMethod;
            Method setMethod;
            try {
                getMethod = clazz.getMethod(getMethodName);
                setMethod = clazz.getMethod(setMethodName, getMethod.getReturnType());
                setMethod.invoke(resultObj, realResult);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
            }

        }

        if (applicationException != null) {
            message = new TMessage(rd.methodName, TMessageType.EXCEPTION, rd.id);
        } else {
            message = new TMessage(rd.methodName, TMessageType.REPLY, rd.id);
        }

        RandomAccessByteArrayOutputStream bos = new RandomAccessByteArrayOutputStream(1024);

        TIOStreamTransport transport = new TIOStreamTransport(bos);

        TBinaryProtocol protocol = new TBinaryProtocol(transport);

        int messageLength;
        int headerLength;

        byte[] bytes = new byte[4];
        try {
            // magic
            protocol.writeI16(MAGIC);
            // message length
            protocol.writeI32(Integer.MAX_VALUE);
            // message header length
            protocol.writeI16(Short.MAX_VALUE);
            // version
            protocol.writeByte(VERSION);
            // service name
            protocol.writeString(rd.serviceName);
            // id
            protocol.writeI64(response.getId());
            protocol.getTransport().flush();
            headerLength = bos.size();

            // message
            protocol.writeMessageBegin(message);
            switch (message.type) {
                case TMessageType.EXCEPTION:
                    applicationException.write(protocol);
                    break;
                case TMessageType.REPLY:
                    resultObj.write(protocol);
                    break;
                default:
            }
            protocol.writeMessageEnd();
            protocol.getTransport().flush();
            int oldIndex = messageLength = bos.size();

            try {
                TFramedTransport.encodeFrameSize(messageLength, bytes);
                bos.setWriteIndex(MESSAGE_LENGTH_INDEX);
                protocol.writeI32(messageLength);
                bos.setWriteIndex(MESSAGE_HEADER_LENGTH_INDEX);
                protocol.writeI16((short) (0xffff & headerLength));
            } finally {
                bos.setWriteIndex(oldIndex);
            }

        } catch (TException e) {
            throw new RpcException(RpcException.SERIALIZATION_EXCEPTION, e.getMessage(), e);
        }

        buffer.writeBytes(bytes);
        buffer.writeBytes(bos.toByteArray());

    }

    static class RequestData {
        int id;
        String serviceName;
        String methodName;

        static RequestData create(int id, String sn, String mn) {
            RequestData result = new RequestData();
            result.id = id;
            result.serviceName = sn;
            result.methodName = mn;
            return result;
        }

    }

}
