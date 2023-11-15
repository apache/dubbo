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
package org.apache.dubbo.rpc.protocol.rest.pu;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.api.AbstractWireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelHandlerPretender;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.netty.RestHttpRequestDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import static org.apache.dubbo.common.constants.CommonConstants.REST_SERVICE_DEPLOYER_URL_ATTRIBUTE_KEY;

@Activate(order = Integer.MAX_VALUE)
public class RestHttp1WireProtocol extends AbstractWireProtocol implements ScopeModelAware {

    private static final ServiceDeployer emptyServiceDeployer = new ServiceDeployer();

    public RestHttp1WireProtocol(FrameworkModel frameworkModel) {
        super(new RestHttp1Detector(frameworkModel));
    }

    @Override
    public void configServerProtocolHandler(URL url, ChannelOperator operator) {

        // h1 upgrade to  don`t response 101 ,cause there is no h1 stream handler
        // TODO add h1 stream handler

        // pathAndInvokerMapper, exceptionMapper getFrom url
        ServiceDeployer serviceDeployer = (ServiceDeployer) url.getAttribute(REST_SERVICE_DEPLOYER_URL_ATTRIBUTE_KEY);

        // maybe current request is qos http or no rest service export
        if (serviceDeployer == null) {
            serviceDeployer = emptyServiceDeployer;
        }

        List<Object> channelHandlers = Arrays.asList(
                new HttpRequestDecoder(
                        url.getParameter(
                                RestConstant.MAX_INITIAL_LINE_LENGTH_PARAM, RestConstant.MAX_INITIAL_LINE_LENGTH),
                        url.getParameter(RestConstant.MAX_HEADER_SIZE_PARAM, RestConstant.MAX_HEADER_SIZE),
                        url.getParameter(RestConstant.MAX_CHUNK_SIZE_PARAM, RestConstant.MAX_CHUNK_SIZE)),
                new HttpObjectAggregator(
                        url.getParameter(RestConstant.MAX_REQUEST_SIZE_PARAM, RestConstant.MAX_REQUEST_SIZE)),
                new HttpResponseEncoder(),
                new RestHttpRequestDecoder(url, serviceDeployer));

        List<ChannelHandler> handlers = new ArrayList<>();
        handlers.add(new ChannelHandlerPretender(channelHandlers));
        operator.configChannelHandler(handlers);
    }
}
