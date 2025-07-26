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
package org.apache.dubbo.xds.cluster;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.List;

public class XdsClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsClusterInvoker.class);

    public XdsClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    protected Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance)
            throws RpcException {
        logger.info("[XDS-CLUSTER] XdsClusterInvoker.doInvoke called with {} invokers", invokers.size());
        
        // 记录所有可用的invoker信息
        for (int i = 0; i < invokers.size(); i++) {
            Invoker<T> invoker = invokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            logger.info("[XDS-CLUSTER] Available invoker[{}]: {} (clusterID: {})", i, address, clusterID);
        }

        while (true) {
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            String selectedAddress = invoker.getUrl().getAddress();
            String selectedClusterID = invoker.getUrl().getParameter("clusterID");
            logger.info("[XDS-CLUSTER] LoadBalance selected invoker: {} (clusterID: {})", selectedAddress, selectedClusterID);
            
            try {
                logger.info("[XDS-CLUSTER] Attempting to invoke: {} with clusterID: {}", selectedAddress, selectedClusterID);
                Result result = invokeWithContext(invoker, invocation);
                logger.info("[XDS-CLUSTER] Invoke SUCCESS: {} returned result", selectedAddress);
                return result;
            } catch (Throwable e) {
                logger.error("[XDS-CLUSTER] Invoke FAILED: {} with error: {}", selectedAddress, e.getMessage());
                if (e instanceof RpcException && ((RpcException) e).isBiz()) { // biz exception.
                    throw (RpcException) e;
                }
                throw new RpcException(
                        e instanceof RpcException ? ((RpcException) e).getCode() : 0,
                        "Xds invoke providers " + invoker.getUrl() + " "
                                + loadbalance.getClass().getSimpleName()
                                + " for service " + getInterface().getName()
                                + " method " + RpcUtils.getMethodName(invocation) + " on consumer "
                                + NetUtils.getLocalHost()
                                + " use dubbo version " + Version.getVersion()
                                + ", but no luck to perform the invocation. Last error is: " + e.getMessage(),
                        e.getCause() != null ? e.getCause() : e);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
