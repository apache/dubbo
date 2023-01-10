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
package org.apache.dubbo.rpc.protocol.rest.request;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.convert.RequestConvert;


import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;


public abstract class BaseConvert<REQ, RES, CLIENT> implements RequestConvert<REQ, RES, CLIENT> {

    protected CLIENT restClient;
    protected RestMethodMetadata restMethodMetadata;
    protected URL url;

    public BaseConvert(URL url) {
        this.url = url;
    }

    public BaseConvert(CLIENT restClient, RestMethodMetadata restMethodMetadata, URL url) {
        this.restClient = restClient;
        this.restMethodMetadata = restMethodMetadata;
        this.url = url;
    }

    public Object request(RequestTemplate requestTemplate) throws RemotingException {


        REQ request = null;
        try {
            request = convert(requestTemplate);
        } catch (Exception e) {
            // TODO convert exception
        }


        RES response = null;
        try {
            response = send(request);
        } catch (Exception e) {
            // TODO send exception
        }


        // TODO add http response code judge

        Object result = null;
        try {
            result = convertResponse(response);
        } catch (Exception e) {
            // TODO response parse exception
        }

        return result;


    }

    protected Class<?> getReturnType() {
        Class<?> returnType = restMethodMetadata.getReflectMethod().getReturnType();
        return returnType;
    }

    protected int getTimeout() {
        int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        return timeout;
    }

    public CLIENT getRestClient() {
        return restClient;
    }

    @Override
    public RequestConvert init(CLIENT restClient, RestMethodMetadata restMethodMetadata) {
        this.restClient = restClient;
        this.restMethodMetadata = restMethodMetadata;
        return this;
    }
}
