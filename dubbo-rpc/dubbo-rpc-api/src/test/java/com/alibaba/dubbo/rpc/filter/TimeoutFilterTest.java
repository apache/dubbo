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
package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.support.BlockMyInvoker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.mockito.Mockito.*;

public class TimeoutFilterTest {

    private TimeoutFilter timeoutFilter = new TimeoutFilter();

    @Test
    public void testInvokeWithoutTimeout() throws Exception {
        int timeout = 3000;

        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new RpcResult("result"));
        when(invoker.getUrl()).thenReturn(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&timeout=" + timeout));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testInvokeWithoutTimeout");

        Result result = timeoutFilter.invoke(invoker, invocation);
        Assert.assertEquals("result", result.getValue());
    }

    @Test
    public void testInvokeWithTimeout() throws Exception {
        int timeout = 100;

        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&timeout=" + timeout);
        Invoker invoker = new BlockMyInvoker(url, (timeout + 100));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testInvokeWithTimeout");

        Result result = timeoutFilter.invoke(invoker, invocation);
        Assert.assertEquals("alibaba", result.getValue());

    }
}
