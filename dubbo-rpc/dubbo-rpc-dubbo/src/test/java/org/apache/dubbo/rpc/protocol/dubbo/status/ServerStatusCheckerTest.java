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

package org.apache.dubbo.rpc.protocol.dubbo.status;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import org.apache.dubbo.rpc.protocol.dubbo.support.ProtocolUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ServerStatusCheckerTest {
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @AfterAll
    public static void after() {
        ApplicationModel.getServiceRepository().unregisterService(DemoService.class);
    }

    @BeforeAll
    public static void setup() {
        ApplicationModel.getServiceRepository().registerService(DemoService.class);
    }

    @Test
    public void testServerStatusChecker() throws Exception {
        int port = NetUtils.getAvailablePort(7000);
        URL url = URL.valueOf("dubbo://127.0.0.1:" + port + "/" + DemoService.class.getName());
        DemoService service = new DemoServiceImpl();

        DubboProtocol.getDubboProtocol().export(proxy.getInvoker(service, DemoService.class, url));

        StatusChecker server = ExtensionLoader.getExtensionLoader(StatusChecker.class).getExtension("server");
        Assertions.assertEquals(ServerStatusChecker.class, server.getClass());

        Status status = server.check();
        Assertions.assertEquals(Status.Level.OK, status.getLevel());

        ProtocolUtils.closeAll();
    }
}
