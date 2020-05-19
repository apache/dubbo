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
package org.apache.dubbo.config.spring.schema;

import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = GenericServiceTest.class)
@ImportResource(locations = "classpath:/META-INF/spring/dubbo-generic-consumer.xml")
public class GenericServiceTest {

    @Before
    public void setUp() {
        ApplicationModel.reset();
    }

    @After
    public void tearDown() {
        ApplicationModel.reset();
    }

    @Autowired
    @Qualifier("demoServiceRef")
    private ReferenceBean referenceBean;

    @Autowired
    @Qualifier("demoService")
    private ServiceBean serviceBean;

    @Test
    public void testBeanDefinitionParser() {
        assertNotNull(referenceBean);
        assertNotNull(serviceBean);
    }
}
