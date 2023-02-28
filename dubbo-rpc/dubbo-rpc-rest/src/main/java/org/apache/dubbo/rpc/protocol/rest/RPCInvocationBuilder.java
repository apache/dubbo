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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;


import java.util.Arrays;
import java.util.Enumeration;


public class RPCInvocationBuilder {


    private static final ParamParserManager paramParser = new ParamParserManager();


    public static Pair<RpcInvocation, Invoker> build(RequestFacade request, Object servletRequest, Object servletResponse) {

        Pair<Invoker, RestMethodMetadata> invokerRestMethodMetadataPair = getRestMethodMetadata(request);

        RpcInvocation rpcInvocation = createBaseRpcInvocation(request, invokerRestMethodMetadataPair.getSecond());

        ProviderParseContext parseContext = createParseContext(request, servletRequest, servletResponse, invokerRestMethodMetadataPair.getSecond());

        Object[] args = paramParser.providerParamParse(parseContext);

        rpcInvocation.setArguments(args);

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


        int localPort = request.getLocalPort();
        String localAddr = request.getLocalAddr();
        int remotePort = request.getRemotePort();
        String remoteAddr = request.getRemoteAddr();

        String HOST = request.getHeader(RestConstant.HOST);
        String GROUP = request.getHeader(RestConstant.GROUP);

        String PATH = request.getHeader(RestConstant.PATH);
        String VERSION = request.getHeader(RestConstant.VERSION);

        String METHOD = restMethodMetadata.getMethod().getName();
        String[] PARAMETER_TYPES_DESC = restMethodMetadata.getMethod().getParameterTypes();

        rpcInvocation.setParameterTypes(restMethodMetadata.getReflectMethod().getParameterTypes());


        rpcInvocation.setMethodName(METHOD);
        rpcInvocation.setAttachment(RestConstant.GROUP, GROUP);
        rpcInvocation.setAttachment(RestConstant.METHOD, METHOD);
        rpcInvocation.setAttachment(RestConstant.PARAMETER_TYPES_DESC, PARAMETER_TYPES_DESC);
        rpcInvocation.setAttachment(RestConstant.PATH, PATH);
        rpcInvocation.setAttachment(RestConstant.VERSION, VERSION);
        rpcInvocation.setAttachment(RestConstant.HOST, HOST);
        rpcInvocation.setAttachment(RestConstant.REMOTE_ADDR, remoteAddr);
        rpcInvocation.setAttachment(RestConstant.LOCAL_ADDR, localAddr);
        rpcInvocation.setAttachment(RestConstant.REMOTE_PORT, remotePort);
        rpcInvocation.setAttachment(RestConstant.LOCAL_PORT, localPort);

        Enumeration<String> attachments = request.getHeaders(RestConstant.DUBBO_ATTACHMENT_HEADER);

        while (attachments != null && attachments.hasMoreElements()) {
            String header = attachments.nextElement();
            int index = header.indexOf("=");
            if (index > 0) {
                String key = header.substring(0, index);
                String value = header.substring(index + 1);
                if (!StringUtils.isEmpty(key)) {
                    RpcContext.getServerAttachment().setAttachment(key.trim(), value.trim());
                }
            }
        }


        // TODO set path,version,group and so on
        return rpcInvocation;
    }


    private static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(RequestFacade request) {
        String path = request.getPathInfo();
        String version = request.getHeader(RestConstant.VERSION);
        String group = request.getHeader(RestConstant.GROUP);
        int port = request.getIntHeader(RestConstant.REST_PORT);

        return PathAndInvokerMapper.getRestMethodMetadata(path, version, group, port);
    }


}
