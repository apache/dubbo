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


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

/**
 * for request body Serialize
 */
@Activate(value = RestConstant.SERIALIZE_INTERCEPT, order = Integer.MAX_VALUE)
public class SerializeBodyIntercept implements HttpConnectionPreBuildIntercept {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SerializeBodyIntercept.class);

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();

        if (requestTemplate.isBodyEmpty()) {
            return;
        }


        try {
            Object unSerializedBody = requestTemplate.getUnSerializedBody();
            URL url = connectionCreateContext.getUrl();
            // TODO pool
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Collection<String> headers = requestTemplate.getHeaders(RestConstant.CONTENT_TYPE);
            MediaType mediaType = MediaTypeUtil.convertMediaType(requestTemplate.getBodyType(), headers.toArray(new String[0]));

            // add mediaType by targetClass serialize
            if (headers.isEmpty() && mediaType != null && !mediaType.equals(MediaType.ALL_VALUE)) {
                headers.add(mediaType.value);
            }
            HttpMessageCodecManager.httpMessageEncode(outputStream, unSerializedBody, url, mediaType, requestTemplate.getBodyType());
            requestTemplate.serializeBody(outputStream.toByteArray());
            outputStream.close();
        } catch (Exception e) {
            logger.error(LoggerCodeConstants.PROTOCOL_ERROR_DESERIALIZE, "", "", "Rest SerializeBodyIntercept serialize error: {}", e);
            throw new RpcException(e);
        }


    }


}
