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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolTest {

    IEcho echo = new IEcho() {
        public String echo(String e) {
            return e;
        }
    };

    static {
        InjvmProtocol injvm = InjvmProtocol.getInjvmProtocol(FrameworkModel.defaultModel());
    }

    ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("javassist");

    URL url = URL.valueOf("injvm://localhost:0/org.apache.dubbo.rpc.support.IEcho?interface=org.apache.dubbo.rpc.support.IEcho");

    Invoker<IEcho> invoker = proxyFactory.getInvoker(echo, IEcho.class, url);

    @Test
    public void test_destroyWontCloseAllProtocol() throws Exception {
        Protocol autowireProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        Protocol InjvmProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("injvm");

        assertEquals(0, InjvmProtocol.getDefaultPort());

        InjvmProtocol.export(invoker);

        Invoker<IEcho> refer = InjvmProtocol.refer(IEcho.class, url);
        IEcho echoProxy = proxyFactory.getProxy(refer);

        assertEquals("ok", echoProxy.echo("ok"));

        try {
            autowireProtocol.destroy();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("of interface org.apache.dubbo.rpc.Protocol is not adaptive method!"));
        }

        assertEquals("ok2", echoProxy.echo("ok2"));
    }
}
