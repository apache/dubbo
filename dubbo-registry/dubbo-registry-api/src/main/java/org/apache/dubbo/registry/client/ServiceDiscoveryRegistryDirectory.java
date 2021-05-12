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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.AddressListener;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DISABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;

public class ServiceDiscoveryRegistryDirectory<T> extends DynamicDirectory<T> implements NotifyListener {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryRegistryDirectory.class);

    // instance address to invoker mapping.
    private volatile Map<String, Invoker<T>> urlInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference

    private ServiceInstancesChangedListener listener;

    public ServiceDiscoveryRegistryDirectory(Class<T> serviceType, URL url) {
        super(serviceType, url);
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized void notify(List<URL> instanceUrls) {
        // Set the context of the address notification thread.
        //dubbo://172.203.144.103/org.apache.dubbo.demo.DemoService?application=dubbo-demo-annotation-consumer&check=false&dubbo=2.0.2&init=false&interface=org.apache.dubbo.demo.DemoService&metadata-type=remote&methods=sayHello,sayHelloAsync&pid=6876&register.ip=172.203.144.103&side=consumer&sticky=false&timestamp=1619590317664
        RpcContext.setRpcContext(getConsumerUrl());

        /**
         * 3.x added for extend URL address
         * 使用3.x
         */
        ExtensionLoader<AddressListener> addressListenerExtensionLoader = ExtensionLoader.getExtensionLoader(AddressListener.class);
        List<AddressListener> supportedListeners = addressListenerExtensionLoader.getActivateExtension(getUrl(), (String[]) null);
        if (supportedListeners != null && !supportedListeners.isEmpty()) {
            for (AddressListener addressListener : supportedListeners) {
                instanceUrls = addressListener.notify(instanceUrls, getConsumerUrl(), this);
            }
        }

        /**
         * 刷新Invoker
         */
        refreshInvoker(instanceUrls);
    }

    /**
     *
     * @param invokerUrls
     */
    private void refreshInvoker(List<URL> invokerUrls) {
        Assert.notNull(invokerUrls, "invokerUrls should not be null, use empty url list to clear address.");

        /**
         * 为空
         */
        if (invokerUrls.size() == 0) {
            this.forbidden = true; // Forbid to access
            this.invokers = Collections.emptyList();
            routerChain.setInvokers(this.invokers);
            destroyAllInvokers(); // Close all invokers
            return;
        }

        this.forbidden = false; // Allow to access
        Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
        if (CollectionUtils.isEmpty(invokerUrls)) {
            return;
        }

        /**
         * 创建invokerUrls对应的invoker   消费端
         * 创建invokerUrls对应的invoker   消费端
         * 创建invokerUrls对应的invoker   消费端
         */
        Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);// Translate url list to Invoker map

        if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
            logger.error(new IllegalStateException("Cannot create invokers from url address list (total " + invokerUrls.size() + ")"));
            return;
        }

        List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
        // pre-route and build cache, notice that route cache should build on original Invoker list.
        // toMergeMethodInvokerMap() will wrap some invokers having different groups, those wrapped invokers not should be routed.
        /**
         * 预路由和构建缓存，注意路由缓存应该构建在原始调用器列表上
         * toMergeMethodInvokerMap（）将包装一些具有不同组的调用程序，这些包装的调用程序不应被路由
         *
         * 在第一时间从注册表通知路由器链初始地址。
         * 每当注册表中的地址更改时通知
         */
        routerChain.setInvokers(newInvokers);
        this.invokers = multiGroup ? toMergeInvokerList(newInvokers) : newInvokers;
        /**
         * 赋值
         */
        this.urlInvokerMap = newUrlInvokerMap;

        if (oldUrlInvokerMap != null) {
            try {
                /**
                 * 销毁不需要的invoker
                 */
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }

        // notify invokers refreshed
        this.invokersChanged();
    }

    /**
     * Turn urls into invokers, and if url has been refer, will not re-reference.
     * 将url调整为invokers   当url已经被引用时   不会重新引用
     * @param urls
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<>();
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }

        for (URL url : urls) {
            InstanceAddressURL instanceAddressURL = (InstanceAddressURL) url;
            if (EMPTY_PROTOCOL.equals(instanceAddressURL.getProtocol())) {
                continue;
            }
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(instanceAddressURL.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + instanceAddressURL.getProtocol() +
                        " in notified url: " + instanceAddressURL + " from registry " + getUrl().getAddress() +
                        " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " +
                        ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }

            // FIXME, some keys may need to be removed.
            /**
             * 修改metadataInfo内部protocolServiceKey对应的services中的consumerParams
             */
            instanceAddressURL.addConsumerParams(getConsumerUrl().getProtocolServiceKey(), queryMap);

            Invoker<T> invoker = urlInvokerMap == null ? null : urlInvokerMap.get(instanceAddressURL.getAddress());
            /**
             * invoker为空或有变化
             */
            if (invoker == null || urlChanged(invoker, instanceAddressURL)) { // Not in the cache, refer again
                try {
                    boolean enabled = true;
                    if (instanceAddressURL.hasParameter(DISABLED_KEY)) {
                        enabled = !instanceAddressURL.getParameter(DISABLED_KEY, false);
                    } else {
                        enabled = instanceAddressURL.getParameter(ENABLED_KEY, true);
                    }
                    if (enabled) {
                        /**
                         * 创建instanceAddressURL对应的invoker   消费端
                         * 创建instanceAddressURL对应的invoker   消费端
                         * 创建instanceAddressURL对应的invoker   消费端
                         *
                         * ProtocolFilterWrapper--AbstractProtocol--DubboProtocol
                         */
                        invoker = protocol.refer(serviceType, instanceAddressURL);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + instanceAddressURL + ")" + t.getMessage(), t);
                }
                /**
                 * 缓存invoker
                 */
                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(instanceAddressURL.getAddress(), invoker);
                }
            } else {
                /**
                 * 缓存invoker
                 */
                newUrlInvokerMap.put(instanceAddressURL.getAddress(), invoker);
            }
        }
        return newUrlInvokerMap;
    }

    private boolean urlChanged(Invoker<T> invoker, InstanceAddressURL newURL) {
        InstanceAddressURL oldURL = (InstanceAddressURL) invoker.getUrl();

        /**
         * url是否有变化
         */
        if (!newURL.getInstance().equals(oldURL.getInstance())) {
            return true;
        }

        /**
         * 元数据信息是否有变化
         */
        return !oldURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey())
                .equals(newURL.getMetadataInfo().getServiceInfo(getConsumerUrl().getProtocolServiceKey()));
    }

    private List<Invoker<T>> toMergeInvokerList(List<Invoker<T>> invokers) {
        return invokers;
    }

    /**
     * Close all invokers
     */
    @Override
    protected void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        invokers = null;
    }

    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            /**
             * 遍历oldUrlInvokerMap
             */
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                /**
                 * 两者不一直
                 */
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }

        if (deleted != null) {
            /**
             * 遍历待删除的key
             */
            for (String addressKey : deleted) {
                if (addressKey != null) {
                    /**
                     * 在原有缓存中删除addressKey对应的invoker
                     */
                    Invoker<T> invoker = oldUrlInvokerMap.remove(addressKey);
                    if (invoker != null) {
                        try {
                            /**
                             * 销毁
                             */
                            invoker.destroy();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destroy invoker[" + invoker.getUrl() + "] failed. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }
}
