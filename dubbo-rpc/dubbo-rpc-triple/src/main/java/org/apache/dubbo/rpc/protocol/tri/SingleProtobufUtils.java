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

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.EnumValue;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.google.protobuf.StringValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingleProtobufUtils {
    private static final ConcurrentHashMap<Class<?>, Message> INST_CACHE = new ConcurrentHashMap<>();
    private static final ExtensionRegistryLite GLOBAL_REGISTRY = ExtensionRegistryLite.getEmptyRegistry();
    private static final ConcurrentMap<Class<?>, SingleMessageMarshaller<?>> MARSHALLER_CACHE = new ConcurrentHashMap<>();

    static {
        // Built-in types need to be registered in advance
        marshaller(Empty.getDefaultInstance());
        marshaller(BoolValue.getDefaultInstance());
        marshaller(Int32Value.getDefaultInstance());
        marshaller(Int64Value.getDefaultInstance());
        marshaller(FloatValue.getDefaultInstance());
        marshaller(DoubleValue.getDefaultInstance());
        marshaller(BytesValue.getDefaultInstance());
        marshaller(StringValue.getDefaultInstance());
        marshaller(EnumValue.getDefaultInstance());
        marshaller(ListValue.getDefaultInstance());
    }

    static boolean isSupported(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return MessageLite.class.isAssignableFrom(clazz);
    }

    public static <T extends MessageLite> void marshaller(T defaultInstance) {
        MARSHALLER_CACHE.put(defaultInstance.getClass(), new SingleMessageMarshaller<>(defaultInstance));
    }

    @SuppressWarnings("all")
    public static Message defaultInst(Class<?> clz) {
        Message defaultInst = INST_CACHE.get(clz);
        if (defaultInst != null) {
            return defaultInst;
        }
        try {
            defaultInst = (Message) clz.getMethod("getDefaultInstance").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Create default protobuf instance failed ", e);
        }
        INST_CACHE.put(clz, defaultInst);
        return defaultInst;
    }

    @SuppressWarnings("all")
    public static <T> Parser<T> getParser(Class<T> clz) {
        Message defaultInst = defaultInst(clz);
        return (Parser<T>) defaultInst.getParserForType();
    }


    public static <T> T deserialize(InputStream in, Class<T> clz) throws IOException {
        if (!isSupported(clz)) {
            throw new IllegalArgumentException("This serialization only support google protobuf messages, but the " +
                "actual input type is :" + clz.getName());
        }
        try {
            return (T) getMarshaller(clz).parse(in);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException(e);
        }
    }

    public static void serialize(Object obj, OutputStream os) throws IOException {
        final MessageLite msg = (MessageLite) obj;
        msg.writeTo(os);
    }

    private static SingleMessageMarshaller<?> getMarshaller(Class<?> clz) {
        return MARSHALLER_CACHE.computeIfAbsent(clz, k -> new SingleMessageMarshaller(k));
    }

    public static final class SingleMessageMarshaller<T extends MessageLite> {
        private final Parser<T> parser;
        private final T defaultInstance;

        SingleMessageMarshaller(Class<T> clz) {
            this.defaultInstance = (T) defaultInst(clz);
            this.parser = (Parser<T>) defaultInstance.getParserForType();
        }

        SingleMessageMarshaller(T defaultInstance) {
            this.defaultInstance = defaultInstance;
            this.parser = (Parser<T>) defaultInstance.getParserForType();
        }

        public T parse(InputStream stream) throws InvalidProtocolBufferException {
            return parser.parseFrom(stream, GLOBAL_REGISTRY);
        }
    }

}
