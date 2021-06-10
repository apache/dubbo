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
package org.apache.dubbo.spring.boot;

import org.apache.dubbo.spring.boot.autoconfigure.CompatibleDubboAutoConfigurationTest;
import org.apache.dubbo.spring.boot.autoconfigure.CompatibleDubboAutoConfigurationTestWithoutProperties;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfigurationOnMultipleConfigTest;
import org.apache.dubbo.spring.boot.autoconfigure.DubboAutoConfigurationOnSingleConfigTest;
import org.apache.dubbo.spring.boot.autoconfigure.RelaxedDubboConfigBinderTest;
import org.apache.dubbo.spring.boot.context.event.AwaitingNonWebApplicationListenerTest;
import org.apache.dubbo.spring.boot.context.event.DubboConfigBeanDefinitionConflictApplicationListenerTest;
import org.apache.dubbo.spring.boot.context.event.WelcomeLogoApplicationListenerTest;
import org.apache.dubbo.spring.boot.env.DubboDefaultPropertiesEnvironmentPostProcessorTest;
import org.apache.dubbo.spring.boot.util.DubboUtilsTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CompatibleDubboAutoConfigurationTest.class,
        CompatibleDubboAutoConfigurationTestWithoutProperties.class,
        DubboAutoConfigurationOnMultipleConfigTest.class,
        DubboAutoConfigurationOnSingleConfigTest.class,
        RelaxedDubboConfigBinderTest.class,
        AwaitingNonWebApplicationListenerTest.class,
        DubboConfigBeanDefinitionConflictApplicationListenerTest.class,
        WelcomeLogoApplicationListenerTest.class,
        DubboDefaultPropertiesEnvironmentPostProcessorTest.class,
        DubboUtilsTest.class,
})
public class TestSuite {
}
