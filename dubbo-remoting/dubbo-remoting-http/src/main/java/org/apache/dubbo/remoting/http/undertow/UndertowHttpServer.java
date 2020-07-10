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
package org.apache.dubbo.remoting.http.undertow;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.servlet.DispatcherServlet;
import org.apache.dubbo.remoting.http.servlet.ServletManager;
import org.apache.dubbo.remoting.http.support.AbstractHttpServer;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import javax.servlet.ServletException;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREADS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;

public class UndertowHttpServer extends AbstractHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(UndertowHttpServer.class);

    private static final String CONTEXT_PATH = "/";

    private static final String DEPLOYMENT_NAME = "dubbo";

    private static final String DEFAULT_SERVLET_NAME = "default";

    private final URL url;

    private Undertow server;

    public UndertowHttpServer(URL url, HttpHandler handler) {
        super(url, handler);
        this.url = url;
        DispatcherServlet.addHttpHandler(url.getParameter(Constants.BIND_PORT_KEY, url.getPort()), handler);
        int port = url.getParameter(Constants.BIND_PORT_KEY, url.getPort());
        int ioThreads = url.getParameter(IO_THREADS_KEY, Runtime.getRuntime().availableProcessors());
        int workThreads = url.getParameter(THREADS_KEY, DEFAULT_THREADS);
        String host = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
        try {
            DeploymentManager manager = createDeploymentManager();
            server = Undertow.builder()
                    .setIoThreads(ioThreads)
                    .setWorkerThreads(workThreads)
                    .addHttpListener(port, host)
                    .setHandler(manager.start())
                    .build();
            ServletManager.getInstance().addServletContext(port, manager.getDeployment().getServletContext());
            server.start();
        } catch (ServletException e) {
            e.printStackTrace();
        }
        logger.info("Undertow started on port: " + port);
    }

    @Override
    public void close() {
        super.close();
        ServletManager.getInstance().removeServletContext(url.getParameter(Constants.BIND_PORT_KEY, url.getPort()));
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    private DeploymentManager createDeploymentManager() {
        DeploymentInfo deployment = Servlets.deployment();
        deployment.setClassLoader(getClass().getClassLoader());
        deployment.setContextPath(CONTEXT_PATH);
        deployment.setDeploymentName(DEPLOYMENT_NAME);
        deployment.addServlet(Servlets.servlet(DEFAULT_SERVLET_NAME, DispatcherServlet.class));
        DeploymentManager manager = Servlets.newContainer().addDeployment(deployment);
        manager.deploy();
        return manager;
    }
}
