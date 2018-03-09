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
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;
import com.alibaba.dubbo.rpc.cluster.router.MockInvokersSelector;
import com.alibaba.dubbo.rpc.cluster.router.unit.UnitRouter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.alibaba.dubbo.common.utils.StringUtils.containsParseKey;
import static com.alibaba.dubbo.common.utils.StringUtils.parseQueryString;

/**
 * Abstract implementation of Directory: Invoker list returned from this Directory's list method have been filtered by Routers
 *
 */
public abstract class AbstractDirectory<T> implements Directory<T> {

    // logger
    private static final Logger logger = LoggerFactory.getLogger(AbstractDirectory.class);

    private final URL url;

    private volatile boolean destroyed = false;

    protected volatile URL consumerUrl;

    private volatile List<Router> routers;

    public AbstractDirectory(URL url) {
        this(url, null);
    }

    public AbstractDirectory(URL url, List<Router> routers) {
        this(url, url, routers);
    }

    public AbstractDirectory(URL url, URL consumerUrl, List<Router> routers) {
        if (url == null)
            throw new IllegalArgumentException("url == null");
        this.url = url;
        this.consumerUrl = consumerUrl;
        setRouters(routers);
    }

    public List<Invoker<T>> list(Invocation invocation) throws RpcException {
        if (destroyed) {
            throw new RpcException("Directory already destroyed .url: " + getUrl());
        }
        List<Invoker<T>> invokers = doList(invocation);
        List<Router> localRouters = this.routers; // local reference
        if (localRouters != null && !localRouters.isEmpty()) {
            for (Router router : localRouters) {
                try {
                    if (router.getUrl() == null || router.getUrl().getParameter(Constants.RUNTIME_KEY, false)) {
                        invokers = router.route(invokers, getConsumerUrl(), invocation);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                }
            }
        }
        return invokers;
    }

    public URL getUrl() {
        return url;
    }

    public List<Router> getRouters() {
        return routers;
    }

    protected void setRouters(List<Router> routers) {
        // copy list
        routers = routers == null ? new ArrayList<Router>() : new ArrayList<Router>(routers);
        // append url router
        String routerkey = url.getParameter(Constants.ROUTER_KEY);
        if (routerkey != null && routerkey.length() > 0) {
            RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getExtension(routerkey);
            routers.add(routerFactory.getRouter(url));
        }
        // append mock invoker selector
        routers.add(new MockInvokersSelector());
        Collections.sort(routers);

        /**
         *  We expect unit router will be executed at last,
         *  because if the unit router has no match,
         *  it will return the previous available invokers.
         */

        if(detectUnitRouter()) {
            routers.add(new UnitRouter());
        }

        this.routers = routers;
    }

    protected List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {

        if(invokers == null || invokers.isEmpty()) return invokers;

        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        List<Router> routers = getRouters();
        if (routers != null) {
            for (Router router : routers) {
                if (router.getUrl() != null) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }

    public void setConsumerUrl(URL consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    private boolean detectUnitRouter() {
        String unitKey = url.getParameter(Constants.UNIT_KEY);
        if((unitKey != null && unitKey.length() > 0)
                // we check if consumer contains `unit` property
                || containsParseKey(url.getParameterAndDecoded(Constants.REFER_KEY), Constants.UNIT_KEY))
            return true;

        return false;
    }

    /**
     * use to find current consumer url.
     *
     */
    public URL parseConsumerUrl(final URL url) {

        String refer, path;

        URL invokerUrl = url.setProtocol(
                url.getParameter(Constants.REGISTRY_KEY, Constants.DEFAULT_REGISTRY))
                .removeParameter(Constants.REGISTRY_KEY);

        if((refer = invokerUrl.getParameterAndDecoded(Constants.REFER_KEY)) != null
                && refer.length() > 0) {

            Map<String, String> parameters = parseQueryString(refer);
            parameters.remove(Constants.MONITOR_KEY);

            if(!(Constants.ANY_VALUE).equals(path = parameters.remove(Constants.INTERFACE_KEY))){
                URL consumerUrl = new URL(Constants.CONSUMER_PROTOCOL
                        , parameters.remove(Constants.REGISTER_IP_KEY), 0
                        , path  , parameters)
                        . addParameters(Constants.CATEGORY_KEY
                                , Constants.CONSUMERS_CATEGORY
                                , Constants.CHECK_KEY
                                , String.valueOf(false));

                return consumerUrl;
            }
        }

        return url;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

    protected abstract List<Invoker<T>> doList(Invocation invocation) throws RpcException;

}