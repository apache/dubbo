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

import java.util.HashMap;
import java.util.Map;
/**
 * @since 2.7.0, use https://github.com/dubbo/dubbo-rpc-native-thrift instead
 */
@Deprecated
public enum ThriftType {

    BOOL, BYTE, I16, I32, I64, DOUBLE, STRING;

    private static final Map<Class<?>, ThriftType> types =
            new HashMap<Class<?>, ThriftType>();

    static {
        put(boolean.class, BOOL);
        put(Boolean.class, BOOL);
        put(byte.class, BYTE);
        put(Byte.class, BYTE);
        put(short.class, I16);
    }

    public static ThriftType get(Class<?> key) {
        if (key != null) {
            return types.get(key);
        }
        throw new NullPointerException("key == null");
    }

    private static void put(Class<?> key, ThriftType value) {
        types.put(key, value);
    }

}
