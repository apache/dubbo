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
package org.apache.dubbo.spring.boot.util;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_APPLICATION_ID_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_APPLICATION_NAME_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_APPLICATION_QOS_ENABLE_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_CONFIG_MULTIPLE_PROPERTY;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_CONFIG_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_MAILING_LIST;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SCAN_PREFIX;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_GIT_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_ISSUES_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.MULTIPLE_CONFIG_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.OVERRIDE_CONFIG_FULL_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.SPRING_APPLICATION_NAME_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DubboUtils} Test
 *
 * @see DubboUtils
 * @since 2.7.0
 */
class DubboUtilsTest {

    @Test
    void testConstants() {

        assertEquals("dubbo", DUBBO_PREFIX);

        assertEquals("dubbo.scan.", DUBBO_SCAN_PREFIX);

        assertEquals("base-packages", BASE_PACKAGES_PROPERTY_NAME);

        assertEquals("dubbo.config.", DUBBO_CONFIG_PREFIX);

        assertEquals("multiple", MULTIPLE_CONFIG_PROPERTY_NAME);

        assertEquals("dubbo.config.override", OVERRIDE_CONFIG_FULL_PROPERTY_NAME);

        assertEquals("https://github.com/apache/dubbo/tree/3.0/dubbo-spring-boot", DUBBO_SPRING_BOOT_GITHUB_URL);
        assertEquals("https://github.com/apache/dubbo.git", DUBBO_SPRING_BOOT_GIT_URL);
        assertEquals("https://github.com/apache/dubbo/issues", DUBBO_SPRING_BOOT_ISSUES_URL);

        assertEquals("https://github.com/apache/dubbo", DUBBO_GITHUB_URL);

        assertEquals("dev@dubbo.apache.org", DUBBO_MAILING_LIST);

        assertEquals("spring.application.name", SPRING_APPLICATION_NAME_PROPERTY);
        assertEquals("dubbo.application.id", DUBBO_APPLICATION_ID_PROPERTY);
        assertEquals("dubbo.application.name", DUBBO_APPLICATION_NAME_PROPERTY);
        assertEquals("dubbo.application.qos-enable", DUBBO_APPLICATION_QOS_ENABLE_PROPERTY);
        assertEquals("dubbo.config.multiple", DUBBO_CONFIG_MULTIPLE_PROPERTY);

        assertTrue(DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE);

        assertTrue(DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE);
    }
}
