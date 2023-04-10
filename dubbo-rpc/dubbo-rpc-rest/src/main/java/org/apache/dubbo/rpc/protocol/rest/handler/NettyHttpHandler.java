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

import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.PathAndInvokerMapper;
import org.apache.dubbo.rpc.protocol.rest.RestRPCInvocationUtil;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.exception.MediaTypeUnSupportException;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.pair.InvokerAndRestMethodMetadataPair;
import org.apache.dubbo.rpc.protocol.rest.pair.MessageCodecResultPair;
import org.apache.dubbo.rpc.protocol.rest.request.NettyRequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.io.IOException;

/**
 * netty http request handler
 */
public class NettyHttpHandler implements HttpHandler<NettyRequestFacade, NettyHttpResponse> {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());
    private final PathAndInvokerMapper pathAndInvokerMapper;
    private final ExceptionMapper exceptionMapper;


    public NettyHttpHandler(PathAndInvokerMapper pathAndInvokerMapper, ExceptionMapper exceptionMapper) {
        this.pathAndInvokerMapper = pathAndInvokerMapper;
        this.exceptionMapper = exceptionMapper;
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

        FullHttpRequest nettyHttpRequest = requestFacade.getRequest();

        try {
            doHandler(nettyHttpRequest, nettyHttpResponse, requestFacade);
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

    private void doHandler(FullHttpRequest nettyHttpRequest, NettyHttpResponse nettyHttpResponse, RequestFacade request) throws Exception {
        //  acquire metadata by request
        InvokerAndRestMethodMetadataPair restMethodMetadataPair = RestRPCInvocationUtil.getRestMethodMetadata(request, pathAndInvokerMapper);

        Invoker invoker = restMethodMetadataPair.getInvoker();

        RestMethodMetadata restMethodMetadata = restMethodMetadataPair.getRestMethodMetadata();

        // content-type  support judge,throw unSupportException
        acceptSupportJudge(request, restMethodMetadata.getReflectMethod().getReturnType());

        // build RpcInvocation
        RpcInvocation rpcInvocation = RestRPCInvocationUtil.createBaseRpcInvocation(request, restMethodMetadata);

        // parse method real args
        RestRPCInvocationUtil.parseMethodArgs(rpcInvocation, request, nettyHttpRequest, nettyHttpResponse, restMethodMetadata);

        // execute business  method invoke
        Result result = invoker.invoke(rpcInvocation);

        if (result.hasException()) {
            Throwable exception = result.getException();
            logger.error("", exception.getMessage(), "", "dubbo rest protocol provider Invoker invoke error", exception);

            if (exceptionMapper.hasExceptionMapper(exception)) {
                writeResult(nettyHttpResponse, request, invoker, exceptionMapper.exceptionToResult(result.getException()), rpcInvocation.getReturnType());
                nettyHttpResponse.setStatus(200);
            } else {
                nettyHttpResponse.sendError(500,
                    "\n dubbo rest business exception, error cause is: "
                        + result.getException().getCause()
                        + "\n message is: " + result.getException().getMessage()
                        + "\n stacktrace is: " + stackTraceToString(exception));
            }
        } else {
            Object value = result.getValue();
            writeResult(nettyHttpResponse, request, invoker, value, rpcInvocation.getReturnType());
            nettyHttpResponse.setStatus(200);
        }
    }


    /**
     * write return value by accept
     *
     * @param nettyHttpResponse
     * @param request
     * @param invoker
     * @param value
     * @param returnType
     * @throws Exception
     */
    private void writeResult(NettyHttpResponse nettyHttpResponse, RequestFacade request, Invoker invoker, Object value, Class returnType) throws Exception {
        MediaType mediaType = getAcceptMediaType(request,returnType);

        MessageCodecResultPair booleanMediaTypePair = HttpMessageCodecManager.httpMessageEncode(nettyHttpResponse.getOutputStream(), value, invoker.getUrl(), mediaType, returnType);

        nettyHttpResponse.addOutputHeaders(RestHeaderEnum.CONTENT_TYPE.getHeader(), booleanMediaTypePair.getMediaType().value);
    }

    /**
     * return first match , if any multiple content-type
     *
     * @param request
     * @return
     */
    private MediaType getAcceptMediaType(RequestFacade request,Class<?> returnType) {
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
            getAcceptMediaType(requestFacade,returnType);
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

}
