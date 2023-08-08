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
package org.apache.dubbo.rpc.protocol.rest.filter;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.RestRPCInvocationUtil;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandlerResult;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestFilterContext;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestInterceptContext;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.pair.InvokerAndRestMethodMetadataPair;
import org.apache.dubbo.rpc.protocol.rest.pair.MessageCodecResultPair;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.util.List;

@Activate(value = "invoke", order = Integer.MAX_VALUE)
public class ServiceInvokeRestFilter implements RestRequestFilter {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    private final List<RestResponseInterceptor> restResponseInterceptors;

    public ServiceInvokeRestFilter(FrameworkModel frameworkModel) {
        restResponseInterceptors = frameworkModel.getExtensionLoader(RestResponseInterceptor.class).getActivateExtensions();
    }

    @Override
    public void filter(RestFilterContext restFilterContext) throws Exception {
        NettyRequestFacade nettyRequestFacade = (NettyRequestFacade) restFilterContext.getRequestFacade();

        FullHttpRequest nettyHttpRequest = nettyRequestFacade.getRequest();

        doHandler(nettyHttpRequest, restFilterContext.getResponse(), restFilterContext.getRequestFacade(), restFilterContext.getUrl(), restFilterContext.getServiceDeployer());

    }


    private void doHandler(HttpRequest nettyHttpRequest,
                           NettyHttpResponse nettyHttpResponse,
                           RequestFacade request,
                           URL url,
                           ServiceDeployer serviceDeployer) throws Exception {
        //  acquire metadata by request
        InvokerAndRestMethodMetadataPair restMethodMetadataPair = RestRPCInvocationUtil.getRestMethodMetadataAndInvokerPair(request);

        // path NoFound 404
        if (restMethodMetadataPair == null) {
            throw new PathNoFoundException("rest service Path no found, current path info:" + RestRPCInvocationUtil.createPathMatcher(request));
        }

        Invoker invoker = restMethodMetadataPair.getInvoker();

        RestMethodMetadata restMethodMetadata = restMethodMetadataPair.getRestMethodMetadata();

        // method disallowed
        if (!restMethodMetadata.getRequest().methodAllowed(request.getMethod())) {
            nettyHttpResponse.sendError(405, "service require request method is : "
                + restMethodMetadata.getRequest().getMethod()
                + ", but current request method is: " + request.getMethod()
            );
            return;
        }


        // content-type  support judge,throw unSupportException
        acceptSupportJudge(request, restMethodMetadata.getReflectMethod().getReturnType());

        // build RpcInvocation
        RpcInvocation rpcInvocation = RestRPCInvocationUtil.createBaseRpcInvocation(request, restMethodMetadata);

        // parse method real args
        RestRPCInvocationUtil.parseMethodArgs(rpcInvocation, request, nettyHttpRequest, nettyHttpResponse, restMethodMetadata);

        // execute business  method invoke
        Result result = invoker.invoke(rpcInvocation);

        // set raw response
        nettyHttpResponse.setResponseBody(result.getValue());

        if (result.hasException()) {
            Throwable exception = result.getException();
            logger.error("", exception.getMessage(), "", "dubbo rest protocol provider Invoker invoke error", exception);

            if (serviceDeployer.getExceptionMapper().hasExceptionMapper(exception)) {
                ExceptionHandlerResult exceptionToResult = serviceDeployer.getExceptionMapper().exceptionToResult(result.getException());
                writeResult(nettyHttpResponse, request, url, exceptionToResult.getEntity(), rpcInvocation.getReturnType());
                nettyHttpResponse.setStatus(exceptionToResult.getStatus());
            } else {
                nettyHttpResponse.sendError(500,
                    "\n dubbo rest business exception, error cause is: "
                        + result.getException().getCause()
                        + "\n message is: " + result.getException().getMessage()
                        + "\n stacktrace is: " + stackTraceToString(exception));
            }
        }

        try {
            // invoke the intercept chain before Result  write to  response
            executeResponseIntercepts(url, request, nettyHttpResponse, result.getValue(), rpcInvocation, serviceDeployer);
        } catch (Exception exception) {
            logger.error("", exception.getMessage(), "", "dubbo rest protocol execute ResponseIntercepts error", exception);
            throw exception;
        }
    }


    /**
     * write return value by accept
     *
     * @param nettyHttpResponse
     * @param request
     * @param value
     * @param returnType
     * @throws Exception
     */
    public static void writeResult(NettyHttpResponse nettyHttpResponse, RequestFacade<?> request, URL url, Object value, Class<?> returnType) throws Exception {
        MediaType mediaType = getAcceptMediaType(request, returnType);
        writeResult(nettyHttpResponse, url, value, returnType, mediaType);
    }


    public static void writeResult(NettyHttpResponse nettyHttpResponse, URL url, Object value, Class<?> returnType, MediaType mediaType) throws Exception {
        MessageCodecResultPair booleanMediaTypePair = HttpMessageCodecManager.httpMessageEncode(nettyHttpResponse.getOutputStream(), value, url, mediaType, returnType);
        // reset raw response result
        nettyHttpResponse.setResponseBody(value);
        nettyHttpResponse.addOutputHeaders(RestHeaderEnum.CONTENT_TYPE.getHeader(), booleanMediaTypePair.getMediaType().value);
    }

    /**
     * return first match , if any multiple content-type
     *
     * @param request
     * @return
     */
    public static MediaType getAcceptMediaType(RequestFacade request, Class<?> returnType) {
        String accept = request.getHeader(RestHeaderEnum.ACCEPT.getHeader());
        MediaType mediaType = MediaTypeUtil.convertMediaType(returnType, accept);
        return mediaType;
    }

    /**
     * accept can not support will throw UnSupportAcceptException
     *
     * @param requestFacade
     */
    private void acceptSupportJudge(RequestFacade requestFacade, Class<?> returnType) {
        try {
            // media type judge
            getAcceptMediaType(requestFacade, returnType);
        } catch (UnSupportContentTypeException e) {
            // return type judge
            MediaType mediaType = HttpMessageCodecManager.typeSupport(returnType);

            String accept = requestFacade.getHeader(RestHeaderEnum.ACCEPT.getHeader());
            if (mediaType == null || accept == null) {
                throw e;
            }


            if (!accept.contains(mediaType.value)) {

                throw e;
            }

        }
    }


    public static String stackTraceToString(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();

        StringBuilder stringBuilder = new StringBuilder("\n");
        for (StackTraceElement traceElement : stackTrace) {
            stringBuilder.append("\tat " + traceElement).append("\n");
        }

        return stringBuilder.toString();
    }

    /**
     * execute response Intercepts
     *
     * @param url
     * @param request
     * @param nettyHttpResponse
     * @param result
     * @param rpcInvocation
     * @param serviceDeployer
     * @throws Exception
     */
    public void executeResponseIntercepts(URL url, RequestFacade request, NettyHttpResponse nettyHttpResponse, Object result, RpcInvocation rpcInvocation, ServiceDeployer serviceDeployer) throws Exception {

        RestInterceptContext restFilterContext = new RestInterceptContext(url, request, nettyHttpResponse, serviceDeployer, result, rpcInvocation);

        for (RestResponseInterceptor restResponseInterceptor : restResponseInterceptors) {

            restResponseInterceptor.intercept(restFilterContext);

            if (restFilterContext.complete()) {
                break;
            }

        }

    }
}
