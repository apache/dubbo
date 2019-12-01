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
package org.apache.dubbo.common.serialize.protobuf.support;

import org.apache.dubbo.common.serialize.protobuf.support.wrapper.MapValue;
import org.apache.dubbo.common.serialize.protobuf.support.wrapper.ThrowablePB.StackTraceElementProto;
import org.apache.dubbo.common.serialize.protobuf.support.wrapper.ThrowablePB.ThrowableProto;

import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.FloatValue;
import com.google.protobuf.GeneratedMessageV3.Builder;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProtobufUtils {

    static boolean isSupported(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (MessageLite.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    /* Protobuf json */

    static <T> T deserializeJson(String json, Class<T> requestClass) throws InvalidProtocolBufferException {
        Builder builder;
        try {
            builder = getMessageBuilder(requestClass);
        } catch (Exception e) {
            throw new IllegalArgumentException("Get google protobuf message builder from " + requestClass.getName() + "failed", e);
        }
        JsonFormat.parser().merge(json, builder);
        return (T) builder.build();
    }

    static String serializeJson(Object value) throws InvalidProtocolBufferException {
        Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        return printer.print((MessageOrBuilder) value);
    }

    private static Builder getMessageBuilder(Class<?> requestType) throws Exception {
        Method method = requestType.getMethod("newBuilder");
        return (Builder) method.invoke(null, null);
    }


    /* Protobuf */

    private static ConcurrentMap<Class<? extends MessageLite>, MessageMarshaller> marshallers =
            new ConcurrentHashMap<>();

    private static volatile ExtensionRegistryLite globalRegistry =
            ExtensionRegistryLite.getEmptyRegistry();

    static {
        // Built-in types need to be registered in advance
        marshaller(MapValue.Map.getDefaultInstance());
        marshaller(Empty.getDefaultInstance());
        marshaller(ThrowableProto.getDefaultInstance());
        marshaller(BoolValue.getDefaultInstance());
        marshaller(Int32Value.getDefaultInstance());
        marshaller(Int64Value.getDefaultInstance());
        marshaller(FloatValue.getDefaultInstance());
        marshaller(DoubleValue.getDefaultInstance());
        marshaller(BytesValue.getDefaultInstance());
        marshaller(StringValue.getDefaultInstance());
    }

    public static <T extends MessageLite> void marshaller(T defaultInstance) {
        marshallers.put(defaultInstance.getClass(), new MessageMarshaller<>(defaultInstance));
    }

    static void serialize(Object value, OutputStream os) throws IOException {
        MessageLite messageLite = (MessageLite) value;
        messageLite.writeDelimitedTo(os);
    }

    @SuppressWarnings("unchecked")
    static <T> T deserialize(InputStream is, Class<T> requestClass) throws InvalidProtocolBufferException {
        MessageMarshaller<?> marshaller = marshallers.get(requestClass);
        if (marshaller == null) {
            throw new IllegalStateException(String.format("Protobuf classes should be registered in advance before " +
                    "do serialization, class name: %s", requestClass.getName()));
        }
        return (T) marshaller.parse(is);
    }

    public static Empty convertNullToEmpty() {
        return Empty.newBuilder().build();
    }

    public static Object convertEmptyToNull(Empty empty) {
        return null;
    }

    public static ThrowableProto convertToThrowableProto(Throwable throwable) {
        final ThrowableProto.Builder builder = ThrowableProto.newBuilder();
        builder.setOriginalClassName(throwable.getClass().getCanonicalName());
        builder.setOriginalMessage(Strings.nullToEmpty(throwable.getMessage()));

        for (StackTraceElement e : throwable.getStackTrace()) {
            builder.addStackTrace(toStackTraceElement(e));
        }

        if (throwable.getCause() != null) {
            builder.setCause(convertToThrowableProto(throwable.getCause()));
        }
        return builder.build();
    }

    public static Throwable convertToException(ThrowableProto throwableProto) {
        return new ProtobufWrappedException(throwableProto);
    }

    private static StackTraceElementProto toStackTraceElement(StackTraceElement element) {
        final StackTraceElementProto.Builder builder =
                StackTraceElementProto.newBuilder()
                        .setClassName(element.getClassName())
                        .setMethodName(element.getMethodName())
                        .setLineNumber(element.getLineNumber());
        if (element.getFileName() != null) {
            builder.setFileName(element.getFileName());
        }
        return builder.build();
    }

    private static final class MessageMarshaller<T extends MessageLite> {
        private final Parser<T> parser;
        private final T defaultInstance;

        @SuppressWarnings("unchecked")
        MessageMarshaller(T defaultInstance) {
            this.defaultInstance = defaultInstance;
            parser = (Parser<T>) defaultInstance.getParserForType();
        }

        @SuppressWarnings("unchecked")
        public Class<T> getMessageClass() {
            // Precisely T since protobuf doesn't let messages extend other messages.
            return (Class<T>) defaultInstance.getClass();
        }

        public T getMessagePrototype() {
            return defaultInstance;
        }

        public T parse(InputStream stream) throws InvalidProtocolBufferException {
            return parser.parseDelimitedFrom(stream, globalRegistry);
//            CodedInputStream cis = CodedInputStream.newInstance(stream);
//            // Pre-create the CodedInputStream so that we can remove the size limit restriction
//            // when parsing.
//            cis.setSizeLimit(Integer.MAX_VALUE);
//            return parseFrom(cis);
        }

        private T parseFrom(CodedInputStream stream) throws InvalidProtocolBufferException {
            T message = parser.parseFrom(stream, globalRegistry);
            try {
                stream.checkLastTagWas(0);
                return message;
            } catch (InvalidProtocolBufferException e) {
                e.setUnfinishedMessage(message);
                throw e;
            }
        }
    }
}