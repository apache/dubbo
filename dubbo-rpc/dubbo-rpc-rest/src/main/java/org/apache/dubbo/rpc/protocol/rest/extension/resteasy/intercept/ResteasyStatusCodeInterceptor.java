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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.intercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.extension.resteasy.ResteasyContext;
import org.apache.dubbo.rpc.protocol.rest.filter.RestResponseInterceptor;
import org.apache.dubbo.rpc.protocol.rest.filter.context.RestInterceptContext;

import org.jboss.resteasy.specimpl.AbstractBuiltResponse;

@Activate(
        value = "resteasy-resStatus",
        onClass = {
            "javax.ws.rs.ext.WriterInterceptorContext",
            "org.jboss.resteasy.specimpl.BuiltResponse",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpRequest",
            "org.jboss.resteasy.plugins.server.netty.NettyHttpResponse"
        },
        order = Integer.MAX_VALUE)
public class ResteasyStatusCodeInterceptor implements RestResponseInterceptor, ResteasyContext {

    @Override
    public void intercept(RestInterceptContext restResponseInterceptor) throws Exception {
        Object result = restResponseInterceptor.getResult();

        if (result == null || (!(result instanceof AbstractBuiltResponse))) {
            return;
        }

        AbstractBuiltResponse abstractBuiltResponse = (AbstractBuiltResponse) result;

        restResponseInterceptor.getResponse().setStatus(abstractBuiltResponse.getStatus());
    }
}
