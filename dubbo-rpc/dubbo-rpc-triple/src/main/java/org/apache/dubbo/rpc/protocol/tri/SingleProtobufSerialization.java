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

import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingleProtobufSerialization {
    private static final ConcurrentHashMap<Class<?>, Message> instCache = new ConcurrentHashMap<>();
    private static final ExtensionRegistryLite globalRegistry =
            ExtensionRegistryLite.getEmptyRegistry();
    private final ConcurrentMap<Class<?>, SingleMessageMarshaller<?>> marshallers = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    public static Message defaultInst(Class<?> clz) {
        Message defaultInst = instCache.get(clz);
        if (defaultInst != null) {
            return defaultInst;
        }
        try {
            defaultInst = (Message) clz.getMethod("getDefaultInstance").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Create default protobuf instance failed ", e);
        }
        instCache.put(clz, defaultInst);
        return defaultInst;
    }

    @SuppressWarnings("all")
    public static <T> Parser<T> getParser(Class<T> clz) {
        Message defaultInst = defaultInst(clz);
        return (Parser<T>) defaultInst.getParserForType();
    }

    public Object deserialize(InputStream in, Class<?> clz) throws IOException {
        try {
            return getMarshaller(clz).parse(in);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException(e);
        }
    }

    public void serialize(Object obj, OutputStream os) throws IOException {
        final MessageLite msg = (MessageLite) obj;
        msg.writeTo(os);
    }

    private SingleMessageMarshaller<?> getMarshaller(Class<?> clz) {
        return marshallers.computeIfAbsent(clz, k -> new SingleMessageMarshaller(k));
    }

    public static final class SingleMessageMarshaller<T extends MessageLite> {
        private final Parser<T> parser;

        SingleMessageMarshaller(Class<T> clz) {
            final T inst = (T) defaultInst(clz);
            this.parser = (Parser<T>) inst.getParserForType();
        }

        public T parse(InputStream stream) throws InvalidProtocolBufferException {
            return parser.parseFrom(stream, globalRegistry);
        }
    }

}
