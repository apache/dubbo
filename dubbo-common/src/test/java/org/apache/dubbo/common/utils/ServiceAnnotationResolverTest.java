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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.config.annotation.DubboService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceAnnotationResolverTest {
    @Test
    public void test() {
        ServiceAnnotationResolver resolver = new ServiceAnnotationResolver(Demo.class);
        Assertions.assertEquals(resolver.getServiceType(), Demo.class);
        Assertions.assertNotNull(resolver.getServiceAnnotation());
        Assertions.assertTrue(resolver.getServiceAnnotation().annotationType() == DubboService.class);
        Assertions.assertEquals(resolver.resolveGroup(), "GroupA");
        Assertions.assertEquals(resolver.resolveVersion(), "1.0.0");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ServiceAnnotationResolver(Demo2.class));
    }

    @Test
    public void resolveInterfaceClassNameTest() {
        ServiceAnnotationResolver resolver3 = new ServiceAnnotationResolver(Demo3.class);
        Assertions.assertEquals(resolver3.resolveInterfaceClassName(), IDemo.class.getName());
    }

    interface IDemo {

    }

    @DubboService(version = "1.0.0", group = "GroupA")
    class Demo {

    }

    class Demo2 {

    }

    @DubboService
    class Demo3 implements IDemo {

    }
}
