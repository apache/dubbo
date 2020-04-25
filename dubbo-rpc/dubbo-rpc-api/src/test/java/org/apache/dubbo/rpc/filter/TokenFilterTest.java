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
package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TokenFilterTest {

    private TokenFilter tokenFilter = new TokenFilter();

    @Test
    public void testInvokeWithToken() throws Exception {
        String token = "token";

        Invoker invoker = Mockito.mock(Invoker.class);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&token=" + token);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));

        Map<String, Object> attachments = new HashMap<>();
        attachments.put(TOKEN_KEY, token);
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getObjectAttachments()).thenReturn(attachments);

        Result result = tokenFilter.invoke(invoker, invocation);
        Assertions.assertEquals("result", result.getValue());
    }

    @Test
    public void testInvokeWithWrongToken() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            String token = "token";

            Invoker invoker = Mockito.mock(Invoker.class);
            URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&token=" + token);
            when(invoker.getUrl()).thenReturn(url);
            when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));

            Map<String, Object> attachments = new HashMap<>();
            attachments.put(TOKEN_KEY, "wrongToken");
            Invocation invocation = Mockito.mock(Invocation.class);
            when(invocation.getObjectAttachments()).thenReturn(attachments);

            tokenFilter.invoke(invoker, invocation);
        });
    }

    @Test
    public void testInvokeWithoutToken() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            String token = "token";

            Invoker invoker = Mockito.mock(Invoker.class);
            URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&token=" + token);
            when(invoker.getUrl()).thenReturn(url);
            when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));

            Invocation invocation = Mockito.mock(Invocation.class);

            tokenFilter.invoke(invoker, invocation);
        });
    }
}

