/**
 * Project: dubbo-container
 * 
 * File Created at 2011-10-28
 * $Id$
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
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
 * TODO Comment of JettyContainer
 * 
 * @author william.liangf
 */
@Extension("jetty")
public class JettyContainer implements Container {

    private static final Logger logger             = LoggerFactory.getLogger(JettyContainer.class);

    private static final String JETTY_PORT_KEY     = "jetty.port";

    private static final int    DEFAULT_JETTY_PORT = 8080;

    private SelectChannelConnector connector;

    public void start() {
        String serverPort = System.getProperty(JETTY_PORT_KEY);
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
