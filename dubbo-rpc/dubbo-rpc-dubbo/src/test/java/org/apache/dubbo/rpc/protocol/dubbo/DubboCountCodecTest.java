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

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture;
import org.apache.dubbo.remoting.exchange.support.MultiMessage;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.decode.MockChannel;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.apache.dubbo.rpc.Constants.INPUT_KEY;
import static org.apache.dubbo.rpc.Constants.OUTPUT_KEY;

class DubboCountCodecTest {

    @Test
    void test() throws Exception {
        DubboCountCodec dubboCountCodec = new DubboCountCodec(FrameworkModel.defaultModel());
        ChannelBuffer buffer = ChannelBuffers.buffer(2048);
        Channel channel = new MockChannel();
        Assertions.assertEquals(Codec2.DecodeResult.NEED_MORE_INPUT, dubboCountCodec.decode(channel, buffer));
        List<DefaultFuture> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Request request = new Request(i);
            futures.add(DefaultFuture.newFuture(channel, request, 1000, null));
            RpcInvocation rpcInvocation = new RpcInvocation(null, "echo", DemoService.class.getName(), "", new Class<?>[]{String.class}, new String[]{"yug"});
            request.setData(rpcInvocation);
            dubboCountCodec.encode(channel, buffer, request);
        }

        for (int i = 0; i < 10; i++) {
            Response response = new Response(i);
            AppResponse appResponse = new AppResponse(i);
            response.setResult(appResponse);
            dubboCountCodec.encode(channel, buffer, response);
        }

        MultiMessage multiMessage = (MultiMessage) dubboCountCodec.decode(channel, buffer);
        Assertions.assertEquals(20, multiMessage.size());
        int requestCount = 0;
        int responseCount = 0;
        Iterator iterator = multiMessage.iterator();
        while (iterator.hasNext()) {
            Object result = iterator.next();
            if (result instanceof Request) {
                requestCount++;
                Object bytes = ((RpcInvocation) ((Request) result).getData()).getObjectAttachment(INPUT_KEY);
                Assertions.assertNotNull(bytes);
            } else if (result instanceof Response) {
                responseCount++;
                Object bytes = ((AppResponse) ((Response) result).getResult()).getObjectAttachment(OUTPUT_KEY);
                Assertions.assertNotNull(bytes);
            }
        }
        Assertions.assertEquals(requestCount, 10);
        Assertions.assertEquals(responseCount, 10);

        futures.forEach(DefaultFuture::cancel);
    }

}
