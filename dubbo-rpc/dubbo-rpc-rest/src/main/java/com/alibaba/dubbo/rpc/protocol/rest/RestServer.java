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
package com.alibaba.dubbo.rpc.protocol.rest;

import com.alibaba.dubbo.common.URL;

/**
 * Rest Server 接口
 */
public interface RestServer {

    /**
     * 启动服务器
     *
     * @param url URL 对象
     */
    void start(URL url);

    /**
     * 停止服务器
     */
    void stop();

    /**
     * 部署服务
     *
     * @param resourceDef 服务类
     * @param resourceInstance 服务对象
     * @param contextPath ContextPath
     */
    void deploy(Class resourceDef, Object resourceInstance, String contextPath);

    /**
     * 取消服务
     *
     * @param resourceDef 服务类
     */
    void undeploy(Class resourceDef);

}
