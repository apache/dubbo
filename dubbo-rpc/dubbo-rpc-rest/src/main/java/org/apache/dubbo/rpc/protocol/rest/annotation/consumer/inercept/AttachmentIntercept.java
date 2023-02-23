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
package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.nio.charset.StandardCharsets;
import java.util.Map;
@Activate(value = RestConstant.RPCCONTEXT_INTERCEPT,order = 3)
public class AttachmentIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();
        int size = 0;
        for (Map.Entry<String, Object> entry : connectionCreateContext.getInvocation().getObjectAttachments().entrySet()) {
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

            String attachments = key + "=" + value;
            requestTemplate.addHeader(RestConstant.DUBBO_ATTACHMENT_HEADER, attachments);
        }
    }

    private boolean illegalHttpHeaderKey(String key) {
        if (StringUtils.isNotEmpty(key)) {
            return key.contains(",") || key.contains("=");
        }
        return false;
    }

    private boolean illegalHttpHeaderValue(String value) {
        if (StringUtils.isNotEmpty(value)) {
            return value.contains(",");
        }
        return false;
    }
}
