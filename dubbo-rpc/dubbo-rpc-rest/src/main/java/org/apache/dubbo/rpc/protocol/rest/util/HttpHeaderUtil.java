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

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpHeaderUtil {

    private static final List<String> serverForbiddenAttachments = Arrays.asList(RestHeaderEnum.ACCEPT.getHeader(),
        RestHeaderEnum.VERSION.getHeader(),
        RestHeaderEnum.PATH.getHeader(),
        RestHeaderEnum.CONNECTION.getHeader(),
        RestHeaderEnum.KEEP_ALIVE_HEADER.getHeader(),
        RestHeaderEnum.KEEP_ALIVE_HEADER.getHeader(),
        RestHeaderEnum.CONTENT_TYPE.getHeader());


    public static Map<String, List<String>> createAttachments(Map<String, Object> attachmentMap) {
        Map<String, List<String>> attachments = new HashMap<>();
        int size = 0;
        for (Map.Entry<String, Object> entry : attachmentMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());

            if (value != null) {
                size += value.getBytes(StandardCharsets.UTF_8).length;
            }
            if (size > RestConstant.MAX_HEADER_SIZE) {
                throw new IllegalArgumentException("The attachments of " + RpcContext.class.getSimpleName() + " is too big");
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


    public static void addRequestAttachments(RequestTemplate requestTemplate, Map<String, Object> attachmentMap) {
        Map<String, List<String>> attachments = createAttachments(attachmentMap);

        attachments.entrySet().forEach(attachment -> {
            requestTemplate.addHeaders(attachment.getKey(), attachment.getValue());
        });

    }

    public static void addResponseAttachments(NettyHttpResponse nettyHttpResponse) {
        Map<String, List<String>> attachments = createAttachments(RpcContext.getServerContext().getObjectAttachments());

        attachments.entrySet().stream().forEach(attachment -> {
            nettyHttpResponse.getOutputHeaders().put(attachment.getKey(), attachment.getValue());
        });
    }

    public static void parseServerContextAttachment(RequestFacade<DefaultFullHttpRequest> requestFacade) {

        Enumeration<String> headerNames = requestFacade.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();

            if (StringUtils.isEmpty(header) || serverForbiddenAttachments.contains(header)) {
                continue;
            }

            RpcContext.getServerAttachment().setAttachment(header.trim(), requestFacade.getHeader(header));

        }
    }

}
