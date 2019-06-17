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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.common.utils.NetUtils;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * {@link DubboBootstrap} Test
 *
 * @since 2.7.3
 */
public class DubboBootstrapTest {

    private static int zkServerPort = NetUtils.getAvailablePort();

    private static TestingServer zkServer;


    @BeforeAll
    public static void init() throws Exception {
        zkServer = new TestingServer(zkServerPort, true);
    }

    @AfterAll
    public static void destroy() throws IOException {
        zkServer.stop();
        zkServer.close();
    }

    @Test
    public void testProviderInFluentAPI() {

        new DubboBootstrap()
                .application("dubbo-provider-demo")
                .next()
                .registry()
                .address("zookeeper://127.0.0.1:" + zkServerPort + "?registry-type=service")
                .next()
                .protocol()
                .name("dubbo")
                .port(-1)
                .next()
                .service("test")
                .interfaceClass(EchoService.class)
                .ref(new EchoServiceImpl())
                .group("DEFAULT")
                .version("1.0.0")
                .next()
                .start()
                .stop();

    }

    @Test
    public void testProviderInLambda() {
        new DubboBootstrap()
                .application("dubbo-provider-demo", builder -> {
                })
                .registry("default", builder ->
                        builder.address("zookeeper://127.0.0.1:" + zkServerPort + "?registry-type=service")
                )
                .protocol("defalt", builder ->
                        builder.name("dubbo")
                                .port(-1)
                )
                .service("test", builder ->
                        builder.interfaceClass(EchoService.class)
                                .ref(new EchoServiceImpl())
                                .group("DEFAULT")
                                .version("1.0.0")
                )
                .start()
                .stop();
    }
}
