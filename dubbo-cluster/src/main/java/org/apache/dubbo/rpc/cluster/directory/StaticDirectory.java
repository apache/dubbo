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
package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_SITE_SELECTION;

/**
 * StaticDirectory
 */
public class StaticDirectory<T> extends AbstractDirectory<T> {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(StaticDirectory.class);

    public StaticDirectory(List<Invoker<T>> invokers) {
        this(null, invokers, null);
    }

    public StaticDirectory(List<Invoker<T>> invokers, RouterChain<T> routerChain) {
        this(null, invokers, routerChain);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers) {
        this(url, invokers, null);
    }

    public StaticDirectory(URL url, List<Invoker<T>> invokers, RouterChain<T> routerChain) {
        super(url == null && CollectionUtils.isNotEmpty(invokers) ? invokers.get(0).getUrl() : url, routerChain, false);
        if (CollectionUtils.isEmpty(invokers)) {
            throw new IllegalArgumentException("invokers == null");
        }
        this.setInvokers(new BitList<>(invokers));
    }

    @Override
    public Class<T> getInterface() {
        return getInvokers().get(0).getInterface();
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return getInvokers();
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        for (Invoker<T> invoker : getValidInvokers()) {
            if (invoker.isAvailable()) {
                return true;
            } else {
                addInvalidateInvoker(invoker);
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        for (Invoker<T> invoker : getInvokers()) {
            invoker.destroy();
        }
        super.destroy();
    }

    public void buildRouterChain() {
        RouterChain<T> routerChain = RouterChain.buildChain(getInterface(), getUrl());
        routerChain.setInvokers(getInvokers(), () -> {
        });
        this.setRouterChain(routerChain);
    }

    public void notify(List<Invoker<T>> invokers) {
        BitList<Invoker<T>> bitList = new BitList<>(invokers);
        if (routerChain != null) {
            refreshRouter(bitList.clone(),  () -> this.setInvokers(bitList));
        } else {
            this.setInvokers(bitList);
        }
    }

    @Override
    protected List<Invoker<T>> doList(SingleRouterChain<T> singleRouterChain, BitList<Invoker<T>> invokers, Invocation invocation) throws RpcException {
        if (singleRouterChain != null) {
            try {
                List<Invoker<T>> finalInvokers = singleRouterChain.route(getConsumerUrl(), invokers, invocation);
                return finalInvokers == null ? BitList.emptyList() : finalInvokers;
            } catch (Throwable t) {
                logger.error(CLUSTER_FAILED_SITE_SELECTION, "Failed to execute router", "", "Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
                return BitList.emptyList();
            }
        }
        return invokers;
    }

}
