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

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.GeneratedMessageV3.Builder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import java.lang.reflect.Method;

public class ProtobufUtils {

    static boolean isSupported(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (GeneratedMessageV3.class.isAssignableFrom(clazz)) {
            return true;
        }
        return false;
    }

    static <T> T deserialize(String json, Class<T> requestClass) throws InvalidProtocolBufferException {
        Builder builder;
        try {
            builder = getMessageBuilder(requestClass);
        } catch (Exception e) {
            throw new IllegalArgumentException("Get google protobuf message builder from " + requestClass.getName() + "failed", e);
        }
        JsonFormat.parser().merge(json, builder);
        return (T) builder.build();
    }

    static String serialize(Object value) throws InvalidProtocolBufferException {
        Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        return printer.print((MessageOrBuilder) value);
    }

    private static Builder getMessageBuilder(Class<?> requestType) throws Exception {
        Method method = requestType.getMethod("newBuilder");
        return (Builder) method.invoke(null, null);
    }
}