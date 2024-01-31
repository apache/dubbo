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
package org.apache.dubbo.demo.rest.api.impl;

import org.apache.dubbo.demo.rest.api.HttpRequestAndResponseRPCContextService;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service("httpRequestAndResponseRPCContextService")
public class HttpRequestAndResponseRPCContextServiceImpl implements HttpRequestAndResponseRPCContextService {
    @Override
    public String httpRequestParam(String hello) {
        Object request = RpcContext.getServerAttachment().getRequest();
        return ((RequestFacade) request).getParameter("name");
    }

    @Override
    public String httpRequestHeader(String hello) {
        Object request = RpcContext.getServerAttachment().getRequest();
        return ((RequestFacade) request).getHeader("header");
    }

    @Override
    public List<String> httpResponseHeader(String hello) {
        Object response = RpcContext.getServerAttachment().getResponse();
        Map<String, List<String>> outputHeaders = ((NettyHttpResponse) response).getOutputHeaders();
        String responseKey = "response";
        outputHeaders.put(responseKey, Arrays.asList(hello));

        return outputHeaders.get(responseKey);
    }
}
