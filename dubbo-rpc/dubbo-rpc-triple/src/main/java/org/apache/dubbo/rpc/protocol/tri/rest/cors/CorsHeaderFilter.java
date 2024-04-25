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
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpResultPayloadException;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.filter.RestHeaderFilterAdapter;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RequestMapping;

@Activate(group = CommonConstants.PROVIDER, order = 1000)
public class CorsHeaderFilter extends RestHeaderFilterAdapter {
    private final CorsProcessor corsProcessor;

    public CorsHeaderFilter(FrameworkModel frameworkModel) {
        corsProcessor = frameworkModel.getBeanFactory().getOrRegisterBean(CorsProcessor.class);
    }

    @Override
    protected RpcInvocation invoke(
            Invoker<?> invoker, RpcInvocation invocation, HttpRequest request, HttpResponse response)
            throws RpcException {
        RequestMapping mapping = request.attribute(RestConstants.MAPPING_ATTRIBUTE);
        processCors(mapping, request, response);
        return invocation;
    }

    private void processCors(RequestMapping mapping, HttpRequest request, HttpResponse response) {
        if (!corsProcessor.process(mapping.getCorsMeta(), request, response)) {
            throw new HttpResultPayloadException(HttpResult.builder()
                    .status(HttpStatus.FORBIDDEN)
                    .body(response.body())
                    .headers(response.headers())
                    .build());
        }
    }
}
