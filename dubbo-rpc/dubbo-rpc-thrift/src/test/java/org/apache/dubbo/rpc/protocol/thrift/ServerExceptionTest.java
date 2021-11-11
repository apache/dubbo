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
package org.apache.dubbo.rpc.protocol.thrift;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.gen.dubbo.$__DemoStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testServerException() throws Exception {
        Assertions.assertThrows(RpcException.class, () -> {
            Assertions.assertNotNull(invoker);

            RpcInvocation invocation = new RpcInvocation();

            invocation.setMethodName("echoString");

            invocation.setParameterTypes(new Class<?>[]{String.class});

            String arg = "Hello, World!";

            invocation.setArguments(new Object[]{arg});

            Result result = invoker.invoke(invocation);

            System.out.println(result);
        });

    }

    @Override
    protected URL getUrl() {
        URL url = super.getUrl();
//        url = url.addParameter( Constants.TIMEOUT_KEY, Integer.MAX_VALUE );
        return url;
    }

}
