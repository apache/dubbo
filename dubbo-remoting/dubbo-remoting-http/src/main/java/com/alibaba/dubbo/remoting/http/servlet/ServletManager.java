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
package com.alibaba.dubbo.remoting.http.servlet;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO this may not be a pretty elegant solution,
 * 这可能不是一个非常优雅的解决方案。
 *
 * Servlet 管理器
 */
public class ServletManager {

    /**
     * 外部服务器端口，用于 `servlet` 的服务器端口
     */
    public static final int EXTERNAL_SERVER_PORT = -1234;

    /**
     * 单例
     */
    private static final ServletManager instance = new ServletManager();
    /**
     * ServletContext 集合
     */
    private final Map<Integer, ServletContext> contextMap = new ConcurrentHashMap<Integer, ServletContext>();

    public static ServletManager getInstance() {
        return instance;
    }

    /**
     * 添加 ServletContext 对象
     *
     * @param port 服务器端口
     * @param servletContext ServletContext 对象
     */
    public void addServletContext(int port, ServletContext servletContext) {
        contextMap.put(port, servletContext);
    }

    /**
     * 移除 ServletContext 对象
     *
     * @param port 服务器端口
     */
    public void removeServletContext(int port) {
        contextMap.remove(port);
    }

    /**
     * 获得 ServletContext 对象
     *
     * @param port 服务器端口
     * @return ServletContext 对象
     */
    public ServletContext getServletContext(int port) {
        return contextMap.get(port);
    }

}
