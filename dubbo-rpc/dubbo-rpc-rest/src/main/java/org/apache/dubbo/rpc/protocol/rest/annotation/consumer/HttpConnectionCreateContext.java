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
package org.apache.dubbo.rpc.protocol.rest.annotation.consumer;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;

import java.util.List;

public class HttpConnectionCreateContext {

    private RequestTemplate requestTemplate;
    private HttpConnectionConfig connectionConfig;
    private RestMethodMetadata restMethodMetadata;
    private List<Object> methodRealArgs;

    public HttpConnectionCreateContext() {
    }


    public HttpConnectionCreateContext(RequestTemplate requestTemplate,
                                       HttpConnectionConfig connectionConfig,
                                       List<Object> methodRealArgs) {
        this.requestTemplate = requestTemplate;
        this.connectionConfig = connectionConfig;
        this.methodRealArgs = methodRealArgs;
    }

    public void setRequestTemplate(RequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    public void setConnectionConfig(HttpConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }


    public RequestTemplate getRequestTemplate() {
        return requestTemplate;
    }

    public HttpConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public ServiceRestMetadata getServiceRestMetadata() {
        return restMethodMetadata.getServiceRestMetadata();
    }

    public RestMethodMetadata getRestMethodMetadata() {
        return restMethodMetadata;
    }

    public void setRestMethodMetadata(RestMethodMetadata restMethodMetadata) {
        this.restMethodMetadata = restMethodMetadata;
    }

    public List<Object> getMethodRealArgs() {
        return methodRealArgs;
    }

    public void setMethodRealArgs(List<Object> methodRealArgs) {
        this.methodRealArgs = methodRealArgs;
    }
}
