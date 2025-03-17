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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CustomExchangerTest {

    @Test
    public void testGeneralizedCallWithCustomExchanger() throws Exception {
        URL serverUrl = URL.valueOf("dubbo://localhost:20880/com.example.TestService?exchanger=custom");
        ExchangeHandler handler = new ExchangeHandler() {
            @Override
            public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
                Map<String, Object> response = new HashMap<>();
                response.put("result", "Hello from CustomExchanger");
                return CompletableFuture.completedFuture(response);
            }

            @Override
            public void connected(Channel channel) throws RemotingException {}

            @Override
            public void disconnected(Channel channel) throws RemotingException {}

            @Override
            public void sent(Channel channel, Object message) throws RemotingException {}

            @Override
            public void received(Channel channel, Object message) throws RemotingException {}

            @Override
            public void caught(Channel channel, Throwable exception) throws RemotingException {}
        };

        CustomExchanger exchanger = new CustomExchanger();
        ExchangeServer server = exchanger.bind(serverUrl, handler);
        assertNotNull(server);

        URL clientUrl = URL.valueOf("dubbo://localhost:20880/com.example.TestService?exchanger=custom&generic=true");
        CustomExchangeClient client = (CustomExchangeClient) exchanger.connect(clientUrl, handler);

        Map<String, Object> request = new HashMap<>();
        request.put("method", "sayHello");
        request.put("parameterTypes", new String[] {"java.lang.String"});
        request.put("args", new Object[] {"World"});
        client.send(request);

        client.close();
        server.close();
    }
}
