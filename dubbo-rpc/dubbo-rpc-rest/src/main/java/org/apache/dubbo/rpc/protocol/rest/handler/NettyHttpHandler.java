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
package org.apache.dubbo.rpc.protocol.rest.handler;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.PathAndInvokerMapper;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.exception.MediaTypeUnSupportException;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.filter.RestFilter;
import org.apache.dubbo.rpc.protocol.rest.filter.RestFilterChain;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_DEPLOYER_ATTRIBUTE_KEY;
import static org.apache.dubbo.rpc.protocol.rest.filter.ServiceInvokeRestFilter.stackTraceToString;

/**
 * netty http request handler
 */
public class NettyHttpHandler implements HttpHandler<NettyRequestFacade, NettyHttpResponse> {
    private static final List<RestFilter> restFilters = new ArrayList(FrameworkModel.defaultModel().getExtensionLoader(RestFilter.class).getSupportedExtensionInstances());
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    private final ServiceDeployer serviceDeployer;
    private final PathAndInvokerMapper pathAndInvokerMapper;
    private final ExceptionMapper exceptionMapper;
    private final URL url;

    public NettyHttpHandler(ServiceDeployer serviceDeployer, URL url) {
        this.serviceDeployer = serviceDeployer;
        this.pathAndInvokerMapper = serviceDeployer.getPathAndInvokerMapper();
        this.exceptionMapper = serviceDeployer.getExceptionMapper();
        this.url = url;

    }

    @Override
    public void handle(NettyRequestFacade requestFacade, NettyHttpResponse nettyHttpResponse) throws IOException {

        // set remote address
        RpcContext.getServiceContext().setRemoteAddress(requestFacade.getRemoteAddr(), requestFacade.getRemotePort());

        // set local address
        RpcContext.getServiceContext().setLocalAddress(requestFacade.getLocalAddr(), requestFacade.getLocalPort());

        // set request
        RpcContext.getServiceContext().setRequest(requestFacade);

        // set response
        RpcContext.getServiceContext().setResponse(nettyHttpResponse);
        // TODO add request filter chain
        Object nettyHttpRequest = requestFacade.getRequest();

        RpcContext.getServerAttachment().setObjectAttachment(SERVICE_DEPLOYER_ATTRIBUTE_KEY, serviceDeployer);


        try {
            new RestFilter() {
                @Override
                public void filter(URL url, RequestFacade requestFacade, NettyHttpResponse response, RestFilterChain restFilterChain) throws Exception {

                    restFilterChain.filter(url, requestFacade, response, restFilterChain);
                }
            }.filter(url, requestFacade, nettyHttpResponse, new RestFilterChain(restFilters));

        } catch (PathNoFoundException pathNoFoundException) {
            logger.error("", pathNoFoundException.getMessage(), "", "dubbo rest protocol provider path   no found ,raw request is :" + nettyHttpRequest, pathNoFoundException);
            nettyHttpResponse.sendError(404, pathNoFoundException.getMessage());
        } catch (ParamParseException paramParseException) {
            logger.error("", paramParseException.getMessage(), "", "dubbo rest protocol provider param parse error ,and raw request is :" + nettyHttpRequest, paramParseException);
            nettyHttpResponse.sendError(400, paramParseException.getMessage());
        } catch (MediaTypeUnSupportException contentTypeException) {
            logger.error("", contentTypeException.getMessage(), "", "dubbo rest protocol provider content-type un support" + nettyHttpRequest, contentTypeException);
            nettyHttpResponse.sendError(415, contentTypeException.getMessage());
        } catch (Throwable throwable) {
            logger.error("", throwable.getMessage(), "", "dubbo rest protocol provider error ,and raw request is  " + nettyHttpRequest, throwable);
            nettyHttpResponse.sendError(500, "dubbo rest invoke Internal error, message is " + throwable.getMessage()
                + " , stacktrace is: " + stackTraceToString(throwable));
        }


    }

}
