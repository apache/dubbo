/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.container.jetty;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.Container;
import com.alibaba.dubbo.container.page.PageServlet;

/**
 * JettyContainer
 * 
 * @author william.liangf
 */
@Extension("jetty")
public class JettyContainer implements Container {

    private static final Logger logger             = LoggerFactory.getLogger(JettyContainer.class);

    public static final String JETTY_PORT_KEY     = "jetty.port";

    public static final int    DEFAULT_JETTY_PORT = 8080;

    private SelectChannelConnector connector;

    public void start() {
        String serverPort = System.getProperty(JETTY_PORT_KEY, System.getProperty("dubbo.service.console.port")); // compatible
        int port;
        if (serverPort == null || serverPort.length() == 0) {
            port = DEFAULT_JETTY_PORT;
        } else {
            port = Integer.parseInt(serverPort);
        }
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        ServletHandler handler = new ServletHandler();
        ServletHolder holder = handler.addServletWithMapping(PageServlet.class, "/*");
        holder.setInitOrder(1);
        Server server = new Server();
        server.addConnector(connector);
        server.addHandler(handler);
        try {
            server.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start jetty server on " + NetUtils.getLocalHost() + ":" + port + ", cause: " + e.getMessage(), e);
        }
        logger.info("Dubbo jetty container started!");
    }

    public void stop() {
        try {
            if (connector != null) {
                connector.close();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("Dubbo jetty container stopped!");
    }

}