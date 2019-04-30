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
package org.apache.dubbo.config.spring.beans.factory.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link YamlPropertySourceFactory} Test
 *
 * @since 2.6.5
 */
@RunWith(SpringRunner.class)
@PropertySource(name = "yaml-source", value = {"classpath:/META-INF/dubbo.yml"}, factory = YamlPropertySourceFactory.class)
@Configuration
@ContextConfiguration(classes = YamlPropertySourceFactoryTest.class)
public class YamlPropertySourceFactoryTest {

    @Autowired
    private Environment environment;

    @Value("${dubbo.consumer.default}")
    private Boolean isDefault;

    @Value("${dubbo.consumer.client}")
    private String client;

    @Value("${dubbo.consumer.threadpool}")
    private String threadPool;

    @Value("${dubbo.consumer.corethreads}")
    private Integer coreThreads;

    @Value("${dubbo.consumer.threads}")
    private Integer threads;

    @Value("${dubbo.consumer.queues}")
    private Integer queues;

    @Test
    public void testProperty() {
        Assert.assertEquals(isDefault, environment.getProperty("dubbo.consumer.default", Boolean.class));
        Assert.assertEquals(client, environment.getProperty("dubbo.consumer.client", String.class));
        Assert.assertEquals(threadPool, environment.getProperty("dubbo.consumer.threadpool", String.class));
        Assert.assertEquals(coreThreads, environment.getProperty("dubbo.consumer.corethreads", Integer.class));
        Assert.assertEquals(threads, environment.getProperty("dubbo.consumer.threads", Integer.class));
        Assert.assertEquals(queues, environment.getProperty("dubbo.consumer.queues", Integer.class));
    }
}
