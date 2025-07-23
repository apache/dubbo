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
package org.apache.dubbo.rpc.protocol.tri.rest.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.support.InstantiationStrategy;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionAccessorAware;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestInitializeException;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.RadixTree.Match;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Activate(group = CommonConstants.PROVIDER, order = 1000)
public class RestExtensionExecutionFilter extends RestFilterAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestExtensionExecutionFilter.class);
    private static final String KEY = RestExtensionExecutionFilter.class.getSimpleName();
    private static final String REST_FILTER_CACHE = "REST_FILTER_CACHE";

    private final Map<RestFilter, RadixTree<Boolean>> filterTreeCache = CollectionUtils.newConcurrentHashMap();
    private final ApplicationModel applicationModel;
    private final List<RestExtensionAdapter<Object>> extensionAdapters;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public RestExtensionExecutionFilter(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        extensionAdapters = (List) applicationModel.getActivateExtensions(RestExtensionAdapter.class);
    }

    @Override
    protected Result invoke(Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response)
            throws RpcException {
        RestFilter[] filters = matchFilters(getFilters(invoker), request.path());
        DefaultFilterChain chain = new DefaultFilterChain(filters, invocation, () -> invoker.invoke(invocation));
        invocation.put(KEY, chain);
        try {
            Result result = chain.execute(request, response);
            if (result != null) {
                return result;
            }
            Object body = response.body();
            if (body instanceof Throwable) {
                response.setBody(null);
                return AsyncRpcResult.newDefaultAsyncResult((Throwable) body, invocation);
            }
            if (body instanceof CompletableFuture) {
                CompletableFuture<?> future = (CompletableFuture<?>) body;
                response.setBody(null);
                return new AsyncRpcResult(
                        future.handleAsync((v, t) -> {
                            AppResponse r = new AppResponse(invocation);
                            if (t != null) {
                                r.setException(t);
                            } else {
                                r.setValue(v);
                            }
                            return r;
                        }),
                        invocation);
            }
            return AsyncRpcResult.newDefaultAsyncResult(invocation);
        } catch (Throwable t) {
            throw ExceptionUtils.wrap(t);
        }
    }

    @Override
    protected void onResponse(
            Result result, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {
        DefaultFilterChain chain = (DefaultFilterChain) invocation.get(KEY);
        if (chain == null) {
            return;
        }
        chain.onResponse(result, request, response);
        if (result.hasException()) {
            Object body = response.body();
            if (body != null) {
                if (body instanceof Throwable) {
                    result.setException((Throwable) body);
                } else {
                    result.setValue(body);
                    result.setException(null);
                }
                response.setBody(null);
            }
        }
    }

    @Override
    protected void onError(
            Throwable t, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {
        DefaultFilterChain chain = (DefaultFilterChain) invocation.get(KEY);
        if (chain == null) {
            return;
        }
        chain.onError(t, request, response);
    }

    private RestFilter[] matchFilters(RestFilter[] filters, String path) {
        int len = filters.length;
        BitSet bitSet = new BitSet(len);
        for (int i = 0; i < len; i++) {
            RestFilter filter = filters[i];
            String[] patterns = filter.getPatterns();
            if (ArrayUtils.isEmpty(patterns)) {
                continue;
            }

            RadixTree<Boolean> filterTree = filterTreeCache.computeIfAbsent(filter, f -> {
                RadixTree<Boolean> tree = new RadixTree<>();
                for (String pattern : patterns) {
                    if (StringUtils.isNotEmpty(pattern)) {
                        if (pattern.charAt(0) == '!') {
                            tree.addPath(pattern.substring(1), false);
                        } else {
                            tree.addPath(pattern, true);
                        }
                    }
                }
                return tree;
            });

            List<Match<Boolean>> matches = filterTree.match(path);
            int size = matches.size();
            if (size > 0) {
                if (size > 1) {
                    Collections.sort(matches);
                }
                if (matches.get(0).getValue()) {
                    continue;
                }
            }
            bitSet.set(i);
        }
        if (bitSet.isEmpty()) {
            return filters;
        }
        RestFilter[] matched = new RestFilter[len - bitSet.cardinality()];
        for (int i = 0, j = 0; i < len; i++) {
            if (!bitSet.get(i)) {
                matched[j++] = filters[i];
            }
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Matched filters for path '{}' is {}", path, Arrays.toString(matched));
        }
        return matched;
    }

    private RestFilter[] getFilters(Invoker<?> invoker) {
        return UrlUtils.computeServiceAttribute(invoker.getUrl(), REST_FILTER_CACHE, this::loadFilters);
    }

    private RestFilter[] loadFilters(URL url) {
        List<RestFilter> extensions = new ArrayList<>();

        // 1. load from extension config
        String extensionConfig = url.getParameter(RestConstants.EXTENSION_KEY);
        InstantiationStrategy strategy = new InstantiationStrategy(() -> applicationModel);
        for (String className : StringUtils.tokenize(extensionConfig)) {
            try {
                Object extension = strategy.instantiate(ClassUtils.loadClass(className));
                if (extension instanceof ExtensionAccessorAware) {
                    ((ExtensionAccessorAware) extension).setExtensionAccessor(applicationModel);
                }
                adaptExtension(extension, extensions);
            } catch (Throwable t) {
                throw new RestInitializeException(t, Messages.EXTENSION_INIT_FAILED, className, url);
            }
        }

        // 2. load from extension loader
        List<RestExtension> restExtensions = applicationModel
                .getExtensionLoader(RestExtension.class)
                .getActivateExtension(url, RestConstants.REST_FILTER_KEY);
        for (RestExtension extension : restExtensions) {
            adaptExtension(extension, extensions);
        }

        // 3. sorts by order
        extensions.sort(Comparator.comparingInt(RestUtils::getPriority));

        LOGGER.info("Rest filters for [{}] loaded: {}", url, extensions);
        return extensions.toArray(new RestFilter[0]);
    }

    private void adaptExtension(Object extension, List<RestFilter> extensions) {
        if (extension instanceof Supplier) {
            extension = ((Supplier<?>) extension).get();
        }
        if (extension instanceof RestFilter) {
            addRestFilter(extension, (RestFilter) extension, extensions);
            return;
        }
        for (RestExtensionAdapter<Object> adapter : extensionAdapters) {
            if (adapter.accept(extension)) {
                addRestFilter(extension, adapter.adapt(extension), extensions);
            }
        }
    }

    private void addRestFilter(Object extension, RestFilter filter, List<RestFilter> extensions) {
        extensions.add(filter);
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder(64);
        sb.append("Rest filter [").append(extension).append("] loaded");
        if (filter.getPriority() != 0) {
            sb.append(", priority=").append(filter.getPriority());
        }
        if (ArrayUtils.isNotEmpty(filter.getPatterns())) {
            sb.append(", patterns=").append(Arrays.toString(filter.getPatterns()));
        }
        LOGGER.info(sb.toString());
    }
}
