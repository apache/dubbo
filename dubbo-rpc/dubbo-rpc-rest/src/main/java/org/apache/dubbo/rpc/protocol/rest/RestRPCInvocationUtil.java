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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.BaseServiceMetadata;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.pair.InvokerAndRestMethodMetadataPair;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.HttpHeaderUtil;


import java.util.Arrays;
import java.util.List;


public class RestRPCInvocationUtil {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RestRPCInvocationUtil.class);

    /**
     * service method real args parse
     *
     * @param rpcInvocation
     * @param request
     * @param servletRequest
     * @param servletResponse
     * @param restMethodMetadata
     */
    public static void parseMethodArgs(RpcInvocation rpcInvocation, RequestFacade request, Object servletRequest,
                                       Object servletResponse,
                                       RestMethodMetadata restMethodMetadata) {

        try {
            ProviderParseContext parseContext = createParseContext(request, servletRequest, servletResponse, restMethodMetadata);
            Object[] args = ParamParserManager.providerParamParse(parseContext);

            List<ArgInfo> argInfos = parseContext.getArgInfos();

            for (ArgInfo argInfo : argInfos) {
                // TODO set default value
                if (argInfo.getParamType().isPrimitive() && args[argInfo.getIndex()] == null) {
                    throw new ParamParseException("\n dubbo provider primitive arg not exist in request, method is: "
                        + restMethodMetadata.getReflectMethod() + "\n type is: " +
                        argInfo.getParamType() + " \n and arg index is: " + argInfo.getIndex());
                }
            }

            rpcInvocation.setArguments(args);
        } catch (Exception e) {
            logger.error("", e.getMessage(), "", "dubbo rest provider method args parse error: ", e);
            throw new ParamParseException(e.getMessage());
        }
    }

    /**
     * create parseMethodArgs context
     *
     * @param request
     * @param servletRequest
     * @param servletResponse
     * @param restMethodMetadata
     * @return
     */
    private static ProviderParseContext createParseContext(RequestFacade request, Object servletRequest, Object servletResponse, RestMethodMetadata restMethodMetadata) {
        ProviderParseContext parseContext = new ProviderParseContext(request);
        parseContext.setResponse(servletResponse);
        parseContext.setRequest(servletRequest);

        Object[] objects = new Object[restMethodMetadata.getArgInfos().size()];
        parseContext.setArgs(Arrays.asList(objects));
        parseContext.setArgInfos(restMethodMetadata.getArgInfos());


        return parseContext;
    }

    /**
     * build RpcInvocation
     *
     * @param request
     * @param restMethodMetadata
     * @return
     */
    public static RpcInvocation createBaseRpcInvocation(RequestFacade request, RestMethodMetadata restMethodMetadata) {
        RpcInvocation rpcInvocation = new RpcInvocation();

        rpcInvocation.setParameterTypes(restMethodMetadata.getReflectMethod().getParameterTypes());
        rpcInvocation.setReturnType(restMethodMetadata.getReflectMethod().getReturnType());
        rpcInvocation.setMethodName(restMethodMetadata.getMethod().getName());

        // TODO set   protocolServiceKey ,but no set method
//

        HttpHeaderUtil.parseRequest(rpcInvocation, request);

        String serviceKey = BaseServiceMetadata.buildServiceKey(request.getHeader(RestHeaderEnum.PATH.getHeader()),
            request.getHeader(RestHeaderEnum.GROUP.getHeader()),
            request.getHeader(RestHeaderEnum.VERSION.getHeader()));
        rpcInvocation.setTargetServiceUniqueName(serviceKey);

        return rpcInvocation;
    }


    /**
     * get  path mapping
     *
     * @param request
     * @param pathAndInvokerMapper
     * @return
     */
    public static InvokerAndRestMethodMetadataPair getRestMethodMetadata(RequestFacade request, PathAndInvokerMapper pathAndInvokerMapper) {
        String path = request.getPath();
        String version = request.getHeader(RestHeaderEnum.VERSION.getHeader());
        String group = request.getHeader(RestHeaderEnum.GROUP.getHeader());

        return pathAndInvokerMapper.getRestMethodMetadata(path, version, group, null);
    }


}
