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
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.json.JSON;
import org.apache.dubbo.common.json.factory.JsonFactory;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.io.ByteArrayOutputStream;

@Activate(RestConstant.SERIALIZE_INTERCEPT)
public class SerializeBodyIntercept implements HttpConnectionPreBuildIntercept {

    private static final Logger logger = LoggerFactory.getLogger(SerializeBodyIntercept.class);

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();

        if (requestTemplate.isBodyEmpty()) {
            return;
        }
        Object unSerializedBody = requestTemplate.getUnSerializedBody();


        try {

            URL url = connectionCreateContext.getUrl();
            JsonFactory jsonFactory = url.getApplicationModel().getAdaptiveExtension(JsonFactory.class);
            JSON json = jsonFactory.createJson(url);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            json.serializeObject(outputStream, unSerializedBody);
            requestTemplate.serializeBody(outputStream.toByteArray());
        } catch (Exception e) {

            logger.error("Rest  SerializeBodyIntercept serialize error: {}", e);
        }


    }


}
