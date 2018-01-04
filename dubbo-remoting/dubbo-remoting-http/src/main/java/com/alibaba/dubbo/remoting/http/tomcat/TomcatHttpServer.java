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
package com.alibaba.dubbo.remoting.http.tomcat;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class TomcatHttpServer extends AbstractHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(TomcatHttpServer.class);

    private final Tomcat tomcat;

    private final URL url;

    public TomcatHttpServer(URL url, final HttpHandler handler) {
        super(url, handler);

        this.url = url;
        DispatcherServlet.addHttpHandler(url.getPort(), handler);
        String baseDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        tomcat = new Tomcat();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(url.getPort());
        tomcat.getConnector().setProperty(
                "maxThreads", String.valueOf(url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS)));
//        tomcat.getConnector().setProperty(
//                "minSpareThreads", String.valueOf(url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS)));

        tomcat.getConnector().setProperty(
                "maxConnections", String.valueOf(url.getParameter(Constants.ACCEPTS_KEY, -1)));

        tomcat.getConnector().setProperty("URIEncoding", "UTF-8");
        tomcat.getConnector().setProperty("connectionTimeout", "60000");

        tomcat.getConnector().setProperty("maxKeepAliveRequests", "-1");
        tomcat.getConnector().setProtocol("org.apache.coyote.http11.Http11NioProtocol");

        Context context = tomcat.addContext("/", baseDir);
        Tomcat.addServlet(context, "dispatcher", new DispatcherServlet());
        context.addServletMapping("/*", "dispatcher");
        ServletManager.getInstance().addServletContext(url.getPort(), context.getServletContext());

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            throw new IllegalStateException("Failed to start tomcat server at " + url.getAddress(), e);
        }
    }

    public void close() {
        super.close();

        ServletManager.getInstance().removeServletContext(url.getPort());

        try {
            tomcat.stop();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }
}