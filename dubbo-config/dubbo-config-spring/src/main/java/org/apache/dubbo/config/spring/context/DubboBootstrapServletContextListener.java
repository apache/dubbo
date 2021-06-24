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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.config.bootstrap.BootstrapTakeoverMode;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * DubboBootstrap lifecycle controller for Servlet container.
 *
 * @see DubboBootstrapApplicationListener
 */
@WebListener
public class DubboBootstrapServletContextListener implements ServletContextListener {

    public DubboBootstrapServletContextListener() {
        // Set takeover mode when servlet container is starting
        if (DubboBootstrap.getInstance().getTakeoverMode() == BootstrapTakeoverMode.AUTO) {
            DubboBootstrap.getInstance().setTakeoverMode(BootstrapTakeoverMode.SERVLET);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        // If takeover mode is not changed on servlet container is initialized, it means dubbo does not running in Spring context.
        // Otherwise, the takeover mode will be changed if dubbo is loaded by Spring.
        if (DubboBootstrap.getInstance().getTakeoverMode() == BootstrapTakeoverMode.SERVLET) {
            DubboBootstrap.getInstance().start();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (DubboBootstrap.getInstance().getTakeoverMode() == BootstrapTakeoverMode.SERVLET) {
            DubboShutdownHook.getDubboShutdownHook().run();
        }
    }

}
