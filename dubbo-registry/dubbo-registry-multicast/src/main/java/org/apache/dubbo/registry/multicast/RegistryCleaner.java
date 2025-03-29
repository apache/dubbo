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
package org.apache.dubbo.registry.multicast;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class RegistryCleaner {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RegistryCleaner.class);
    private final MulticastRegistry registry;
    private final ConcurrentMap<URL, Set<URL>> received;

    public RegistryCleaner(MulticastRegistry registry, ConcurrentMap<URL, Set<URL>> received) {
        this.registry = registry;
        this.received = received;
    }

    public void clean() {
        for (Set<URL> providers : received.values()) {
            for (URL url : providers) {
                if (isExpired(url)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Clean expired provider " + url);
                    }
                    registry.doUnregister(url);
                }
            }
        }
    }

    private boolean isExpired(URL url) {
        if (!url.getParameter("dynamic", true) || url.getPort() <= 0) {
            return false;
        }
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
