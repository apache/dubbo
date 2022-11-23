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

import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.EnumValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.StringValue;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * {@link SingleProtobufUtils}
 */
class SingleProtobufUtilsTest {

    @Test
    void test() throws IOException {
        Assertions.assertFalse(SingleProtobufUtils.isSupported(SingleProtobufUtilsTest.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(Empty.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(BoolValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(Int32Value.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(Int64Value.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(FloatValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(DoubleValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(BytesValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(StringValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(EnumValue.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(ListValue.class));

        Assertions.assertTrue(SingleProtobufUtils.isSupported(HealthCheckResponse.class));
        Assertions.assertTrue(SingleProtobufUtils.isSupported(HealthCheckRequest.class));

        Message message = SingleProtobufUtils.defaultInst(HealthCheckRequest.class);
        Assertions.assertNotNull(message);
        Parser<HealthCheckRequest> parser = SingleProtobufUtils.getParser(HealthCheckRequest.class);
        Assertions.assertNotNull(parser);

        TripleWrapper.TripleRequestWrapper requestWrapper = TripleWrapper.TripleRequestWrapper.newBuilder()
                .setSerializeType("hessian4").build();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SingleProtobufUtils.serialize(requestWrapper, bos);

        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        TripleWrapper.TripleRequestWrapper tripleRequestWrapper = SingleProtobufUtils.deserialize(bis, TripleWrapper.TripleRequestWrapper.class);
        Assertions.assertEquals(tripleRequestWrapper.getSerializeType(), "hessian4");
    }
}
