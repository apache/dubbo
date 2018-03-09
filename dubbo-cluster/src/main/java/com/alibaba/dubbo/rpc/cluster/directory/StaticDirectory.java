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
package com.alibaba.dubbo.rpc.cluster.directory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.alibaba.dubbo.common.utils.StringUtils.parseQueryString;

/**
 * StaticDirectory,
 * support new unit feature.
 * @author yiji.github@hotmail.com
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {

    private final List<Invoker<T>> invokers;

    private volatile Map<String, List<Invoker<T>>> methodInvokerMap;

    public StaticDirectory(List<Invoker<T>> invokers) {
        this(null, invokers, null);
    }

    public StaticDirectory(List<Invoker<T>> invokers, List<Router> routers) {
        this(null, invokers, routers);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, List<Router> routers) {
        super(url == null && invokers != null && !invokers.isEmpty() ? invokers.get(0).getUrl() : url, routers);
        if (invokers == null || invokers.isEmpty())
            throw new IllegalArgumentException("invokers == null");
        this.invokers = invokers;
        this.consumerUrl = parseConsumerUrl(getUrl());
        this.methodInvokerMap = toMethodInvokerMap(invokers);
    }

    /**
     * Transform the invokers list into a mapping relationship with a method
     */
    private Map<String,List<Invoker<T>>> toMethodInvokerMap(List<Invoker<T>> invokers) {

        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        newMethodInvokerMap.put(Constants.ANY_VALUE, invokers);

        if (invokers != null && invokers.size() > 0) {
            for (Invoker<T> invoker : invokers) {
                URL invokerUrl = invoker.getUrl();
                if(invokerUrl != null) {
                    String parameter = invokerUrl.getParameter(Constants.METHODS_KEY);
                    if (parameter != null && parameter.length() > 0) {
                        String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                        if (methods != null && methods.length > 0) {
                            for (String method : methods) {
                                if (method != null && method.length() > 0
                                        && !Constants.ANY_VALUE.equals(method)) {
                                    List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                    if (methodInvokers == null) {
                                        methodInvokers = new ArrayList<Invoker<T>>();
                                        newMethodInvokerMap.put(method, methodInvokers);
                                    }
                                    methodInvokers.add(invoker);
                                }
                            }
                        }
                    }
                }
            }
        }

        String methods = parseQueryString(getUrl().getParameterAndDecoded(Constants.REFER_KEY)).get(Constants.METHODS_KEY);
        if(methods != null) {
            String[] serviceMethods = Constants.COMMA_SPLIT_PATTERN.split(methods);
            if (serviceMethods != null && serviceMethods.length > 0) {
                for (String method : serviceMethods) {
                    List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                    if (methodInvokers == null || methodInvokers.isEmpty()) {
                        methodInvokers = invokers;
                    }
                    newMethodInvokerMap.put(method, route(methodInvokers, method));
                }
            }
        }

        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);

            Collections.sort(methodInvokers, new Comparator<Invoker<T>>() {
                @Override
                public int compare(Invoker<T> provider, Invoker<T> other) {
                    URL providerUrl = provider.getUrl(), otherUrl = other.getUrl();
                    if(providerUrl != null && otherUrl != null)
                        return providerUrl.toString().compareTo(otherUrl.toString());
                    if(providerUrl == null && otherUrl == null) return 0;
                    return -1;
                }
            });

            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }

        return Collections.unmodifiableMap(newMethodInvokerMap);
    }

    public Class<T> getInterface() {
        return invokers.get(0).getInterface();
    }

    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        for (Invoker<T> invoker : invokers) {
            if (invoker.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        super.destroy();
        for (Invoker<T> invoker : invokers) {
            invoker.destroy();
        }
        invokers.clear();
    }

    @Override
    protected List<Invoker<T>> doList(Invocation invocation) throws RpcException {

        List<Invoker<T>> invokers = null;

        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap;
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);

            if (invokers == null) {
                invokers = localMethodInvokerMap.get(methodName);
            }
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
        }

        return invokers == null ? this.invokers : invokers;
    }
}