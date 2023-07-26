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
package org.apache.dubbo.remoting.http.jetty;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeErrorTypeAwareLogger;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;

import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.util.log.Log;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JettyLoggerAdapterTest {

    @Test
    void testJettyUseDubboLogger() throws Exception{
        int port = NetUtils.getAvailablePort();
        URL url = new ServiceConfigURL("http", "localhost", port,
            new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        HttpServer httpServer = new JettyHttpServer(url, new HttpHandler<HttpServletRequest,HttpServletResponse>() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.getWriter().write("Jetty is using Dubbo's logger");
            }
        });
        Request.Get(url.toJavaURL().toURI()).execute().returnContent().asString();

        assertThat(Log.getLog().getClass().isAssignableFrom(JettyLoggerAdapter.class), is(true));

        httpServer.close();
    }


    @Test
    void testSuccessLogger() throws Exception{
        Logger successLogger = mock(Logger.class);
        Class<?> clazz = Class.forName("org.apache.dubbo.remoting.http.jetty.JettyLoggerAdapter");
        JettyLoggerAdapter jettyLoggerAdapter = (JettyLoggerAdapter) clazz.getDeclaredConstructor().newInstance();

        Field loggerField = clazz.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(jettyLoggerAdapter, new FailsafeErrorTypeAwareLogger(successLogger));
        jettyLoggerAdapter.setDebugEnabled(true);

        when(successLogger.isDebugEnabled()).thenReturn(true);
        when(successLogger.isWarnEnabled()).thenReturn(true);
        when(successLogger.isInfoEnabled()).thenReturn(true);

        jettyLoggerAdapter.warn("warn");
        jettyLoggerAdapter.info("info");
        jettyLoggerAdapter.debug("debug");

        verify(successLogger).warn(anyString());
        verify(successLogger).info(anyString());
        verify(successLogger).debug(anyString());

        jettyLoggerAdapter.warn(new Exception("warn"));
        jettyLoggerAdapter.info(new Exception("info"));
        jettyLoggerAdapter.debug(new Exception("debug"));
        jettyLoggerAdapter.ignore(new Exception("ignore"));

        jettyLoggerAdapter.warn("warn", new Exception("warn"));
        jettyLoggerAdapter.info("info", new Exception("info"));
        jettyLoggerAdapter.debug("debug", new Exception("debug"));
    }


    @Test
    void testNewLogger(){
        JettyLoggerAdapter loggerAdapter = new JettyLoggerAdapter();
        org.eclipse.jetty.util.log.Logger logger = loggerAdapter.newLogger(this.getClass().getName());
        assertThat(logger.getClass().isAssignableFrom(JettyLoggerAdapter.class), is(true));
    }


    @Test
    void testDebugEnabled(){
        JettyLoggerAdapter loggerAdapter = new JettyLoggerAdapter();
        loggerAdapter.setDebugEnabled(true);
        assertThat(loggerAdapter.isDebugEnabled(), is(true));
    }


    @Test
    void testLoggerFormat() throws Exception{
        Class<?> clazz = Class.forName("org.apache.dubbo.remoting.http.jetty.JettyLoggerAdapter");
        Object newInstance = clazz.getDeclaredConstructor().newInstance();

        Method method = clazz.getDeclaredMethod("format", String.class, Object[].class);
        method.setAccessible(true);

        String print = (String) method.invoke(newInstance, "Hello,{}! I'am {}", new  String[]{"World","Jetty"});

        assertThat(print, is("Hello,World! I'am Jetty"));
    }
}
