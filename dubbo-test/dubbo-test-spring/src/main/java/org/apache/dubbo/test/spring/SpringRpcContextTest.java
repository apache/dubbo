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
package org.apache.dubbo.test.spring;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.test.common.SysProps;
import org.apache.dubbo.test.common.api.AsyncContextService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.apache.dubbo.common.constants.CommonConstants.SHUTDOWN_WAIT_KEY;

public class SpringRpcContextTest {
    private static ClassPathXmlApplicationContext providerContext;

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
        providerContext.close();
    }

    private void startProvider() {
        providerContext = new ClassPathXmlApplicationContext("/spring/dubbo-demo-provider.xml");
    }

    @Test
    public void test() {
        SysProps.setProperty(SHUTDOWN_WAIT_KEY, "10000");
        startProvider();
        ClassPathXmlApplicationContext applicationContext = null;
        try {
            applicationContext = new ClassPathXmlApplicationContext("/spring/dubbo-demo.xml");
            AsyncContextService asyncContextService =
                    applicationContext.getBean("asyncContextService", AsyncContextService.class);

            AtomicBoolean contextCorrect = new AtomicBoolean(true);

            for (int i = 0; i < 5; i++) {
                CompletableFuture<String> serverSideContext = asyncContextService.getServerSideContext();
                serverSideContext.whenComplete((s, throwable) -> {
                    if (!"Hello, Dubbo"
                            .equals(RpcContext.getClientResponseContext().getAttachment("theToken"))) {
                        contextCorrect.set(false);
                    }
                    RpcContext.removeContext();
                });
                serverSideContext.get();
            }
            Assertions.assertTrue(contextCorrect.get());

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            SysProps.clear();
            if (applicationContext != null) {
                applicationContext.close();
            }
        }
    }
}
