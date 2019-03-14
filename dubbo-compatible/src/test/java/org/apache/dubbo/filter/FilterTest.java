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

package org.apache.dubbo.filter;

import org.apache.dubbo.rpc.RpcException;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class FilterTest {

    Filter myFilter = new MyFilter();

    @Test
    public void testInvokeException() {
        try {
            Invoker<FilterTest> invoker = new LegacyInvoker<FilterTest>(null);
            Invocation invocation = new LegacyInvocation("aa");
            myFilter.invoke(invoker, invocation);
            fail();
        } catch (RpcException e) {
            Assertions.assertTrue(e.getMessage().contains("arg0 illegal"));
        }
    }

    @Test
    public void testDefault() {
        Invoker<FilterTest> invoker = new LegacyInvoker<FilterTest>(null);
        Invocation invocation = new LegacyInvocation("bbb");
        Result res = myFilter.invoke(invoker, invocation);
        System.out.println(res);
    }

    @AfterAll
    public static void tear() {
        Assertions.assertEquals(2, MyFilter.count);
    }
}
