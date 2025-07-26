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
package org.apache.dubbo.xds.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.support.FailbackRegistry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.XdsResourceFactory;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;

public class XdsRegistry extends FailbackRegistry {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsRegistry.class);
    private final XdsResourceFactory xdsResourceFactory = XdsResourceFactory.getInstance();

    // 用于累积来自不同集群的invoker
    private final Map<String, Map<String, List<URL>>> accumulatedInvokers = new ConcurrentHashMap<>();
    // 用于跟踪每个服务的NotifyListener
    private final Map<String, NotifyListener> serviceListeners = new ConcurrentHashMap<>();
    private ApplicationModel applicationModel;

    public XdsRegistry(URL url) {
        super(url);
        this.applicationModel = url.getApplicationModel();
        logger.info("[XDS] XdsRegistry initialized with URL: {}", url);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void doRegister(URL url) {
        logger.info("[XDS] doRegister called with URL: {}, but implementation is empty", url);
    }

    @Override
    public void doUnregister(URL url) {
        logger.info("[XDS] doUnregister called with URL: {}, but implementation is empty", url);
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        logger.info("[XDS] doSubscribe called with URL: {}", url);
        String appName = url.getParameter(PROVIDED_BY);
        logger.info("[XDS] PROVIDED_BY parameter: {}", appName);
        
        // 记录这个服务的NotifyListener
        final String finalAppName = appName;
        serviceListeners.put(finalAppName, listener);

        logger.info("[XDS] Calling subscribeApp for {} with listener {}", appName, listener);
        xdsResourceFactory.subscribeApp(appName, (addresses -> {
            logger.info("[XDS] EdsListener callback received addresses: {}", addresses);

            // 从地址中提取clusterID来识别这是哪个集群的更新
            String clusterID = null;
            if (!addresses.isEmpty()) {
                clusterID = addresses.get(0).getParameter("clusterID");
                logger.info("[XDS] Extracted clusterID: {} from addresses", clusterID);
            }

            List<URL> instances = addresses.stream()
                    .map(address -> new DubboServiceAddressURL(address.getUrlAddress(), address.getUrlParam(), url, null))
                    .collect(Collectors.toList());
            logger.info("[XDS] Converted to {} instances for cluster: {}", instances.size(), clusterID);

            // 累积来自不同集群的invoker
            accumulateAndNotifyInvokers(finalAppName, clusterID, instances, listener);
        }));
        logger.info("[XDS] doSubscribe completed for {}", appName);
    }

    private void accumulateAndNotifyInvokers(String serviceName, String clusterID, List<URL> newInvokers, NotifyListener listener) {
        logger.info("[XDS] accumulateAndNotifyInvokers called for service: {}, cluster: {}, invokers: {}", serviceName, clusterID, newInvokers.size());

        // 为这个服务创建集群映射（如果不存在）
        Map<String, List<URL>> clusterInvokers = accumulatedInvokers.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>());

        // 更新这个集群的invoker
        if (clusterID != null) {
            clusterInvokers.put(clusterID, new CopyOnWriteArrayList<>(newInvokers));
            logger.info("[XDS] Updated invokers for cluster: {}, count: {}", clusterID, newInvokers.size());
        } else {
            logger.warn("[XDS] ClusterID is null, using default key");
            clusterInvokers.put("default", new CopyOnWriteArrayList<>(newInvokers));
        }

        // 合并所有集群的invoker
        List<URL> allInvokers = clusterInvokers.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());

        logger.info("[XDS] Total accumulated invokers for service {}: {}", serviceName, allInvokers.size());
        logger.info("[XDS] Invokers by cluster: {}", clusterInvokers.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().size()
            )));

        // 通知监听器
        logger.info("[XDS] Notifying listener with {} total invokers", allInvokers.size());
        listener.notify(allInvokers);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        logger.info("[XDS] doUnsubscribe called with URL: {} and listener: {}, but implementation is empty", url, listener);
    }
}
