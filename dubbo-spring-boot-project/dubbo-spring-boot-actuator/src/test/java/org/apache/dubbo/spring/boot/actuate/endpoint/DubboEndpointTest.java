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
package org.apache.dubbo.spring.boot.actuate.endpoint;

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.spring.boot.util.DubboUtils;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.apache.dubbo.common.Version.getVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link DubboQosEndpoints} Test
 *
 * @see DubboQosEndpoints
 * @since 2.7.0
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {DubboQosEndpoints.class},
        properties = {"dubbo.application.name = dubbo-demo-application"})
@EnableAutoConfiguration
class DubboEndpointTest {

    @Autowired
    private DubboQosEndpoints dubboQosEndpoints;

    @BeforeEach
    public void init() {
        DubboBootstrap.reset();
    }

    @AfterEach
    public void destroy() {
        DubboBootstrap.reset();
    }

    @Test
    void testInvoke() {

        Map<String, Object> metadata = dubboQosEndpoints.invoke();

        assertNotNull(metadata.get("timestamp"));

        Map<String, String> versions = (Map<String, String>) metadata.get("versions");
        Map<String, String> urls = (Map<String, String>) metadata.get("urls");

        assertFalse(versions.isEmpty());
        assertFalse(urls.isEmpty());

        assertEquals(getVersion(DubboUtils.class, "1.0.0"), versions.get("dubbo-spring-boot"));
        assertEquals(getVersion(), versions.get("dubbo"));

        assertEquals("https://github.com/apache/dubbo", urls.get("dubbo"));
        assertEquals("dev@dubbo.apache.org", urls.get("mailing-list"));
        assertEquals("https://github.com/apache/dubbo-spring-boot-project", urls.get("github"));
        assertEquals("https://github.com/apache/dubbo-spring-boot-project/issues", urls.get("issues"));
        assertEquals("https://github.com/apache/dubbo-spring-boot-project.git", urls.get("git"));
    }
}
