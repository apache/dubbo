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
package org.apache.dubbo.config.spring.initializer;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Automatically register {@link DubboApplicationListener} to Spring context
 * A {@link org.springframework.web.context.ContextLoaderListener} class is defined in
 * src/main/resources/META-INF/web-fragment.xml
 * In the web-fragment.xml, {@link DubboApplicationContextInitializer} is defined in context params.
 * This file will be discovered if running under a servlet 3.0+ container.
 * Even if user specifies {@link org.springframework.web.context.ContextLoaderListener} in web.xml,
 * it will be merged to web.xml.
 * If user specifies <metadata-complete="true" /> in web.xml, this will no take effect,
 * unless user configures {@link DubboApplicationContextInitializer} explicitly in web.xml.
 */
public class DubboApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.addApplicationListener(new DubboApplicationListener());
    }
}
