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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.rpc.Constants;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ServletExchanger {

    private static final AtomicReference<URL> url = new AtomicReference<>();
    private static final AtomicReference<Integer> serverPort = new AtomicReference<>();

    private static boolean ENABLED = false;

    public static void init(Configuration configuration) {
        ENABLED = configuration.getBoolean(Constants.H2_SETTINGS_SERVLET_ENABLED, false);
    }

    private ServletExchanger() {}

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void bind(URL url) {
        ServletExchanger.url.compareAndSet(null, url);
    }

    public static void bindServerPort(int serverPort) {
        ServletExchanger.serverPort.compareAndSet(null, serverPort);
    }

    public static URL getUrl() {
        return Objects.requireNonNull(url.get(), "ServletExchanger not bound to triple protocol");
    }

    public static Integer getServerPort() {
        return serverPort.get();
    }
}
