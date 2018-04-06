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
package com.alibaba.dubbo.remoting.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * http invocation handler.
 *
 * HTTP 处理器接口
 */
public interface HttpHandler {

    /**
     * invoke.
     *
     * 处理器请求
     *
     * @param request  request. 请求
     * @param response response. 响应
     * @throws IOException 当 IO 发生异常
     * @throws ServletException 当 Servlet 发生异常
     */
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

}