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
package org.apache.dubbo.rpc.protocol.jsonrpc;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


public class RpcContextFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcContextFilter.class);

    private static final String COMMA = ",";
    private static final String EQUAL = "=";

    public static final String DUBBO_ATTACHMENT_HEADER = "Dubbo-Attachments";

    /**
     * Add Dubbo-Attachments from HttpServletRequest to RpcContext.
     *
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    public void preHandle(HttpServletRequest request, HttpServletResponse response) {
        if (request == null) {
            return;
        }

        RpcContext.getContext().setRequest(request);
        RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());

        String headers = request.getHeader(DUBBO_ATTACHMENT_HEADER);
        if (headers != null) {
            Map<String, String> attachments = parse(headers);
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                RpcContext.getContext().setAttachment(entry.getKey(), entry.getValue());
            }
        }
    }

    public static Map<String, String> parse(String headers) {
        Map<String, String> result = new HashMap<>();
        for (String header : headers.split(COMMA)) {
            if (StringUtils.isBlank(header)) {
                continue;
            }

            int index = header.indexOf(EQUAL);
            if (index > 0) {
                String key = header.substring(0, index);
                String value = header.substring(index + 1);
                if (!StringUtils.isEmpty(key)) {
                    result.put(key.trim(), value.trim());
                }
            }
        }

        return result;
    }

    public static String parse(Map<String, String> attachments) {
        StringBuilder sb = new StringBuilder();
        int size = attachments.size();
        int index = size;
        for (Map.Entry<String, String> entry : attachments.entrySet()) {
            if (entry.getValue().contains(COMMA) || entry.getValue().contains(EQUAL)
                    || entry.getKey().contains(COMMA) || entry.getKey().contains(EQUAL)) {
                LOGGER.warn("Ignore the attachment : " + entry + " of " + RpcContext.class.getSimpleName() +
                        " must not contain ',' or '=' when using jsonrpc protocol");
                index--;
                continue;
            }

            if (index < size && index > 0) {
                sb.append(',');
            }
            index--;

            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
