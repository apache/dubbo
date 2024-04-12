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
package org.apache.dubbo.xds.security.authz.rule.source;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.listener.LdsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;

import static org.apache.dubbo.xds.listener.ListenerConstants.LDS_CONNECTION_MANAGER;
import static org.apache.dubbo.xds.listener.ListenerConstants.LDS_VIRTUAL_INBOUND;


@Activate
public class LdsRuleProvider implements LdsListener, RuleProvider<HttpFilter> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(LdsRuleProvider.class);

    public LdsRuleProvider(ApplicationModel applicationModel) {}

    private volatile List<HttpFilter> rbacFilters = Collections.emptyList();

    @Override
    public void onResourceUpdate(List<Listener> listeners) {
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        this.rbacFilters = resolveHttpFilter(listeners);
    }

    public static List<HttpFilter> resolveHttpFilter(List<Listener> listeners) {
        List<HttpFilter> httpFilters = new ArrayList<>();
        for (Listener listener : listeners) {
            if (!listener.getName().equals(LDS_VIRTUAL_INBOUND)) {
                continue;
            }
            for (FilterChain filterChain : listener.getFilterChainsList()) {
                for (Filter filter : filterChain.getFiltersList()) {
                    if (!filter.getName().equals(LDS_CONNECTION_MANAGER)) {
                        continue;
                    }
                    HttpConnectionManager httpConnectionManager = unpackHttpConnectionManager(filter.getTypedConfig());
                    if (httpConnectionManager == null) {
                        continue;
                    }
                    for (HttpFilter httpFilter : httpConnectionManager.getHttpFiltersList()) {
                        if (httpFilter != null) {
                            httpFilters.add(httpFilter);
                        }
                    }
                }
            }
        }
        return httpFilters;
    }

    public static HttpConnectionManager unpackHttpConnectionManager(Any any) {
        try {
            if (!any.is(HttpConnectionManager.class)) {
                return null;
            }
            return any.unpack(HttpConnectionManager.class);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public List<HttpFilter> getSource(URL url, Invocation invocation) {
        return rbacFilters;
    }
}
