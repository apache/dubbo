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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.PojoUtils;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.builders.ServiceBuilder;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * {@link DubboBootstrap} for Refresh Test
 * The refresh means restart with {@link DubboBootstrap#stop()} and {@link DubboBootstrap#start()}.
 *
 * @date 2025-06-13 08:30:25
 * @since 3.3
 */
class DubboBootstrapRefreshTest {

    private static final Supplier<DubboBootstrap> BOOTSTRAP_SUPPLIER = () -> {
        ServiceConfig<Object> serviceConfig = ServiceBuilder.newBuilder()
                .interfaceClass(DemoService.class)
                .ref(new DemoServiceImpl())
                .build();
        return DubboBootstrap.newInstance(FrameworkModel.defaultModel())
                .protocol(new ProtocolConfig(CommonConstants.DUBBO, 50051))
                .service(serviceConfig);
    };

    private static DubboBootstrap instance;

    @BeforeAll
    static void start() {
        // The test will fail because the static property of {@link PojoUtils#GENERIC_WITH_CLZ} created an
        // ApplicationModel instance,
        // but not destroy when DubboBootstrap stopped, and that caused DubboBootstrap not thoroughly destroyed!
        // This commit fixed this question.
        PojoUtils.isPojo(DubboBootstrap.class);

        instance = BOOTSTRAP_SUPPLIER.get();
        instance.start();
    }

    @AfterAll
    static void destroy() {
        instance.stop();
    }

    @Test
    void refreshTest() throws Exception {
        // destroy instance
        destroy();

        // restart new instance
        start();
    }

    private interface DemoService {
        String sayHello(String name);
    }

    private static class DemoServiceImpl implements DemoService {
        @Override
        public String sayHello(String name) {
            return "Hello " + name + ", response from provider.";
        }
    }
}
