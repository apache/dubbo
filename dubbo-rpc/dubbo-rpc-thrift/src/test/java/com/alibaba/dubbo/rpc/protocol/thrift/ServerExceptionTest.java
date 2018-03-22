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
package com.alibaba.dubbo.rpc.protocol.thrift;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.gen.dubbo.$__DemoStub;

import org.junit.Assert;
import org.junit.Test;

public class ServerExceptionTest extends AbstractTest {

    @Override
    protected $__DemoStub.Iface getServiceImpl() {

        return new $__DemoStub.Iface() {

            public boolean echoBool(boolean arg) {

                return false;
            }

            public byte echoByte(byte arg) {

                return 0;
            }

            public short echoI16(short arg) {

                return 0;
            }

            public int echoI32(int arg) {

                return 0;
            }

            public long echoI64(long arg) {

                return 0;
            }

            public double echoDouble(double arg) {
                return 0;
            }

            public String echoString(String arg) {
                // On server side, thrift can not handle exceptions not declared in idl
                throw new RuntimeException("just for test");
            }
        };

    }

    @Test(expected = RpcException.class)
    public void testServerException() throws Exception {

        Assert.assertNotNull(invoker);

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName("echoString");

        invocation.setParameterTypes(new Class<?>[]{String.class});

        String arg = "Hello, World!";

        invocation.setArguments(new Object[]{arg});

        Result result = invoker.invoke(invocation);

        System.out.println(result);

    }

    @Override
    protected URL getUrl() {
        URL url = super.getUrl();
//        url = url.addParameter( Constants.TIMEOUT_KEY, Integer.MAX_VALUE );
        return url;
    }

}
