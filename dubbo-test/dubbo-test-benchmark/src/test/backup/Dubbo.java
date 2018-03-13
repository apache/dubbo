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
package com.dubbo.serialize.benchmark;

import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.common.serialize.support.dubbo.GenericObjectInput;
import com.alibaba.dubbo.common.serialize.support.dubbo.GenericObjectOutput;

import data.media.MediaContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Dubbo Seriazition Benchmark
 */
public class Dubbo {

    public static Serializer<Object> GenericSerializer = new Serializer<Object>() {
        public Object deserialize(byte[] array) throws Exception {
            GenericObjectInput objectInput = new GenericObjectInput(new ByteArrayInputStream(array));
            return objectInput.readObject();
        }

        public byte[] serialize(Object data) throws java.io.IOException {
            UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(10240);
            GenericObjectOutput objectOutput = new GenericObjectOutput(os);
            objectOutput.writeObject(data);
            objectOutput.flushBuffer();
            return os.toByteArray();
        }

        public String getName() {
            return "dubbo";
        }
    };

    public static void register(TestGroups groups) {
        groups.media.add(JavaBuiltIn.MediaTransformer, Dubbo.<MediaContent>GenericSerializer());
    }

    // ------------------------------------------------------------
    // Serializer (just one)

    public static <T> Serializer<T> GenericSerializer() {
        @SuppressWarnings("unchecked")
        Serializer<T> s = (Serializer<T>) GenericSerializer;
        return s;
    }
}
