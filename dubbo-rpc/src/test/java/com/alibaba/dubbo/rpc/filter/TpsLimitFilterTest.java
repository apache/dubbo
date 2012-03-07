/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.rpc.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcStatus;
import com.alibaba.dubbo.rpc.support.MockInvocation;
import com.alibaba.dubbo.rpc.support.TestInvoker;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class TpsLimitFilterTest {

    private TpsLimitFilter filter = new TpsLimitFilter();

    @Test
    public void testWithoutCount() throws Exception {
        URL url = URL.valueOf("test://test");
        url = url.addParameter(Constants.TPS_MAX_KEY, 5);
        Invoker<TpsLimitFilterTest> invoker = new TestInvoker<TpsLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        filter.invoke(invoker, invocation);
    }
    
    @Test(expected = RpcException.class)
    public void testFail() throws Exception {
        URL url = URL.valueOf("test://test");
        url = url.addParameter(Constants.TPS_MAX_KEY, 5);
        Invoker<TpsLimitFilterTest> invoker = new TestInvoker<TpsLimitFilterTest>(url);
        Invocation invocation = new MockInvocation();
        for (int i = 0; i < 10; i++) {
            RpcStatus.beginCount(url, invocation.getMethodName());
            Thread.sleep(100);
            RpcStatus.endCount(url, invocation.getMethodName(), 100, true);
        }
        filter.invoke(invoker, invocation);
    }

}
