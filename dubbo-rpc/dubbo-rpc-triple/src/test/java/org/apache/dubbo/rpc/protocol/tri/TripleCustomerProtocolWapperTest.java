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

import com.google.protobuf.ByteString;
import org.apache.dubbo.triple.TripleWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TripleCustomerProtocolWapperTest {

    @Test
    void testVarInt() {
        int seed = 3;
        while (seed < Integer.MAX_VALUE && seed > 0) {
            byte[] varIntBytes = TripleCustomerProtocolWapper.varIntEncode(seed);
            ByteBuffer buffer = ByteBuffer.wrap(varIntBytes);
            int numDecodeFromVarIntByte = TripleCustomerProtocolWapper.readRawVarint32(buffer);
            Assertions.assertEquals(seed, numDecodeFromVarIntByte);
            seed = seed * 7;
        }
    }

    @Test
    void testRangeViaInt() {
        for (int index = 0; index < 100000; index++) {
            byte[] varIntBytes = TripleCustomerProtocolWapper.varIntEncode(index);
            ByteBuffer buffer = ByteBuffer.wrap(varIntBytes);
            int numDecodeFromVarIntByte = TripleCustomerProtocolWapper.readRawVarint32(buffer);
            Assertions.assertEquals(index, numDecodeFromVarIntByte);
        }
    }

    @Test
    void testTripleRequestWrapperWithOnlySerializeType() {
        String serialize = "hession";
        TripleCustomerProtocolWapper.TripleRequestWrapper.Builder builder = TripleCustomerProtocolWapper.TripleRequestWrapper.Builder.newBuilder();
        TripleCustomerProtocolWapper.TripleRequestWrapper tripleRequestWrapper = builder.setSerializeType(serialize).build();
        final TripleWrapper.TripleRequestWrapper.Builder pbbuilder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(serialize);
        Assertions.assertArrayEquals(tripleRequestWrapper.toByteArray(), pbbuilder.build().toByteArray());
    }

    @Test
    void testTripleRequestWrapperBuild() {

        byte[] firstArg = "i am first arg".getBytes(StandardCharsets.UTF_8);
        byte[] secondArg = "i am second arg".getBytes(StandardCharsets.UTF_8);

        String serialize = "hession";
        TripleCustomerProtocolWapper.TripleRequestWrapper.Builder builder = TripleCustomerProtocolWapper.TripleRequestWrapper.Builder.newBuilder();
        TripleCustomerProtocolWapper.TripleRequestWrapper tripleRequestWrapper = builder
            .setSerializeType(serialize)
            .addArgTypes("com.google.protobuf.ByteString")
            .addArgTypes("org.apache.dubbo.common.URL")
            .addArgs(firstArg)
            .addArgs(secondArg)
            .build();

        final TripleWrapper.TripleRequestWrapper.Builder pbbuilder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(serialize)
            .addArgTypes("com.google.protobuf.ByteString")
            .addArgTypes("org.apache.dubbo.common.URL")
            .addArgs(ByteString.copyFrom(firstArg))
            .addArgs(ByteString.copyFrom(secondArg));

        Assertions.assertArrayEquals(tripleRequestWrapper.toByteArray(), pbbuilder.build().toByteArray());
    }

    @Test
    void testTripleRequestWrapperParseFrom() {
        byte[] firstArg = "i am first arg".getBytes(StandardCharsets.UTF_8);
        byte[] secondArg = "i am second arg".getBytes(StandardCharsets.UTF_8);

        String serialize = "hession4";
        TripleCustomerProtocolWapper.TripleRequestWrapper.Builder builder = TripleCustomerProtocolWapper.TripleRequestWrapper.Builder.newBuilder();
        TripleCustomerProtocolWapper.TripleRequestWrapper tripleRequestWrapper = builder
            .setSerializeType(serialize)
            .addArgTypes("com.google.protobuf.ByteString")
            .addArgTypes("org.apache.dubbo.common.URL")
            .addArgs(firstArg)
            .addArgs(secondArg)
            .build();
        final TripleWrapper.TripleRequestWrapper.Builder pbbuilder = TripleWrapper.TripleRequestWrapper.newBuilder()
            .setSerializeType(serialize)
            .addArgTypes("com.google.protobuf.ByteString")
            .addArgTypes("org.apache.dubbo.common.URL")
            .addArgs(ByteString.copyFrom(firstArg))
            .addArgs(ByteString.copyFrom(secondArg));

        TripleCustomerProtocolWapper.TripleRequestWrapper parseFrom = TripleCustomerProtocolWapper.TripleRequestWrapper.parseFrom(pbbuilder.build().toByteArray());
        Assertions.assertEquals(parseFrom.getSerializeType(), tripleRequestWrapper.getSerializeType());
        Assertions.assertArrayEquals(parseFrom.getArgs().toArray(), tripleRequestWrapper.getArgs().toArray());
        Assertions.assertArrayEquals(parseFrom.getArgTypes().toArray(), tripleRequestWrapper.getArgTypes().toArray());
    }

    @Test
    void testTripleResponseWrapperWithNullData() {
        String serializeType = "hession4";
        String type = "String";
        TripleCustomerProtocolWapper.TripleResponseWrapper.Builder builder = TripleCustomerProtocolWapper.TripleResponseWrapper.Builder.newBuilder();
        TripleCustomerProtocolWapper.TripleResponseWrapper tripleResponseWrapper = builder
            .setSerializeType(serializeType)
            .setType(type)
            .build();
        TripleWrapper.TripleResponseWrapper.Builder pbBuilder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setType(type)
            .setSerializeType(serializeType);
        Assertions.assertArrayEquals(pbBuilder.build().toByteArray(), tripleResponseWrapper.toByteArray());
    }

    @Test
    void testTripleResponseWrapper() {
        String serializeType = "hession4";
        String type = "String";
        String data = "/*\n" +
            " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
            " * contributor license agreements.  See the NOTICE file distributed with\n" +
            " * this work for additional information regarding copyright ownership.\n" +
            " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
            " * (the \"License\"); you may not use this file except in compliance with\n" +
            " * the License.  You may obtain a copy of the License at\n" +
            " *\n" +
            " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
            " *\n" +
            " * Unless required by applicable law or agreed to in writing, software\n" +
            " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            " * See the License for the specific language governing permissions and\n" +
            " * limitations under the License.\n" +
            " */";
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        TripleCustomerProtocolWapper.TripleResponseWrapper.Builder builder = TripleCustomerProtocolWapper.TripleResponseWrapper.Builder.newBuilder();
        TripleCustomerProtocolWapper.TripleResponseWrapper tripleResponseWrapper = builder
            .setSerializeType(serializeType)
            .setType(type)
            .setData(dataBytes)
            .build();
        TripleWrapper.TripleResponseWrapper.Builder pbBuilder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setType(type)
            .setData(ByteString.copyFrom(dataBytes))
            .setSerializeType(serializeType);
        Assertions.assertArrayEquals(pbBuilder.build().toByteArray(), tripleResponseWrapper.toByteArray());
    }

    @Test
    void testTripleResponseParseFrom() {
        String serializeType = "hession4";
        String type = "String";
        String data = "/*\n" +
            " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
            " * contributor license agreements.  See the NOTICE file distributed with\n" +
            " * this work for additional information regarding copyright ownership.\n" +
            " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
            " * (the \"License\"); you may not use this file except in compliance with\n" +
            " * the License.  You may obtain a copy of the License at\n" +
            " *\n" +
            " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
            " *\n" +
            " * Unless required by applicable law or agreed to in writing, software\n" +
            " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            " * See the License for the specific language governing permissions and\n" +
            " * limitations under the License.\n" +
            " */";
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        TripleWrapper.TripleResponseWrapper.Builder pbBuilder = TripleWrapper.TripleResponseWrapper.newBuilder()
            .setType(type)
            .setData(ByteString.copyFrom(dataBytes))
            .setSerializeType(serializeType);
        byte[] pbRawBytes = pbBuilder.build().toByteArray();
        TripleCustomerProtocolWapper.TripleResponseWrapper tripleResponseWrapper = TripleCustomerProtocolWapper.TripleResponseWrapper.parseFrom(pbRawBytes);
        Assertions.assertArrayEquals(pbRawBytes, tripleResponseWrapper.toByteArray());
        Assertions.assertArrayEquals(dataBytes, tripleResponseWrapper.getData());
        Assertions.assertEquals(serializeType, tripleResponseWrapper.getSerializeType());
        Assertions.assertEquals(type, tripleResponseWrapper.getType());
    }

}
