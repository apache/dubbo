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


public class RPCInvocationBuilder {


    private static final ParamParserManager paramParser = new ParamParserManager();


    public static Pair<RpcInvocation, Invoker> build(RequestFacade request, Object servletRequest, Object servletResponse) {

        Pair<Invoker, RestMethodMetadata> invokerRestMethodMetadataPair = getRestMethodMetadata(request);

        RpcInvocation rpcInvocation = createBaseRpcInvocation(request, invokerRestMethodMetadataPair.getSecond());

        ProviderParseContext parseContext = createParseContext(request, servletRequest, servletResponse, invokerRestMethodMetadataPair.getSecond());

        try {
            Object[] args = paramParser.providerParamParse(parseContext);

            rpcInvocation.setArguments(args);
        } catch (Exception paramParseException) {
            throw new ParamParseException(paramParseException.getMessage());
        }

        return Pair.make(rpcInvocation, invokerRestMethodMetadataPair.getFirst());

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

    private static RpcInvocation createBaseRpcInvocation(RequestFacade request, RestMethodMetadata restMethodMetadata) {
        RpcInvocation rpcInvocation = new RpcInvocation();

        String method = restMethodMetadata.getMethod().getName();
        String[] parameterTypes = restMethodMetadata.getMethod().getParameterTypes();

        rpcInvocation.setParameterTypes(restMethodMetadata.getReflectMethod().getParameterTypes());
        rpcInvocation.setReturnType(restMethodMetadata.getReflectMethod().getReturnType());
        rpcInvocation.setMethodName(method);
        rpcInvocation.put(RestConstant.PARAMETER_TYPES_DESC, parameterTypes);
        rpcInvocation.put(RestConstant.METHOD, method);


        // TODO set setTargetServiceUniqueName  protocolServiceKey
//        rpcInvocation.setTargetServiceUniqueName();
//        rpcInvocation.protocolServiceKey();

        HttpHeaderUtil.parseRequest(rpcInvocation, request);


        return rpcInvocation;
    }


    private static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(RequestFacade request) {
        String path = request.getPathInfo();
        String version = request.getHeader(RestHeaderEnum.VERSION.getHeader());
        String group = request.getHeader(RestHeaderEnum.GROUP.getHeader());

        return PathAndInvokerMapper.getRestMethodMetadata(path, version, group, null);
    }


}
