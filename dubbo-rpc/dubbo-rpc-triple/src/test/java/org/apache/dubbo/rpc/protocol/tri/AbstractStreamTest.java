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
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.protocol.tri.support.MockAbstractStreamImpl;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.ByteString;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum.MESSAGE_KEY;
import static org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum.STATUS_DETAIL_KEY;
import static org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum.STATUS_KEY;

/**
 * {@link AbstractStream}
 */
public class AbstractStreamTest {

    private URL url = URL.valueOf("test://127.0.0.1/test");
    private AbstractStream stream = new MockAbstractStreamImpl(url);

    @Test
    public void testTransportError() {
        Exception exception = getException();
        OutboundTransportObserver transportObserver = Mockito.mock(OutboundTransportObserver.class);
        stream.subscribe(transportObserver);
        GrpcStatus grpcStatus = GrpcStatus
            .fromCode(GrpcStatus.Code.INTERNAL)
            .withDescription("TEST")
            .withCause(exception);
        Map<String, Object> attachments = new HashMap<>();
        attachments.put("strKey", "v1");
        attachments.put("binKey", new byte[]{1});
        attachments.put(String.valueOf(Http2Headers.PseudoHeaderName.PATH.value()), "path");
        attachments.put(CommonConstants.GROUP_KEY, "group");

        stream.transportError(grpcStatus, attachments, false);

        ArgumentCaptor<DefaultMetadata> metadataArgumentCaptor = ArgumentCaptor.forClass(DefaultMetadata.class);
        Mockito.verify(transportObserver, Mockito.times(2)).onMetadata(metadataArgumentCaptor.capture(), Mockito.anyBoolean());

        DefaultMetadata defaultMetadata = metadataArgumentCaptor.getValue();
        Assertions.assertEquals(defaultMetadata.get(STATUS_KEY.getHeader()), String.valueOf(grpcStatus.code.code));
        Assertions.assertEquals(defaultMetadata.get(MESSAGE_KEY.getHeader()), grpcStatus.description);
        Assertions.assertNotNull(defaultMetadata.get(STATUS_DETAIL_KEY.getHeader()));
        Assertions.assertTrue(defaultMetadata.contains("strKey".toLowerCase(Locale.ROOT)));
        Assertions.assertTrue(defaultMetadata.contains("binKey".toLowerCase(Locale.ROOT) + TripleConstant.GRPC_BIN_SUFFIX));
        Assertions.assertFalse(defaultMetadata.contains(String.valueOf(Http2Headers.PseudoHeaderName.PATH.value())));
        Assertions.assertFalse(defaultMetadata.contains(CommonConstants.GROUP_KEY));

        // test parseMetadataToAttachmentMap
        Map<String, Object> attachmentMap = stream.parseMetadataToAttachmentMap(defaultMetadata);
        Assertions.assertTrue(attachmentMap.containsKey("strKey".toLowerCase(Locale.ROOT)));
        Assertions.assertTrue(attachmentMap.containsKey("binKey".toLowerCase(Locale.ROOT)));

    }

    @Test
    public void testPackUnPack() {
        TripleWrapper.TripleRequestWrapper requestWrapper = TripleWrapper.TripleRequestWrapper.newBuilder()
            .addArgTypes(ReflectUtils.getDesc(String.class))
            .addArgs(ByteString.copyFrom("TEST_ARG".getBytes(StandardCharsets.UTF_8)))
            .setSerializeType(TripleConstant.HESSIAN4)
            .build();

        byte[] bytes = stream.pack(requestWrapper);
        TripleWrapper.TripleRequestWrapper unpackedData = stream.unpack(bytes, TripleWrapper.TripleRequestWrapper.class);

        Assertions.assertEquals(unpackedData.getArgTypes(0), requestWrapper.getArgTypes(0));
        Assertions.assertEquals(unpackedData.getArgs(0), requestWrapper.getArgs(0));
        Assertions.assertEquals(unpackedData.getArgs(0), requestWrapper.getArgs(0));
        Assertions.assertEquals(unpackedData.getSerializeType(), requestWrapper.getSerializeType());
    }

    @Test
    public void testCodec() {
        String str = "BCN";
        String base64ASCII = stream.encodeBase64ASCII(str.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = stream.decodeASCIIByte(base64ASCII);
        Assertions.assertEquals(str, new String(bytes, StandardCharsets.UTF_8));
    }

    private Exception getException() {
        Exception exception = null;
        try {
            int count = 1 / 0;
        } catch (Exception e) {
            exception = e;
        }
        return exception;
    }
}
