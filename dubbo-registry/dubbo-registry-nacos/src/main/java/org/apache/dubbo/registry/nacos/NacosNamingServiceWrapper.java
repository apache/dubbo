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
package org.apache.dubbo.registry.nacos;

import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.StringUtils;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class NacosNamingServiceWrapper {

    private static final String INNERCLASS_SYMBOL = "$";

    private static final String INNERCLASS_COMPATIBLE_SYMBOL = "___";

    private final NacosConnectionManager nacosConnectionManager;

    private final boolean isSupportBatchRegister;

    private final Map<InstanceId, InstancesInfo> registerStatus = new ConcurrentHashMap<>();
    private final Map<SubscribeInfo, NamingService> subscribeStatus = new ConcurrentHashMap<>();

    public NacosNamingServiceWrapper(NacosConnectionManager nacosConnectionManager) {
        this.nacosConnectionManager = nacosConnectionManager;
        this.isSupportBatchRegister = MethodUtils.findMethod(NamingService.class, "batchRegisterInstance", String.class, String.class, List.class) != null;
    }


    public String getServerStatus() {
        return nacosConnectionManager.getNamingService().getServerStatus();
    }

    public void subscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        SubscribeInfo subscribeInfo = new SubscribeInfo(nacosServiceName, group, eventListener);
        NamingService namingService = subscribeStatus.computeIfAbsent(subscribeInfo, (info) -> nacosConnectionManager.getNamingService());
        namingService.subscribe(nacosServiceName, group, eventListener);
    }

    public void unsubscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        SubscribeInfo subscribeInfo = new SubscribeInfo(nacosServiceName, group, eventListener);
        NamingService namingService = subscribeStatus.get(subscribeInfo);
        if (namingService != null) {
            namingService.unsubscribe(nacosServiceName, group, eventListener);
            subscribeStatus.remove(subscribeInfo);
        }
    }

    public List<Instance> getAllInstances(String serviceName, String group) throws NacosException {
        return nacosConnectionManager.getNamingService().getAllInstances(handleInnerSymbol(serviceName), group);
    }

    public void registerInstance(String serviceName, String group, Instance instance) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = registerStatus.computeIfAbsent(new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

        try {
            instancesInfo.lock();
            if (instancesInfo.getInstances().size() == 0) {
                // directly register
                NamingService namingService = nacosConnectionManager.getNamingService();
                namingService.registerInstance(nacosServiceName, group, instance);
                instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                return;
            }

            if (instancesInfo.getInstances().size() == 1) {
                if (isSupportBatchRegister) {
                    InstanceInfo previous = instancesInfo.getInstances().get(0);
                    List<Instance> instanceListToRegister = new ArrayList<>();

                    NamingService namingService = previous.getNamingService();
                    instanceListToRegister.add(previous.getInstance());

                    try {
                        namingService.batchRegisterInstance(nacosServiceName, group, instanceListToRegister);
                        instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                        instancesInfo.setBatchRegistered(true);
                        return;
                    } catch (NacosException e) {
                        // ignore
                    }
                }
            }

            if (instancesInfo.isBatchRegistered()) {
                NamingService namingService = instancesInfo.getInstances().get(0).getNamingService();
                List<Instance> instanceListToRegister = new ArrayList<>();
                for (InstanceInfo instanceInfo : instancesInfo.getInstances()) {
                    instanceListToRegister.add(instanceInfo.getInstance());
                }
                instanceListToRegister.add(instance);
                namingService.batchRegisterInstance(nacosServiceName, group, instanceListToRegister);
                instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                return;
            }

            // fallback to register one by one
            Set<NamingService> selectedNamingServices = instancesInfo.getInstances()
                .stream()
                .map(InstanceInfo::getNamingService)
                .collect(Collectors.toSet());
            NamingService namingService = nacosConnectionManager.getNamingService(selectedNamingServices);
            namingService.registerInstance(nacosServiceName, group, instance);
            instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
        } finally {
            instancesInfo.unlock();
        }
    }

    public void deregisterInstance(String serviceName, String group, String ip, int port) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = registerStatus.computeIfAbsent(new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

        try {
            instancesInfo.lock();

            List<Instance> instances = instancesInfo.getInstances()
                .stream()
                .map(InstanceInfo::getInstance)
                .filter(instance -> Objects.equals(instance.getIp(), ip) && instance.getPort() == port)
                .collect(Collectors.toList());
            for (Instance instance : instances) {
                deregisterInstance(serviceName, group, instance);
            }
        } finally {
            instancesInfo.unlock();
        }
    }


    public void deregisterInstance(String serviceName, String group, Instance instance) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = registerStatus.computeIfAbsent(new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

        try {
            instancesInfo.lock();
            Optional<InstanceInfo> optional = instancesInfo.getInstances()
                .stream()
                .filter(instanceInfo -> instanceInfo.getInstance().equals(instance))
                .findAny();
            if (!optional.isPresent()) {
                return;
            }
            InstanceInfo instanceInfo = optional.get();
            instancesInfo.getInstances().remove(instanceInfo);

            // only one registered
            if (instancesInfo.getInstances().size() == 0) {
                // directly unregister
                instanceInfo.getNamingService().deregisterInstance(nacosServiceName, group, instance);
                instancesInfo.setBatchRegistered(false);
                return;
            }

            if (instancesInfo.isBatchRegistered()) {
                // register the rest instances
                List<Instance> instanceListToRegister = new ArrayList<>();
                for (InstanceInfo info : instancesInfo.getInstances()) {
                    instanceListToRegister.add(info.getInstance());
                }
                instanceInfo.getNamingService().batchRegisterInstance(nacosServiceName, group, instanceListToRegister);
            } else {
                // unregister one
                instanceInfo.getNamingService().deregisterInstance(nacosServiceName, group, instance);
            }
        } finally {
            instancesInfo.unlock();
        }
    }

    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String group) throws NacosException {
        return nacosConnectionManager.getNamingService().getServicesOfServer(pageNo, pageSize, group);
    }

    public List<Instance> selectInstances(String serviceName, String group, boolean healthy) throws NacosException {
        return nacosConnectionManager.getNamingService().selectInstances(handleInnerSymbol(serviceName), group, healthy);
    }

    public void shutdown() throws NacosException {
        this.nacosConnectionManager.shutdownAll();
    }

    /**
     * see https://github.com/apache/dubbo/issues/7129
     * nacos service name just support `0-9a-zA-Z-._:`, grpc interface is inner interface, need compatible.
     */
    private String handleInnerSymbol(String serviceName) {
        if (StringUtils.isEmpty(serviceName)) {
            return null;
        }
        return serviceName.replace(INNERCLASS_SYMBOL, INNERCLASS_COMPATIBLE_SYMBOL);
    }

    private static class InstanceId {
        private final String serviceName;
        private final String group;

        public InstanceId(String serviceName, String group) {
            this.serviceName = serviceName;
            this.group = group;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getGroup() {
            return group;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InstanceId that = (InstanceId) o;
            return Objects.equals(serviceName, that.serviceName) && Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName, group);
        }
    }

    private static class InstancesInfo {
        private final Lock lock = new ReentrantLock();
        private final List<InstanceInfo> instances = new ArrayList<>();
        private volatile boolean batchRegistered = false;

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }

        public List<InstanceInfo> getInstances() {
            return instances;
        }

        public boolean isBatchRegistered() {
            return batchRegistered;
        }

        public void setBatchRegistered(boolean batchRegistered) {
            this.batchRegistered = batchRegistered;
        }
    }

    private static class InstanceInfo {
        private final Instance instance;
        private final NamingService namingService;

        public InstanceInfo(Instance instance, NamingService namingService) {
            this.instance = instance;
            this.namingService = namingService;
        }

        public Instance getInstance() {
            return instance;
        }

        public NamingService getNamingService() {
            return namingService;
        }
    }

    private static class SubscribeInfo {
        private final String serviceName;
        private final String group;
        private final EventListener eventListener;

        public SubscribeInfo(String serviceName, String group, EventListener eventListener) {
            this.serviceName = serviceName;
            this.group = group;
            this.eventListener = eventListener;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SubscribeInfo that = (SubscribeInfo) o;
            return Objects.equals(serviceName, that.serviceName) && Objects.equals(group, that.group) && Objects.equals(eventListener, that.eventListener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceName, group, eventListener);
        }
    }
}
