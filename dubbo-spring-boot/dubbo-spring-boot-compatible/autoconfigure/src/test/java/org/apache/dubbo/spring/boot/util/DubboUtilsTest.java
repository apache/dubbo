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

import org.junit.Assert;
import org.junit.Test;

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

/**
 * {@link DubboUtils} Test
 *
 * @see DubboUtils
 * @since 2.7.0
 */
public class DubboUtilsTest {

    @Test
    public void testConstants() {

        Assert.assertEquals("dubbo", DUBBO_PREFIX);

        Assert.assertEquals("dubbo.scan.", DUBBO_SCAN_PREFIX);

        Assert.assertEquals("base-packages", BASE_PACKAGES_PROPERTY_NAME);

        Assert.assertEquals("dubbo.config.", DUBBO_CONFIG_PREFIX);

        Assert.assertEquals("multiple", MULTIPLE_CONFIG_PROPERTY_NAME);

        Assert.assertEquals("dubbo.config.override", OVERRIDE_CONFIG_FULL_PROPERTY_NAME);

        Assert.assertEquals("https://github.com/apache/dubbo/tree/3.0/dubbo-spring-boot", DUBBO_SPRING_BOOT_GITHUB_URL);
        Assert.assertEquals("https://github.com/apache/dubbo.git", DUBBO_SPRING_BOOT_GIT_URL);
        Assert.assertEquals("https://github.com/apache/dubbo/issues", DUBBO_SPRING_BOOT_ISSUES_URL);

        Assert.assertEquals("https://github.com/apache/dubbo", DUBBO_GITHUB_URL);

        Assert.assertEquals("dev@dubbo.apache.org", DUBBO_MAILING_LIST);

        Assert.assertEquals("spring.application.name", SPRING_APPLICATION_NAME_PROPERTY);
        Assert.assertEquals("dubbo.application.id", DUBBO_APPLICATION_ID_PROPERTY);
        Assert.assertEquals("dubbo.application.name", DUBBO_APPLICATION_NAME_PROPERTY);
        Assert.assertEquals("dubbo.application.qos-enable", DUBBO_APPLICATION_QOS_ENABLE_PROPERTY);
        Assert.assertEquals("dubbo.config.multiple", DUBBO_CONFIG_MULTIPLE_PROPERTY);

        Assert.assertTrue(DEFAULT_MULTIPLE_CONFIG_PROPERTY_VALUE);

        Assert.assertTrue(DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE);


    }

}
