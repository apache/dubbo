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
package org.apache.dubbo.rpc.protocol.tri.rest.support.servlet;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.message.HttpMessageAdapterFactory;

@Activate(order = -100, onClass = "javax.servlet.http.HttpServletRequest")
public final class ServletHttpMessageAdapterFactory
        implements HttpMessageAdapterFactory<ServletHttpRequestAdaptee, HttpMetadata, Void> {

    @Override
    public ServletHttpRequestAdaptee adapterRequest(HttpMetadata rawRequest, HttpChannel channel) {
        return new ServletHttpRequestAdaptee(rawRequest, channel);
    }

    @Override
    public HttpResponse adapterResponse(ServletHttpRequestAdaptee request, HttpMetadata rawRequest, Void rawResponse) {
        return new ServletHttpResponseAdaptee();
    }
}
