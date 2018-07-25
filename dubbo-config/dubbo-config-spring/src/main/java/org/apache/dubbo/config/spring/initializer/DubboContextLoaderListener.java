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

import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.servlet.ServletContext;

/**
 * This class is introduced mainly for compatibility.
 * Consider the following case:
 * 1. listener class {@link ContextLoaderListener} is defined in web.xml
 *    servlet container will initialized {@link ContextLoaderListener} before this class.
 *    Therefore Dubbo's elegant shutdown feature will not be enabled.
 *    To enable it, you need to either:
 *    1) remove listener class {@link ContextLoaderListener} in your web.xml
 *    2) change listener class to {@link DubboContextLoaderListener} in your web.xml
 * 2. listener class {@link DubboContextLoaderListener} is defined in web.xml
 *    This is automatically enable Dubbo's elegant shutdown feature,
 *    even for a servlet container which is not 3.0 compatible
 * 3. no listener class defined in web.xml
 *    Dubbo's elegant shutdown feature will be automatically enabled
 *    if Dubbo is running under a servlet 3.0+ compatible container
 *    see {@link DubboWebApplicationInitializer} for more details.
 */
public class DubboContextLoaderListener extends ContextLoaderListener {

    /**
     * The root WebApplicationContext instance that this loader manages.
     */
    private WebApplicationContext context;

    /**
     * This is used for xml configuration, which does not require servlet version to be 3.0+
     */
    public DubboContextLoaderListener(){
        this.context = new XmlWebApplicationContext();
        ((XmlWebApplicationContext)context).addApplicationListener(new DubboApplicationListener());
    }

    /**
     * This is used for programmatic API, which requires servlet version to be 3.0+
     * @param context the web application context
     */
    public DubboContextLoaderListener(WebApplicationContext context) {
        this.context = context;
    }

    @Override
    public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
        if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
            // if the root web application has already been created, just ignore.
            return this.context;
        }
        return super.initWebApplicationContext(servletContext);
    }
}
