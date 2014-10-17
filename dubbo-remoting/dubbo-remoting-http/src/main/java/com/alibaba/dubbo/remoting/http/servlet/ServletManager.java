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
package com.alibaba.dubbo.remoting.http.servlet;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO this may not be a pretty elegant solution,
 * and we may need to make change to the whole remoting-http architecture in the future
 *
 * @author lishen
 */
public class ServletManager {

    public static final int EXTERNAL_SERVER_PORT = -1234;

    private static final ServletManager instance = new ServletManager();

    private final Map<Integer, ServletContext> contextMap = new ConcurrentHashMap<Integer, ServletContext>();

    public static ServletManager getInstance() {
        return instance;
    }

    public void addServletContext(int port, ServletContext servletContext) {
        contextMap.put(port, servletContext);
    }

    public void removeServletContext(int port) {
        contextMap.remove(port);
    }

    public ServletContext getServletContext(int port) {
        return contextMap.get(port);
    }
}
