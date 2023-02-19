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

import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.annotation.ParamParserManager;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacadeFactory;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;


import java.util.ArrayList;
import java.util.List;


public class RPCInvocationBuilder {


    private static final ParamParserManager paramParser = new ParamParserManager();


    public static Pair<RpcInvocation, Invoker> build(Object servletRequest, Object response) {

        RequestFacade request = RequestFacadeFactory.createRequestFacade(servletRequest);

        Pair<Invoker, RestMethodMetadata> invokerRestMethodMetadataPair = getRestMethodMetadata(request);

        RpcInvocation rpcInvocation = createBaseRpcInvocation(request, invokerRestMethodMetadataPair);

        ProviderParseContext parseContext = createParseContext(request, response, rpcInvocation);

        Object[] args = paramParser.providerParamParse(parseContext);
        rpcInvocation.setArguments(args);

        return Pair.make(rpcInvocation, invokerRestMethodMetadataPair.getFirst());

    }

    private static ProviderParseContext createParseContext(Object request, Object response, RpcInvocation rpcInvocation) {
        ProviderParseContext parseContext = new ProviderParseContext(RequestFacadeFactory.createRequestFacade(request));
        parseContext.setResponse(response);
        parseContext.setRequest(request);

        // TODO create  List<ArgInfo> according to consumer method definition related header

        List<ArgInfo> argInfos = new ArrayList<>();

        //
        parseContext.setArgInfos(argInfos);


        return parseContext;
    }

    private static RpcInvocation createBaseRpcInvocation(RequestFacade request, Pair<Invoker, RestMethodMetadata> invokerRestMethodMetadataPair) {
        RpcInvocation rpcInvocation = new RpcInvocation();


        int localPort = request.getLocalPort();
        String localAddr = request.getLocalAddr();
        int remotePort = request.getRemotePort();
        String remoteAddr = request.getRemoteAddr();

        String HOST = request.getHeader(RestConstant.HOST);
        String GROUP = request.getHeader(RestConstant.GROUP);

        String PATH = request.getHeader(RestConstant.PATH);
        String VERSION = request.getHeader(RestConstant.VERSION);


        RestMethodMetadata serviceRestMetadata = invokerRestMethodMetadataPair.getSecond();
        String METHOD = serviceRestMetadata.getMethod().getName();
        String[] PARAMETER_TYPES_DESC = serviceRestMetadata.getMethod().getParameterTypes();


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
        // TODO set path,version,group and so on
        return rpcInvocation;
    }


    private static Pair<Invoker, RestMethodMetadata> getRestMethodMetadata(RequestFacade request) {
        int port = request.getLocalPort();
        String path = request.getRequestURI();
        String version = request.getHeader(RestConstant.VERSION);
        String group = request.getHeader(RestConstant.GROUP);

        return PathAndInvokerMapper.getRestMethodMetadata(path, version, group, port);
    }


}
