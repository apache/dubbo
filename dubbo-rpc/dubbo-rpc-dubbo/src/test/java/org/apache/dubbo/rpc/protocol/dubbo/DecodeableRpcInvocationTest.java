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
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBufferInputStream;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.decode.MockChannel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.protocol.dubbo.DubboCodec.DUBBO_VERSION;

/**
 * {@link DecodeableRpcInvocation}
 */
public class DecodeableRpcInvocationTest {

    @Test
    public void test() throws Exception {
        // Simulate the data called by the client(The called data is stored in invocation and written to the buffer)
        URL url = new ServiceConfigURL("dubbo", "127.0.0.1", 9103, DemoService.class.getName(), VERSION_KEY, "1.0.0");
        RpcInvocation inv = new RpcInvocation(null, "sayHello", DemoService.class.getName(), "", new Class<?>[]{String.class}, new String[]{"yug"});
        inv.setObjectAttachment(PATH_KEY, url.getPath());
        inv.setObjectAttachment(VERSION_KEY, url.getVersion());
        inv.setObjectAttachment(DUBBO_VERSION_KEY, DUBBO_VERSION);
        inv.setObjectAttachment("k1", "v1");
        inv.setObjectAttachment("k2", "v2");
        inv.setTargetServiceUniqueName(url.getServiceKey());
        // Write the data of inv to the buffer
        Byte proto = CodecSupport.getIDByName("hessian2");
        ChannelBuffer buffer = writeBuffer(url, inv, proto);

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = new ApplicationModel(frameworkModel);
        applicationModel.getDefaultModule().getServiceRepository().registerService(DemoService.class.getName(), DemoService.class);
        frameworkModel.getServiceRepository().registerProviderUrl(url);

        // Simulate the server to decode
        Channel channel = new MockChannel();
        Request request = new Request(1);
        ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, buffer.readableBytes());
        DecodeableRpcInvocation decodeableRpcInvocation = new DecodeableRpcInvocation(frameworkModel, channel, request, is, proto);
        decodeableRpcInvocation.decode();

        // Verify that the decodeableRpcInvocation data decoded by the server is consistent with the invocation data of the client
        Assertions.assertEquals(request.getVersion(), DUBBO_VERSION);
        Assertions.assertEquals(decodeableRpcInvocation.getObjectAttachment(DUBBO_VERSION_KEY), DUBBO_VERSION);
        Assertions.assertEquals(decodeableRpcInvocation.getObjectAttachment(VERSION_KEY), inv.getObjectAttachment(VERSION_KEY));
        Assertions.assertEquals(decodeableRpcInvocation.getObjectAttachment(PATH_KEY), inv.getObjectAttachment(PATH_KEY));
        Assertions.assertEquals(decodeableRpcInvocation.getMethodName(), inv.getMethodName());
        Assertions.assertEquals(decodeableRpcInvocation.getParameterTypesDesc(), inv.getParameterTypesDesc());
        Assertions.assertArrayEquals(decodeableRpcInvocation.getParameterTypes(), inv.getParameterTypes());
        Assertions.assertArrayEquals(decodeableRpcInvocation.getArguments(), inv.getArguments());
        Assertions.assertTrue(CollectionUtils.mapEquals(decodeableRpcInvocation.getObjectAttachments(), inv.getObjectAttachments()));
        Assertions.assertEquals(decodeableRpcInvocation.getTargetServiceUniqueName(), inv.getTargetServiceUniqueName());

        frameworkModel.destroy();
    }

    private ChannelBuffer writeBuffer(URL url, RpcInvocation inv, Byte proto) throws IOException {
        Serialization serialization = CodecSupport.getSerializationById(proto);
        ChannelBuffer buffer = ChannelBuffers.buffer(1024);
        ChannelBufferOutputStream outputStream = new ChannelBufferOutputStream(buffer);
        ObjectOutput out = serialization.serialize(url, outputStream);
        out.writeUTF(inv.getAttachment(DUBBO_VERSION_KEY)); // dubbo version
        out.writeUTF(inv.getAttachment(PATH_KEY)); // path
        out.writeUTF(inv.getAttachment(VERSION_KEY)); // version
        out.writeUTF(inv.getMethodName()); // methodName
        out.writeUTF(inv.getParameterTypesDesc()); // parameterTypesDesc
        Object[] args = inv.getArguments();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                out.writeObject(args[i]); // args
            }
        }
        out.writeAttachments(inv.getObjectAttachments()); // attachments
        out.flushBuffer();
        outputStream.close();
        return buffer;
    }
}
