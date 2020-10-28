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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.gen.dubbo.Demo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class ThriftProtocolTest extends AbstractTest {

    public final int DEFAULT_PORT = NetUtils.getAvailablePort();

    private ThriftProtocol protocol;

    private Invoker<Demo> invoker;

    private URL url;

    @BeforeEach
    public void setUp() throws Exception {

        init();

        protocol = new ThriftProtocol();

        url = URL.valueOf(ThriftProtocol.NAME + "://127.0.0.1:" + PORT + "/" + Demo.class.getName());

    }

    @AfterEach
    public void tearDown() throws Exception {

        destroy();

        if (protocol != null) {
            protocol.destroy();
            protocol = null;
        }

        if (invoker != null) {
            invoker.destroy();
            invoker = null;
        }

    }
/*
    @Test
    public void testRefer() throws Exception {
        // FIXME
        *//*invoker = protocol.refer( Demo.class, url );

        Assertions.assertNotNull( invoker );

        RpcInvocation invocation = new RpcInvocation();

        invocation.setMethodName( "echoString" );

        invocation.setParameterTypes( new Class<?>[]{ String.class } );

        String arg = "Hello, World!";

        invocation.setArguments( new Object[] { arg } );

        Result result = invoker.invoke( invocation );

        Assertions.assertEquals( arg, result.getResult() );*//*

    }*/

}
