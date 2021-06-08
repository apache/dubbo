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

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.boot.util.DubboUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.apache.dubbo.common.Version.getVersion;

/**
 * {@link DubboMetadataEndpoint} Test
 *
 * @see DubboMetadataEndpoint
 * @since 2.7.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                DubboMetadataEndpoint.class
        },
        properties = {
                "dubbo.application.name = dubbo-demo-application"
        }
)
@EnableAutoConfiguration
public class DubboEndpointTest {


    @Autowired
    private DubboMetadataEndpoint dubboEndpoint;

    @Before
    public void init() {
        ApplicationModel.reset();
    }

    @After
    public void destroy() {
        ApplicationModel.reset();
    }

    @Test
    public void testInvoke() {

        Map<String, Object> metadata = dubboEndpoint.invoke();

        Assert.assertNotNull(metadata.get("timestamp"));

        Map<String, String> versions = (Map<String, String>) metadata.get("versions");
        Map<String, String> urls = (Map<String, String>) metadata.get("urls");

        Assert.assertFalse(versions.isEmpty());
        Assert.assertFalse(urls.isEmpty());

        Assert.assertEquals(getVersion(DubboUtils.class, "1.0.0"), versions.get("dubbo-spring-boot"));
        Assert.assertEquals(getVersion(), versions.get("dubbo"));

        Assert.assertEquals("https://github.com/apache/dubbo", urls.get("dubbo"));
        Assert.assertEquals("dev@dubbo.apache.org", urls.get("mailing-list"));
        Assert.assertEquals("https://github.com/apache/dubbo-spring-boot-project", urls.get("github"));
        Assert.assertEquals("https://github.com/apache/dubbo-spring-boot-project/issues", urls.get("issues"));
        Assert.assertEquals("https://github.com/apache/dubbo-spring-boot-project.git", urls.get("git"));

    }


}
