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
package org.apache.dubbo.metrics.metrics.model;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.metrics.model.key.MetricsLevel;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;

class MethodMetricTest {

    private static ApplicationModel applicationModel;
    private static String interfaceName;
    private static String methodName;
    private static String group;
    private static String version;
    private static RpcInvocation invocation;

    @BeforeAll
    public static void setup() {

        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockMetrics");
        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);

        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        group = "mockGroup";
        version = "1.0.0";
        invocation = new RpcInvocation(methodName, interfaceName, "serviceKey", null, null);

        invocation.setTargetServiceUniqueName(group + "/" + interfaceName + ":" + version);
        invocation.setAttachment(GROUP_KEY, group);
        invocation.setAttachment(VERSION_KEY, version);
        RpcContext.getServiceContext()
                .setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
    }

    @Test
    void test() {
        MethodMetric metric =
                new MethodMetric(applicationModel, invocation, MethodMetric.isServiceLevel(applicationModel));
        Assertions.assertEquals(metric.getServiceKey(), interfaceName);
        Assertions.assertEquals(metric.getMethodName(), methodName);
        Assertions.assertEquals(metric.getGroup(), group);
        Assertions.assertEquals(metric.getVersion(), version);

        Map<String, String> tags = metric.getTags();
        Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
    }

    @Test
    void testServiceMetrics() {
        MetricsConfig metricConfig = new MetricsConfig();
        applicationModel.getApplicationConfigManager().setMetrics(metricConfig);
        metricConfig.setRpcLevel(MetricsLevel.SERVICE.name());
        MethodMetric metric =
                new MethodMetric(applicationModel, invocation, MethodMetric.isServiceLevel(applicationModel));
        Assertions.assertEquals(metric.getServiceKey(), interfaceName);
        Assertions.assertNull(metric.getMethodName(), methodName);
        Assertions.assertEquals(metric.getGroup(), group);
        Assertions.assertEquals(metric.getVersion(), version);

        Map<String, String> tags = metric.getTags();
        Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationModel.getApplicationName());

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
        Assertions.assertNull(tags.get(TAG_METHOD_KEY));
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
    }
}
