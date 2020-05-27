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
package org.apache.dubbo.rpc.cluster.router.expression.context;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default context builder used for evaluating the expressions.
 * @author Weihua
 * @since 2.7.8
 */
@Activate
public class DefaultContextBuilder implements ContextBuilder {

    /**
     * The object name of Client-Side
     */
    private static final String CLIENT_NAME = "c";
    /**
     * The object name of Request
     */
    private static final String REQUEST_NAME = "r";
    /**
     * The object name of Server-Side
     */
    private static final String SERVER_NAME = "s";

    @Override
    public Map<String, Object> buildClientContext(URL url, Invocation invocation) {
        return CollectionUtils.toMap(REQUEST_NAME, invocation.getAttachments(), CLIENT_NAME, url.getParameters());
    }

    @Override
    public <T> Map<String, Object> buildServerContext(Invoker<T> invoker, URL url, Invocation invocation) {
        Map<String, Object> params = new HashMap<>(invoker.getUrl().getParameters());
        params.put("port", invoker.getUrl().getPort());
        params.put("address", invoker.getUrl().getAddress());
        return Collections.singletonMap(SERVER_NAME, params);
    }
}
