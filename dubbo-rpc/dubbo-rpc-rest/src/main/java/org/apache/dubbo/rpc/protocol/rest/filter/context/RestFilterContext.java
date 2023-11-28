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
package org.apache.dubbo.rpc.protocol.rest.filter.context;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

public class RestFilterContext implements FilterContext {
    protected URL url;
    protected RequestFacade requestFacade;
    protected NettyHttpResponse response;
    protected ServiceDeployer serviceDeployer;
    protected boolean completed;
    protected Object originRequest;
    protected Object originResponse;

    public RestFilterContext(
            URL url, RequestFacade requestFacade, NettyHttpResponse response, ServiceDeployer serviceDeployer) {
        this.url = url;
        this.requestFacade = requestFacade;
        this.response = response;
        this.serviceDeployer = serviceDeployer;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public RequestFacade getRequestFacade() {
        return requestFacade;
    }

    @Override
    public NettyHttpResponse getResponse() {
        return response;
    }

    @Override
    public ServiceDeployer getServiceDeployer() {
        return serviceDeployer;
    }

    @Override
    public boolean complete() {
        return completed;
    }

    @Override
    public void setComplete(boolean complete) {
        this.completed = complete;
    }

    @Override
    public Object getOriginRequest() {
        return originRequest;
    }

    @Override
    public Object getOriginResponse() {
        return originResponse;
    }

    public void setOriginRequest(Object originRequest) {
        if (this.originRequest != null) {
            return;
        }
        this.originRequest = originRequest;
    }

    public void setOriginResponse(Object originResponse) {
        this.originResponse = originResponse;
    }
}
