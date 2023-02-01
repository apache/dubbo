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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.request.factory.HttpRequestFactory;
import org.apache.dubbo.rpc.protocol.rest.response.HttpResponseFacade;
import org.apache.dubbo.rpc.protocol.rest.response.HttpResponseFactory;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.lang.reflect.Method;

public class RestHttpMessageManager {
    private static final HttpRequestFactory httpRequestFactory =
        ApplicationModel.defaultModel().getExtensionLoader(HttpRequestFactory.class).getAdaptiveExtension();


    private RestClient restClient;
    private RequestTemplate requestTemplate;
    private Method method;
    private URL url;


    public RestHttpMessageManager(RestClient restClient, RequestTemplate requestTemplate, Method method, URL url) {
        this.restClient = restClient;
        this.requestTemplate = requestTemplate;
        this.method = method;
        this.url = url;
    }

    public Object requestAndGetResponse() throws Exception {
        Object httpRequest = httpRequestFactory.factory(url).createHttpRequest(requestTemplate);

        Object response = restClient.send(httpRequest);

        HttpResponseFacade responseFacade = HttpResponseFactory.createFacade(url.getParameter(Constants.CLIENT_KEY,"okhttp"), response);

        // TODO response code
        int responseCode = responseFacade.getResponseCode();


        return HttpMessageCodecManager.httpMessageDecode(responseFacade.getBody(), method.getReturnType(),
            MediaTypeUtil.convertMediaType(responseFacade.getContentType()));

    }


}
