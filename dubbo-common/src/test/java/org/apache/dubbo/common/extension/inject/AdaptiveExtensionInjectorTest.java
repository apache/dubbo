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
package org.apache.dubbo.common.extension.inject;

import org.apache.dubbo.common.beans.ScopeBeanExtensionInjector;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionInjector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.director.FooFrameworkProvider;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link AdaptiveExtensionInjector}
 * {@link ScopeBeanExtensionInjector}
 * {@link SpiExtensionInjector}
 */
public class AdaptiveExtensionInjectorTest {

    @Test
    public void test() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ExtensionLoader<ExtensionInjector> extensionLoader = frameworkModel.getExtensionLoader(ExtensionInjector.class);

        ExtensionInjector adaptiveExtensionInjector = extensionLoader.getAdaptiveExtension();
        ExtensionInjector scopeExtensionInjector = extensionLoader.getExtension(ScopeBeanExtensionInjector.NAME);
        ExtensionInjector spiExtensionInjector = extensionLoader.getExtension(SpiExtensionInjector.NAME);

        FooFrameworkProvider testFrameworkProvider = adaptiveExtensionInjector.getInstance(FooFrameworkProvider.class, "testFrameworkProvider");
        Assertions.assertNotNull(testFrameworkProvider);
        Assertions.assertTrue(testFrameworkProvider.getClass().getName().endsWith("$Adaptive"));
        Assertions.assertEquals(spiExtensionInjector.getInstance(FooFrameworkProvider.class, "testFrameworkProvider"), testFrameworkProvider);

        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();
        AdaptiveExtensionInjectorTest obj = new AdaptiveExtensionInjectorTest();
        beanFactory.registerBean("bean", obj);
        AdaptiveExtensionInjectorTest bean = adaptiveExtensionInjector.getInstance(AdaptiveExtensionInjectorTest.class, "bean");
        Assertions.assertEquals(bean, obj);
        Assertions.assertEquals(scopeExtensionInjector.getInstance(AdaptiveExtensionInjectorTest.class, "bean"), bean);

        frameworkModel.destroy();
    }
}
