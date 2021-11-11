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
package org.apache.dubbo.rpc.cluster.interceptor;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.interceptor.ConsumerContextClusterInterceptor.NAME;

public class ConsumerContextClusterInterceptorTest {

    private ConsumerContextClusterInterceptor interceptor;

    @BeforeEach
    public void setUp() {
        interceptor = (ConsumerContextClusterInterceptor) ExtensionLoader.
                getExtensionLoader(ClusterInterceptor.class).getExtension(NAME);
    }

    @Test
    public void testGetExtension() {
        Assertions.assertNotNull(interceptor);
        Assertions.assertTrue(interceptor instanceof ConsumerContextClusterInterceptor);
    }

    @Test
    public void testGetActivateExtension() {
        URL url = URL.valueOf("test://test:80");
        List<ClusterInterceptor> interceptors =
                ExtensionLoader.getExtensionLoader(ClusterInterceptor.class).getActivateExtension(url, "");

        Assertions.assertTrue(CollectionUtils.isNotEmpty(interceptors));
        long count = interceptors.stream().
                filter(interceptor -> interceptor instanceof ConsumerContextClusterInterceptor).count();
        Assertions.assertEquals(1l, count);
    }

    @Test
    public void testBefore() {
        AbstractClusterInvoker mockInvoker = Mockito.mock(AbstractClusterInvoker.class);
        RpcContext serverContextBefore = RpcContext.getServerContext();
        RpcInvocation rpcInvocation = new RpcInvocation();

        interceptor.before(mockInvoker, rpcInvocation);

        RpcContext serverContextAfter = RpcContext.getServerContext();

        Assertions.assertNotSame(serverContextBefore, serverContextAfter);
        Assertions.assertSame(mockInvoker, rpcInvocation.getInvoker());
    }

    @Test
    public void testAfter() {
        RpcContext contextBefore = RpcContext.getContext();
        interceptor.after(null, null);
        RpcContext contextAfter = RpcContext.getContext();
        Assertions.assertNotSame(contextBefore, contextAfter);
    }

    @Test
    public void testOnMessage() {
        RpcContext serverContext = RpcContext.getServerContext();
        Result mockResult = Mockito.mock(Result.class);
        Map<String, Object> map = Collections.singletonMap("key", "value");
        Mockito.when(mockResult.getObjectAttachments()).thenReturn(map);
        interceptor.onMessage(mockResult, null, null);

        Map<String, Object> objectAttachments = serverContext.getObjectAttachments();
        Assertions.assertNotNull(objectAttachments);
        Assertions.assertTrue(objectAttachments.size() == 1);
        Assertions.assertEquals("value", objectAttachments.get("key"));
    }

    @Test
    public void testOnError() {
        interceptor.onError(new RpcException(), null, null);
    }

}
