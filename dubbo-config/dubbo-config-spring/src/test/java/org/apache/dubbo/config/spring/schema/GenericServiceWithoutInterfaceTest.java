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

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.service.GenericService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GenericServiceWithoutInterfaceTest.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@ImportResource(locations = "classpath:/META-INF/spring/dubbo-generic-consumer-without-interface.xml")
public class GenericServiceWithoutInterfaceTest {

    @BeforeAll
    public static void beforeAll() {
        DubboBootstrap.reset();
    }

    @AfterAll
    public static void afterAll() {
        DubboBootstrap.reset();
    }

    @Autowired
    @Qualifier("genericServiceWithoutInterfaceRef")
    private GenericService genericServiceWithoutInterfaceRef;

    @Test
    public void testGenericWithoutInterface() {

        // Test generic service without interface class locally
        Object result = genericServiceWithoutInterfaceRef.$invoke("sayHello", new String[]{"java.lang.String"}, new Object[]{"generic"});
        Assertions.assertEquals("Welcome generic", result);

        ReferenceConfigBase<Object> reference = DubboBootstrap.getInstance().getApplicationModel().getDefaultModule().getConfigManager().getReference("genericServiceWithoutInterfaceRef");
        Assertions.assertNull(reference.getServiceInterfaceClass());
        Assertions.assertEquals("org.apache.dubbo.config.spring.api.LocalMissClass", reference.getInterface());
        Assertions.assertThrows(ClassNotFoundException.class, () -> ClassUtils.forName(reference.getInterface()));
    }
}
