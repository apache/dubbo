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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcResult;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

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
        when(invoker.invoke(any(Invocation.class))).thenReturn(new RpcResult("result"));

        Map<String, String> attachments = new HashMap<String, String>();
        attachments.put(Constants.TOKEN_KEY, token);
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getAttachments()).thenReturn(attachments);

        Result result = tokenFilter.invoke(invoker, invocation);
        Assert.assertEquals("result", result.getValue());
    }

    @Test(expected = RpcException.class)
    public void testInvokeWithWrongToken() throws Exception {
        String token = "token";

        Invoker invoker = Mockito.mock(Invoker.class);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&token=" + token);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new RpcResult("result"));

        Map<String, String> attachments = new HashMap<String, String>();
        attachments.put(Constants.TOKEN_KEY, "wrongToken");
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getAttachments()).thenReturn(attachments);

        tokenFilter.invoke(invoker, invocation);
    }

    @Test(expected = RpcException.class)
    public void testInvokeWithoutToken() throws Exception {
        String token = "token";

        Invoker invoker = Mockito.mock(Invoker.class);
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&token=" + token);
        when(invoker.getUrl()).thenReturn(url);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new RpcResult("result"));

        Invocation invocation = Mockito.mock(Invocation.class);

        tokenFilter.invoke(invoker, invocation);
    }
}

