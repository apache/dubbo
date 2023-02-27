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
import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_METHOD_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_VERSION_KEY;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

class MethodMetricTest {

    private static final String applicationName = null;
    private static String interfaceName;
    private static String methodName;
    private static String group;
    private static String version;
    private static RpcInvocation invocation;

    @BeforeAll
    public static void setup() {
        interfaceName = "org.apache.dubbo.MockInterface";
        methodName = "mockMethod";
        group = "mockGroup";
        version = "1.0.0";
        invocation = new RpcInvocation(methodName, interfaceName, "serviceKey", null, null);

        invocation.setTargetServiceUniqueName(group + "/" + interfaceName + ":" + version);
        invocation.setAttachment(GROUP_KEY, group);
        invocation.setAttachment(VERSION_KEY, version);
        RpcContext.getServiceContext().setUrl(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&side=consumer"));
    }

    @Test
    void test() {
        MethodMetric metric = new MethodMetric(applicationName, invocation);
        Assertions.assertEquals(metric.getInterfaceName(), interfaceName);
        Assertions.assertEquals(metric.getMethodName(), methodName);
        Assertions.assertEquals(metric.getGroup(), group);
        Assertions.assertEquals(metric.getVersion(), version);

        Map<String, String> tags = metric.getTags();
        Assertions.assertEquals(tags.get(TAG_IP), getLocalHost());
        Assertions.assertEquals(tags.get(TAG_HOSTNAME), getLocalHostName());
        Assertions.assertEquals(tags.get(TAG_APPLICATION_NAME), applicationName);

        Assertions.assertEquals(tags.get(TAG_INTERFACE_KEY), interfaceName);
        Assertions.assertEquals(tags.get(TAG_METHOD_KEY), methodName);
        Assertions.assertEquals(tags.get(TAG_GROUP_KEY), group);
        Assertions.assertEquals(tags.get(TAG_VERSION_KEY), version);
    }
}
