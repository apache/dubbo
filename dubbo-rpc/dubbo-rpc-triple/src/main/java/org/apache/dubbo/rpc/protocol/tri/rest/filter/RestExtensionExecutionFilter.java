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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;
import org.apache.dubbo.rpc.protocol.tri.rest.RestInitializeException;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Activate(group = CommonConstants.PROVIDER)
public class RestExtensionExecutionFilter extends RestFilterAdapter {

    private static final String KEY = RestExtensionExecutionFilter.class.getSimpleName();

    private final FrameworkModel frameworkModel;
    private final List<RestExtensionAdapter<Object>> extensionAdapters;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public RestExtensionExecutionFilter(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        extensionAdapters = (List) frameworkModel.getActivateExtensions(RestExtensionAdapter.class);
    }

    @Override
    protected Result invoke(Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response)
            throws RpcException {
        RestFilter[] filters = getFilters(invoker);
        DefaultFilterChain chain = new DefaultFilterChain(filters, () -> invoker.invoke(invocation));
        invocation.put(KEY, chain);
        try {
            Result result = chain.execute(request, response);
            if (result != null) {
                return result;
            }
            HttpResult<Object> hResult = response.toHttpResult();
            if (hResult.getBody() instanceof Throwable) {
                return AsyncRpcResult.newDefaultAsyncResult((Throwable) hResult.getBody(), invocation);
            }
            return AsyncRpcResult.newDefaultAsyncResult(hResult, invocation);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RpcException(t);
        }
    }

    @Override
    protected void onResponse(
            Result result, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {
        ((DefaultFilterChain) invocation.get(KEY)).onSuccess(result, request, response);
    }

    @Override
    protected void onError(
            Throwable t, Invoker<?> invoker, Invocation invocation, HttpRequest request, HttpResponse response) {
        ((DefaultFilterChain) invocation.get(KEY)).onError(t, request, response);
    }

    private RestFilter[] getFilters(Invoker<?> invoker) {
        URL url = invoker.getUrl();
        RestFilter[] filters = getFilters(url);
        if (filters != null) {
            return filters;
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (invoker) {
            filters = getFilters(url);
            if (filters != null) {
                return filters;
            }
            filters = loadFilters(url);
            url.putAttribute(RestConstants.EXTENSIONS_ATTRIBUTE_KEY, filters);
            return filters;
        }
    }

    private RestFilter[] getFilters(URL url) {
        return (RestFilter[]) url.getAttribute(RestConstants.EXTENSIONS_ATTRIBUTE_KEY);
    }

    private RestFilter[] loadFilters(URL url) {
        List<RestFilter> extensions = new ArrayList<>();

        // 1. load from extension config
        String extensionConfig = url.getParameter(RestConstants.EXTENSION_KEY);
        InstantiationStrategy strategy = new InstantiationStrategy(() -> frameworkModel);
        for (String className : StringUtils.tokenize(extensionConfig)) {
            try {
                Object extension = strategy.instantiate(TypeUtils.loadClass(className));
                if (extension instanceof ExtensionAccessorAware) {
                    ((ExtensionAccessorAware) extension).setExtensionAccessor(frameworkModel);
                }
                RestFilter filter = adaptExtension(extension);
                if (filter != null) {
                    extensions.add(filter);
                }
            } catch (Throwable t) {
                throw new RestInitializeException(t, Messages.EXTENSION_INIT_FAILED, className, url);
            }
        }

        // 2. load from extension loader
        List<RestExtension> restExtensions = frameworkModel
                .getExtensionLoader(RestExtension.class)
                .getActivateExtension(url, RestConstants.REST_FILTER_KEY);
        for (RestExtension extension : restExtensions) {
            RestFilter filter = adaptExtension(extension);
            if (filter != null) {
                extensions.add(filter);
            }
        }

        // 3. sorts by order
        extensions.sort(Comparator.comparingInt(RestUtils::getPriority));

        return extensions.toArray(new RestFilter[0]);
    }

    private RestFilter adaptExtension(Object extension) {
        if (extension instanceof RestFilter) {
            return (RestFilter) extension;
        }
        for (RestExtensionAdapter<Object> adapter : extensionAdapters) {
            if (adapter.accept(extension)) {
                return adapter.adapt(extension);
            }
        }
        return null;
    }
}
