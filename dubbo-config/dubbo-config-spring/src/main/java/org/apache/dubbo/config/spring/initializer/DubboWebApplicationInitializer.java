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

import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * An initializer to register {@link DubboApplicationListener}
 * to the ApplicationContext seamlessly.
 */
public class DubboWebApplicationInitializer extends AbstractContextLoaderInitializer {

    /**
     * This method won't be triggered if running on spring-boot.
     * It only works when running under a servlet container.
     * @return a WebApplicationContext with DubboApplicationListener registered.
     */
    @Override
    protected WebApplicationContext createRootApplicationContext() {
        XmlWebApplicationContext webApplicationContext = new XmlWebApplicationContext();
        webApplicationContext.addApplicationListener(new DubboApplicationListener());
        return webApplicationContext;
    }
}
