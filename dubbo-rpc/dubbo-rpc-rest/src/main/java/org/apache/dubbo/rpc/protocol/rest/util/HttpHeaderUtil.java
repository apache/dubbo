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
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpHeaderUtil {
    public static boolean illegalHttpHeaderKey(String key) {
        if (StringUtils.isNotEmpty(key)) {
            return key.contains(",") || key.contains("=");
        }
        return false;
    }

    public static boolean illegalHttpHeaderValue(String value) {
        if (StringUtils.isNotEmpty(value)) {
            return value.contains(",");
        }
        return false;
    }


    public static List<String> createAttachments(Map<String, Object> attachmentMap) {
        List<String> attachments = new ArrayList<>();
        int size = 0;
        for (Map.Entry<String, Object> entry : attachmentMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (illegalHttpHeaderKey(key) || illegalHttpHeaderValue(value)) {
                throw new IllegalArgumentException("The attachments of " + RpcContext.class.getSimpleName() + " must not contain ',' or '=' when using rest protocol");
            }

            // TODO for now we don't consider the differences of encoding and server limit
            if (value != null) {
                size += value.getBytes(StandardCharsets.UTF_8).length;
            }
            if (size > RestConstant.MAX_HEADER_SIZE) {
                throw new IllegalArgumentException("The attachments of " + RpcContext.class.getSimpleName() + " is too big");
            }

            String attachment = key + "=" + value;
            attachments.add(attachment);
        }

        return attachments;
    }


    public static void addConsumerAttachments(RequestTemplate requestTemplate, Map<String, Object> attachmentMap) {
        List<String> attachments = createAttachments(attachmentMap);

        attachments.stream().forEach(attachment -> {
            requestTemplate.addHeader(RestConstant.DUBBO_ATTACHMENT_HEADER, attachment);
        });

    }

    public static void addProviderAttachments(NettyHttpResponse nettyHttpResponse) {
        List<String> attachments = createAttachments(RpcContext.getServerContext().getObjectAttachments());
        nettyHttpResponse.getOutputHeaders().put(RestConstant.DUBBO_ATTACHMENT_HEADER, attachments);
    }


    public static void addProviderAttachments(HttpServletResponse httpServletResponse) {
        List<String> attachments = createAttachments(RpcContext.getServerContext().getObjectAttachments());

        attachments.stream().forEach(attachment -> {
            httpServletResponse.addHeader(RestConstant.DUBBO_ATTACHMENT_HEADER, attachment);
        });
    }


}
