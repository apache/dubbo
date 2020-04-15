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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RemoteMetadataServiceProxy} Test
 *
 * @since 2.7.5
 */
public class RemoteMetadataServiceProxyTest {

    @Test
    public void testGetServiceDefinition() {
        String serviceKey = "a/org.apache.dubbo.demo.DemoService:1.0.0";

        String[] services = UrlUtils.parseServiceKey(serviceKey);
        String serviceInterface = services[1];
        // if version or group is not exist
        String group = null;
        if (StringUtils.isNotEmpty(services[0])) {
            group = services[0];
        }
        String version = null;
        if (StringUtils.isNotEmpty(services[2])) {
            version = services[2];
        }
        assertEquals(serviceInterface, "org.apache.dubbo.demo.DemoService");
        assertEquals(version, "1.0.0");
        assertEquals(group, "a");

        serviceKey = "org.apache.dubbo.demo.DemoService";

        services = UrlUtils.parseServiceKey(serviceKey);
        serviceInterface = services[1];
        // if version or group is not exist
        group = null;
        if (StringUtils.isNotEmpty(services[0])) {
            group = services[0];
        }
        version = null;
        if (StringUtils.isNotEmpty(services[2])) {
            version = services[2];
        }
        assertEquals(serviceInterface, "org.apache.dubbo.demo.DemoService");
        assertEquals(version, null);
        assertEquals(group, null);
    }
}
