/**
 * Copyright 1999-2014 dangdang.com.
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
package com.alibaba.dubbo.rpc.protocol.rest;

import com.alibaba.dubbo.common.URL;
import org.jboss.resteasy.plugins.server.sun.http.SunHttpJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * @author lishen
 */
public class SunHttpServer extends BaseRestServer {

    private final SunHttpJaxrsServer server = new SunHttpJaxrsServer();

    protected void doStart(URL url) {
        server.setPort(url.getPort());
        server.start();
    }

    public void stop() {
        server.stop();
    }

    protected ResteasyDeployment getDeployment() {
        return server.getDeployment();
    }
}
