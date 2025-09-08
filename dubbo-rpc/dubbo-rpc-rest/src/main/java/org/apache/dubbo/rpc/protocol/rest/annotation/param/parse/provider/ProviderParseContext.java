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
package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;

import org.apache.dubbo.rpc.protocol.rest.annotation.BaseParseContext;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

public class ProviderParseContext extends BaseParseContext {

    private RequestFacade requestFacade;
    private Object response;
    private Object request;

    public ProviderParseContext(RequestFacade request) {
        this.requestFacade = request;
    }

    public RequestFacade getRequestFacade() {
        return requestFacade;
    }

    public void setValueByIndex(int index, Object value) {

        this.args.set(index, value);
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public String getPathVariable(int urlSplitIndex) {

        String[] split = getRequestFacade().getRequestURI().split("/");
        return split[urlSplitIndex].split("\\?")[0];
    }
}
