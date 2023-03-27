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
package org.apache.dubbo.configcenter.support.nacos;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import static com.alibaba.nacos.client.constant.Constants.HealthCheck.DOWN;
import static com.alibaba.nacos.client.constant.Constants.HealthCheck.UP;
import static org.mockito.ArgumentMatchers.any;

class RetryTest {
    
    private static ApplicationModel applicationModel = ApplicationModel.defaultModel();

    @Test
    void testRetryCreate() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            ConfigService mock = new MockConfigService() {
                @Override
                public String getServerStatus() {
                    return atomicInteger.incrementAndGet() > 10 ? UP : DOWN;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createConfigService((Properties) any())).thenReturn(mock);


            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter("nacos.retry", 5)
                .addParameter("nacos.retry-wait", 10);
            Assertions.assertThrows(IllegalStateException.class, () -> new NacosDynamicConfiguration(url, applicationModel));

            try {
                new NacosDynamicConfiguration(url, applicationModel);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }

    @Test
    void testDisable() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            ConfigService mock = new MockConfigService() {
                @Override
                public String getServerStatus() {
                    return DOWN;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createConfigService((Properties) any())).thenReturn(mock);


            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter("nacos.retry", 5)
                .addParameter("nacos.retry-wait", 10)
                .addParameter("nacos.check", "false");
            try {
                new NacosDynamicConfiguration(url, applicationModel);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }

    @Test
    void testRequest() {
        try (MockedStatic<NacosFactory> nacosFactoryMockedStatic = Mockito.mockStatic(NacosFactory.class)) {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            ConfigService mock = new MockConfigService() {
                @Override
                public String getConfig(String dataId, String group, long timeoutMs) throws NacosException {
                    if (atomicInteger.incrementAndGet() > 10) {
                        return "";
                    } else {
                        throw new NacosException();
                    }
                }

                @Override
                public String getServerStatus() {
                    return UP;
                }
            };
            nacosFactoryMockedStatic.when(() -> NacosFactory.createConfigService((Properties) any())).thenReturn(mock);


            URL url = URL.valueOf("nacos://127.0.0.1:8848")
                .addParameter("nacos.retry", 5)
                .addParameter("nacos.retry-wait", 10);
            Assertions.assertThrows(IllegalStateException.class, () -> new NacosDynamicConfiguration(url, applicationModel));

            try {
                new NacosDynamicConfiguration(url, applicationModel);
            } catch (Throwable t) {
                Assertions.fail(t);
            }
        }
    }
}
