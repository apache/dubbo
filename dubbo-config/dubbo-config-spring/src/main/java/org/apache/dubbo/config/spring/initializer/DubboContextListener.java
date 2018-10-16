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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * A Dubbo context listener is a delegation to org.springframework.web.context.ContextLoaderListener. This is necessary,
 * because Dubbo is packaged into all-in-one jar, therefore it contains a web-fragment.xml from this sub module which's
 * used for helping to assemble spring context listener automatically when it's not configured explicitly by user in
 * web.xml. It works fine with spring, but it will lead to ClassNotFound exception and fail tomcat's bootup when user
 * doesn't depend on spring framework.
 */
public class DubboContextListener implements ServletContextListener {
    private static final Log logger = LogFactory.getLog(DubboContextListener.class);

    private static final String SPRING_CONTEXT_LISTENER = "org.springframework.web.context.ContextLoaderListener";
    private static final String SPRING_CONTEXT_ROOT = "org.springframework.web.context.WebApplicationContext.ROOT";

    private ServletContextListener springContextListener;
    private boolean executed = false;

    public DubboContextListener() {
        try {
            Class c = Class.forName(SPRING_CONTEXT_LISTENER);
            springContextListener = (ServletContextListener) c.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.warn("Servlet container detects dubbo's web fragment configuration, and tries to load " +
                    "org.springframework.web.context.ContextLoaderListener but fails to find the class. " +
                    "If the application don't rely on Spring framework, pls. simply ignore");
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (springContextListener != null) {
            // if spring context listener has already been registered, then do nothing
            ServletContext context = servletContextEvent.getServletContext();
            if (context.getAttribute(SPRING_CONTEXT_ROOT) == null) {
                executed = true;
                springContextListener.contextInitialized(servletContextEvent);
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (springContextListener != null && executed) {
            springContextListener.contextDestroyed(servletContextEvent);
        }
    }
}
