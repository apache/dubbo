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
package org.apache.dubbo.remoting.http.tomcat;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;

import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TomcatHttpBinderTest {
    @Test
    public void shouldAbleHandleRequestForTomcatBinder() throws Exception {
        int port = NetUtils.getAvailablePort();
        URL url = new URL("http", "localhost", port,
                new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});

        HttpServer httpServer = new TomcatHttpBinder().bind(url, new HttpHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
                response.getWriter().write("Tomcat");
            }
        });

        String response = Request.Get(url.toJavaURL().toURI()).execute().returnContent().asString();

        assertThat(response, is("Tomcat"));

        httpServer.close();
    }
}