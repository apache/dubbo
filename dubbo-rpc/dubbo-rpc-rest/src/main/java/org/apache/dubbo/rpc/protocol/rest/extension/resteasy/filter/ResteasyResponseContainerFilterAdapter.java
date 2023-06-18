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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.rest.filter.RestResponseFilter;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.HttpResponse;

import javax.ws.rs.container.ContainerResponseFilter;
import java.util.Iterator;
import java.util.List;

@Activate(value = "resteasy", order = Integer.MAX_VALUE - 1000,onClass = "org.jboss.resteasy.specimpl.BuiltResponse,javax.ws.rs.container.ContainerResponseFilter,org.jboss.resteasy.spi.HttpResponse")
public class ResteasyResponseContainerFilterAdapter implements RestResponseFilter, ResteasyContext {
    @Override
    public void filter(URL url, RequestFacade requestFacade, NettyHttpResponse response, Iterator<RestFilter> restFilterIterator, ServiceDeployer serviceDeployer) throws Exception {


        List<ContainerResponseFilter> containerRequestFilters = getExtension(serviceDeployer, ContainerResponseFilter.class);

        if (containerRequestFilters.isEmpty()) {

            iteratorFilter(url, requestFacade, response, restFilterIterator, serviceDeployer);
            return;
        }


        // empty jaxrsResponse
        BuiltResponse jaxrsResponse = new BuiltResponse();
        // NettyHttpResponse wrapper
        HttpResponse httpResponse = new ResteasyNettyHttpResponse(response);
        DubboContainerResponseContextImpl containerResponseContext = createContainerResponseContext(requestFacade, httpResponse, jaxrsResponse, containerRequestFilters.toArray(new ContainerResponseFilter[0]));
        containerResponseContext.filter();
        addResponseHeaders(response,httpResponse.getOutputHeaders());


    }
}
