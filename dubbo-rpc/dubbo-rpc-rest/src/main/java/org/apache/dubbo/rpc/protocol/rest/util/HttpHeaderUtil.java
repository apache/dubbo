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

package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHeaderUtil {


    /**
     * convert attachment to Map<String, List<String>>
     *
     * @param attachmentMap
     * @return
     */
    public static Map<String, List<String>> createAttachments(Map<String, Object> attachmentMap) {
        Map<String, List<String>> attachments = new HashMap<>();
        int size = 0;
        for (Map.Entry<String, Object> entry : attachmentMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());

            if (value != null) {
                size += value.getBytes(StandardCharsets.UTF_8).length;
            }

            List<String> strings = attachments.get(key);
            if (strings == null) {
                strings = new ArrayList<>();
                attachments.put(key, strings);
            }
            strings.add(value);
        }

        return attachments;
    }


    /**
     * add consumer attachment to request
     *
     * @param requestTemplate
     * @param attachmentMap
     */
    public static void addRequestAttachments(RequestTemplate requestTemplate, Map<String, Object> attachmentMap) {
        Map<String, List<String>> attachments = createAttachments(attachmentMap);

        attachments.entrySet().forEach(attachment -> {
            requestTemplate.addHeaders(appendPrefixToAttachRealHeader(attachment.getKey()), attachment.getValue());
        });

    }


    /**
     * add  provider attachment to response
     *
     * @param nettyHttpResponse
     */
    public static void addResponseAttachments(NettyHttpResponse nettyHttpResponse) {
        Map<String, List<String>> attachments = createAttachments(RpcContext.getServerContext().getObjectAttachments());

        attachments.entrySet().stream().forEach(attachment -> {
            nettyHttpResponse.getOutputHeaders().put(appendPrefixToAttachRealHeader(attachment.getKey()), attachment.getValue());
        });
    }


    /**
     * parse rest request header  attachment & header
     *
     * @param rpcInvocation
     * @param requestFacade
     */
    public static void parseRequestHeader(RpcInvocation rpcInvocation, RequestFacade requestFacade) {

        Enumeration<String> headerNames = requestFacade.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();

            if (!isRestAttachHeader(header)) {
                // attribute
                rpcInvocation.put(header, requestFacade.getHeader(header));
                continue;
            }

            // attachment
            rpcInvocation.setAttachment(subRestAttachRealHeaderPrefix(header.trim()), requestFacade.getHeader(header));

        }
    }


    /**
     * for judge rest header or rest attachment
     *
     * @param header
     * @return
     */
    public static boolean isRestAttachHeader(String header) {

        if (StringUtils.isEmpty(header) || !header.startsWith(RestHeaderEnum.REST_HEADER_PREFIX.getHeader())) {
            return false;
        }

        return true;
    }

    /**
     * for substring attachment prefix
     *
     * @param header
     * @return
     */
    public static String subRestAttachRealHeaderPrefix(String header) {

        return header.substring(RestHeaderEnum.REST_HEADER_PREFIX.getHeader().length());
    }

    /**
     * append prefix to rest header  distinguish from normal header
     *
     * @param header
     * @return
     */
    public static String appendPrefixToAttachRealHeader(String header) {

        return RestHeaderEnum.REST_HEADER_PREFIX.getHeader() + header;
    }


    /**
     *  parse request attribute
     * @param rpcInvocation
     * @param request
     */
    public static void parseRequestAttribute(RpcInvocation rpcInvocation, RequestFacade request) {
        int localPort = request.getLocalPort();
        String localAddr = request.getLocalAddr();
        int remotePort = request.getRemotePort();
        String remoteAddr = request.getRemoteAddr();

        rpcInvocation.put(RestConstant.REMOTE_ADDR, remoteAddr);
        rpcInvocation.put(RestConstant.LOCAL_ADDR, localAddr);
        rpcInvocation.put(RestConstant.REMOTE_PORT, remotePort);
        rpcInvocation.put(RestConstant.LOCAL_PORT, localPort);
    }


    /**
     *  parse request
     * @param rpcInvocation
     * @param request
     */
    public static void parseRequest(RpcInvocation rpcInvocation, RequestFacade request) {
        parseRequestHeader(rpcInvocation, request);
        parseRequestAttribute(rpcInvocation, request);
    }

    /**
     *  parse rest response header to appResponse attribute & attachment
     * @param appResponse
     * @param restResult
     */
    public static void parseResponseHeader(AppResponse appResponse, RestResult restResult) {

        Map<String, List<String>> headers = restResult.headers();
        if (headers == null || headers.isEmpty()) {
            return;
        }

        headers.entrySet().stream().forEach(entry -> {
            String header = entry.getKey();
            if (isRestAttachHeader(header)) {
                // attachment
                appResponse.setAttachment(subRestAttachRealHeaderPrefix(header), entry.getValue());
            } else {
                // attribute
                appResponse.setAttribute(header, entry.getValue());
            }
        });


    }
}
