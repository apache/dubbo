/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.injvm;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author ding.lid
 */
public class ProtocolTest {

    IEcho echo = new IEcho() {
        public String echo(String e) {
            return e;
        }
    };

    ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("javassist");

    URL url = URL.valueOf("injvm://localhost:0/com.alibaba.dubbo.rpc.support.IEcho?interface=com.alibaba.dubbo.rpc.support.IEcho");

    Invoker<IEcho> invoker = proxyFactory.getInvoker(echo, IEcho.class, url);

    @Test
    public void test_destroyWontCloseAllProtocol() throws Exception {
        Protocol autowireProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

        Protocol InjvmProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("injvm");

        InjvmProtocol.export(invoker);

        Invoker<IEcho> refer = InjvmProtocol.refer(IEcho.class, url);
        IEcho echoProxy = proxyFactory.getProxy(refer);

        assertEquals("ok", echoProxy.echo("ok"));

        try {
            autowireProtocol.destroy();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("of interface com.alibaba.dubbo.rpc.Protocol is not adaptive method!"));
        }

        assertEquals("ok2", echoProxy.echo("ok2"));
    }
}