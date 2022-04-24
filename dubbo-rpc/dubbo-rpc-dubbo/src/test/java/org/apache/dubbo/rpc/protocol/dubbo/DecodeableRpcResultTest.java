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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBufferInputStream;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.dubbo.decode.MockChannel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_ID_KEY;
import static org.apache.dubbo.rpc.RpcException.BIZ_EXCEPTION;
import static org.apache.dubbo.rpc.protocol.dubbo.DubboCodec.RESPONSE_VALUE_WITH_ATTACHMENTS;
import static org.apache.dubbo.rpc.protocol.dubbo.DubboCodec.RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS;

/**
 * {@link DecodeableRpcResult}
 */
public class DecodeableRpcResultTest {
    private ModuleServiceRepository repository;

    @BeforeEach
    public void setUp() {
        repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();
    }

    @AfterEach
    public void tearDown() {
        FrameworkModel.destroyAll();
    }

    @Test
    public void test() throws Exception {
        // Mock a rpcInvocation, this rpcInvocation is usually generated by the client request, and stored in Request#data
        Byte proto = CodecSupport.getIDByName("fastjson2");
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 9103, DemoService.class.getName(), VERSION_KEY, "1.0.0");
        ServiceDescriptor serviceDescriptor = repository.registerService(DemoService.class);
        ProviderModel providerModel = new ProviderModel(url.getServiceKey(), new DemoServiceImpl(), serviceDescriptor, null, null);
        RpcInvocation rpcInvocation = new RpcInvocation(providerModel, "echo", DemoService.class.getName(), "", new Class<?>[]{String.class}, new String[]{"yug"});
        rpcInvocation.put(SERIALIZATION_ID_KEY, proto);

        // Mock a response result returned from the server and write to the buffer
        Channel channel = new MockChannel();
        Response response = new Response(1);
        Result result = new AppResponse();
        result.setValue("yug");
        response.setResult(result);
        ChannelBuffer buffer = writeBuffer(url, response, proto, false);

        // The client reads and decode the buffer
        InputStream is = new ChannelBufferInputStream(buffer, buffer.readableBytes());
        DecodeableRpcResult decodeableRpcResult = new DecodeableRpcResult(channel, response, is, rpcInvocation, proto);
        decodeableRpcResult.decode();

        // Verify RESPONSE_VALUE_WITH_ATTACHMENTS
        // Verify that the decodeableRpcResult decoded by the client is consistent with the response returned by the server
        Assertions.assertEquals(decodeableRpcResult.getValue(), result.getValue());
        Assertions.assertTrue(CollectionUtils.mapEquals(decodeableRpcResult.getObjectAttachments(), result.getObjectAttachments()));

        // Verify RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS
        Response exResponse = new Response(2);
        Result exResult = new AppResponse();
        exResult.setException(new RpcException(BIZ_EXCEPTION));
        exResponse.setResult(exResult);
        buffer = writeBuffer(url, exResponse, proto, true);
        is = new ChannelBufferInputStream(buffer, buffer.readableBytes());
        decodeableRpcResult = new DecodeableRpcResult(channel, response, is, rpcInvocation, proto);
        decodeableRpcResult.decode();
        Assertions.assertEquals(((RpcException) decodeableRpcResult.getException()).getCode(), ((RpcException) exResult.getException()).getCode());
        Assertions.assertTrue(CollectionUtils.mapEquals(decodeableRpcResult.getObjectAttachments(), exResult.getObjectAttachments()));

    }

    private ChannelBuffer writeBuffer(URL url, Response response, Byte proto, boolean writeEx) throws IOException {
        Serialization serialization = CodecSupport.getSerializationById(proto);
        ChannelBuffer buffer = ChannelBuffers.buffer(1024 * 10);
        ChannelBufferOutputStream outputStream = new ChannelBufferOutputStream(buffer);
        ObjectOutput out = serialization.serialize(url, outputStream);
        Result result = (Result) response.getResult();
        if (!writeEx) {
            out.writeByte(RESPONSE_VALUE_WITH_ATTACHMENTS);
            out.writeObject(result.getValue());
        } else {
            out.writeByte(RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS);
            out.writeThrowable(result.getException());
        }
        result.getObjectAttachments().put(DUBBO_VERSION_KEY, Version.getProtocolVersion());
        result.getObjectAttachments().put("k1", "v1");
        out.writeAttachments(result.getObjectAttachments());
        out.flushBuffer();
        outputStream.close();
        return buffer;
    }


}
