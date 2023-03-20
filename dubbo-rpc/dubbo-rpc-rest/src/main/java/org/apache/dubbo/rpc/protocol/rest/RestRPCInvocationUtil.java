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
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.HttpHeaderUtil;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;


import java.util.Arrays;


public class RestRPCInvocationUtil {


    public static void parseMethodArgs(RpcInvocation rpcInvocation, RequestFacade request, Object servletRequest,
                                       Object servletResponse,
                                       RestMethodMetadata restMethodMetadata) {

        try {
            ProviderParseContext parseContext = createParseContext(request, servletRequest, servletResponse, restMethodMetadata);
            Object[] args = ParamParserManager.providerParamParse(parseContext);
            rpcInvocation.setArguments(args);
        } catch (Exception e) {
            throw new ParamParseException(e.getMessage());
        }
    }

    private static ProviderParseContext createParseContext(RequestFacade request, Object servletRequest, Object servletResponse, RestMethodMetadata restMethodMetadata) {
        ProviderParseContext parseContext = new ProviderParseContext(request);
        parseContext.setResponse(servletResponse);
        parseContext.setRequest(servletRequest);

        Object[] objects = new Object[restMethodMetadata.getArgInfos().size()];
        parseContext.setArgs(Arrays.asList(objects));
        parseContext.setArgInfos(restMethodMetadata.getArgInfos());


        return parseContext;
    }

    public static RpcInvocation createBaseRpcInvocation(RequestFacade request, RestMethodMetadata restMethodMetadata) {
        RpcInvocation rpcInvocation = new RpcInvocation();

        String method = restMethodMetadata.getMethod().getName();
        String[] parameterTypes = restMethodMetadata.getMethod().getParameterTypes();

        rpcInvocation.setParameterTypes(restMethodMetadata.getReflectMethod().getParameterTypes());
        rpcInvocation.setReturnType(restMethodMetadata.getReflectMethod().getReturnType());
        rpcInvocation.setMethodName(method);
        rpcInvocation.put(RestConstant.PARAMETER_TYPES_DESC, parameterTypes);
        rpcInvocation.put(RestConstant.METHOD, method);


        // TODO set   protocolServiceKey
//

        HttpHeaderUtil.parseRequest(rpcInvocation, request);

        String serviceKey = BaseServiceMetadata.buildServiceKey(request.getPathInfo(),
            request.getHeader(RestHeaderEnum.GROUP.getHeader()),
            request.getHeader(RestHeaderEnum.VERSION.getHeader()));
        rpcInvocation.setTargetServiceUniqueName(serviceKey);

        return rpcInvocation;
    }


    public static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(RequestFacade request, PathAndInvokerMapper pathAndInvokerMapper) {
        String path = request.getPathInfo();
        String version = request.getHeader(RestHeaderEnum.VERSION.getHeader());
        String group = request.getHeader(RestHeaderEnum.GROUP.getHeader());

        return pathAndInvokerMapper.getRestMethodMetadata(path, version, group, null);
    }


}
