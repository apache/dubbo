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
package org.apache.dubbo.rpc.protocol.rest.request.convert;

import okhttp3.*;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.http.okhttp.OKHttpRestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;
import org.apache.dubbo.rpc.protocol.rest.request.BaseConvert;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.util.Collection;
import java.util.Map;

@Activate("okhttp")
public class OkHttpRequestConvert extends BaseConvert<Request, Response, OKHttpRestClient> {

    public OkHttpRequestConvert(OKHttpRestClient restClient, RestMethodMetadata restMethodMetadata, URL url) {
        super(restClient, restMethodMetadata, url);
    }

    public OkHttpRequestConvert(URL url) {
        super(url);
    }

    @Override
    public Response send(Request request) throws Exception {
        return getRestClient().send(request);
    }




    @Override
    public Request convert(RequestTemplate requestTemplate) {
        Request.Builder builder = new Request.Builder();
        // url
        builder.url(requestTemplate.getURL());

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);

            for (String headerValue : headerValues) {
                builder.addHeader(headerName, headerValue);
            }
        }

        builder.method(requestTemplate.getHttpMethod(), RequestBody.create(null, requestTemplate.getSerializedBody()));
        return builder.build();
    }

    @Override
    public Object convertResponse(Response response) throws Exception {
        ResponseBody body = response.body();

        String content_type = response.header(RestConstant.CONTENT_TYPE);


        return HttpMessageCodec.httpMessageDecode(body.byteStream(), getReturnType(), MediaTypeUtil.convertMediaType(content_type));

    }
}
