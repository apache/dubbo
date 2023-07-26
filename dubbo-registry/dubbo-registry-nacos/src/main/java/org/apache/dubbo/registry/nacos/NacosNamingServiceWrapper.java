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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.nacos.function.NacosConsumer;
import org.apache.dubbo.registry.nacos.function.NacosFunction;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

public class NacosNamingServiceWrapper {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosNamingServiceWrapper.class);

    private static final String INNERCLASS_SYMBOL = "$";

    private static final String INNERCLASS_COMPATIBLE_SYMBOL = "___";

    private final NacosConnectionManager nacosConnectionManager;

    private final int retryTimes;

    private final int sleepMsBetweenRetries;

    private final boolean isSupportBatchRegister;

    private final ConcurrentMap<InstanceId, InstancesInfo> registerStatus = new ConcurrentHashMap<>();
    private final ConcurrentMap<SubscribeInfo, NamingService> subscribeStatus = new ConcurrentHashMap<>();

    public NacosNamingServiceWrapper(NacosConnectionManager nacosConnectionManager, int retryTimes, int sleepMsBetweenRetries) {
        this.nacosConnectionManager = nacosConnectionManager;
        this.isSupportBatchRegister = MethodUtils.findMethod(NamingService.class, "batchRegisterInstance", String.class, String.class, List.class) != null;
        logger.info("Nacos batch register enable: " + isSupportBatchRegister);
        this.retryTimes = Math.max(retryTimes, 0);
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
    }

    /**
     * @deprecated for uts only
     */
    @Deprecated
    protected NacosNamingServiceWrapper(NacosConnectionManager nacosConnectionManager, boolean isSupportBatchRegister, int retryTimes, int sleepMsBetweenRetries) {
        this.nacosConnectionManager = nacosConnectionManager;
        this.isSupportBatchRegister = isSupportBatchRegister;
        this.retryTimes = Math.max(retryTimes, 0);
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
    }

    public String getServerStatus() {
        return nacosConnectionManager.getNamingService().getServerStatus();
    }

    public void subscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        SubscribeInfo subscribeInfo = new SubscribeInfo(nacosServiceName, group, eventListener);
        NamingService namingService = ConcurrentHashMapUtils.computeIfAbsent(subscribeStatus, subscribeInfo, info -> nacosConnectionManager.getNamingService());
        accept(() -> namingService.subscribe(nacosServiceName, group, eventListener));
    }

    public void unsubscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        SubscribeInfo subscribeInfo = new SubscribeInfo(nacosServiceName, group, eventListener);
        NamingService namingService = subscribeStatus.get(subscribeInfo);
        if (namingService != null) {
            accept(() -> namingService.unsubscribe(nacosServiceName, group, eventListener));
            subscribeStatus.remove(subscribeInfo);
        }
    }

    public List<Instance> getAllInstances(String serviceName, String group) throws NacosException {
        return apply(() -> nacosConnectionManager.getNamingService().getAllInstances(handleInnerSymbol(serviceName), group));
    }

    public void registerInstance(String serviceName, String group, Instance instance) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = ConcurrentHashMapUtils.computeIfAbsent(registerStatus, new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

        try {
            instancesInfo.lock();
            if (!instancesInfo.isValid()) {
                registerInstance(serviceName, group, instance);
                return;
            }
            if (instancesInfo.getInstances().isEmpty()) {
                // directly register
                NamingService namingService = nacosConnectionManager.getNamingService();
                accept(() -> namingService.registerInstance(nacosServiceName, group, instance));
                instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                return;
            }

            if (instancesInfo.getInstances().size() == 1 && isSupportBatchRegister) {
                InstanceInfo previous = instancesInfo.getInstances().get(0);
                List<Instance> instanceListToRegister = new ArrayList<>();

                NamingService namingService = previous.getNamingService();
                instanceListToRegister.add(previous.getInstance());
                instanceListToRegister.add(instance);

                try {
                    accept(() -> namingService.batchRegisterInstance(nacosServiceName, group, instanceListToRegister));
                    instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                    instancesInfo.setBatchRegistered(true);
                    return;
                } catch (NacosException e) {
                    logger.info("Failed to batch register to nacos. Service Name: " + serviceName + ". Maybe nacos server not support. Will fallback to multi connection register.");
                    // ignore
                }
            }

            if (instancesInfo.isBatchRegistered()) {
                NamingService namingService = instancesInfo.getInstances().get(0).getNamingService();
                List<Instance> instanceListToRegister = new ArrayList<>();
                for (InstanceInfo instanceInfo : instancesInfo.getInstances()) {
                    instanceListToRegister.add(instanceInfo.getInstance());
                }
                instanceListToRegister.add(instance);
                accept(() -> namingService.batchRegisterInstance(nacosServiceName, group, instanceListToRegister));
                instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
                return;
            }

            // fallback to register one by one
            Set<NamingService> selectedNamingServices = instancesInfo.getInstances()
                .stream()
                .map(InstanceInfo::getNamingService)
                .collect(Collectors.toSet());
            NamingService namingService = nacosConnectionManager.getNamingService(selectedNamingServices);
            accept(() -> namingService.registerInstance(nacosServiceName, group, instance));
            instancesInfo.getInstances().add(new InstanceInfo(instance, namingService));
        } finally {
            instancesInfo.unlock();
        }
    }

    public void updateInstance(String serviceName, String group, Instance oldInstance, Instance newInstance) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = ConcurrentHashMapUtils.computeIfAbsent(registerStatus, new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

        try {
            instancesInfo.lock();
            if (!instancesInfo.isValid() || instancesInfo.getInstances().isEmpty()) {
                throw new IllegalArgumentException(serviceName + " has not been registered to nacos.");
            }

            Optional<InstanceInfo> optional = instancesInfo.getInstances()
                .stream()
                .filter(instanceInfo -> instanceInfo.getInstance().equals(oldInstance))
                .findAny();

            if (!optional.isPresent()) {
                throw new IllegalArgumentException(oldInstance + " has not been registered to nacos.");
            }

            InstanceInfo oldInstanceInfo = optional.get();
            instancesInfo.getInstances().remove(oldInstanceInfo);
            instancesInfo.getInstances().add(new InstanceInfo(newInstance, oldInstanceInfo.getNamingService()));

            if (isSupportBatchRegister && instancesInfo.isBatchRegistered()) {
                NamingService namingService = oldInstanceInfo.getNamingService();
                List<Instance> instanceListToRegister = instancesInfo.getInstances().stream()
                    .map(InstanceInfo::getInstance)
                    .collect(Collectors.toList());

                accept(() -> namingService.batchRegisterInstance(nacosServiceName, group, instanceListToRegister));
                return;
            }

            // fallback to register one by one
            accept(() -> oldInstanceInfo.getNamingService().registerInstance(nacosServiceName, group, newInstance));
        } finally {
            instancesInfo.unlock();
        }
    }

    public void deregisterInstance(String serviceName, String group, String ip, int port) throws NacosException {
        String nacosServiceName = handleInnerSymbol(serviceName);
        InstancesInfo instancesInfo = ConcurrentHashMapUtils.computeIfAbsent(registerStatus, new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

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
        InstancesInfo instancesInfo = ConcurrentHashMapUtils.computeIfAbsent(registerStatus, new InstanceId(nacosServiceName, group), id -> new InstancesInfo());

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

            if (instancesInfo.getInstances().isEmpty()) {
                registerStatus.remove(new InstanceId(nacosServiceName, group));
                instancesInfo.setValid(false);
            }

            // only one registered
            if (instancesInfo.getInstances().isEmpty()) {
                // directly unregister
                accept(() -> instanceInfo.getNamingService().deregisterInstance(nacosServiceName, group, instance));
                instancesInfo.setBatchRegistered(false);
                return;
            }

            if (instancesInfo.isBatchRegistered()) {
                // register the rest instances
                List<Instance> instanceListToRegister = new ArrayList<>();
                for (InstanceInfo info : instancesInfo.getInstances()) {
                    instanceListToRegister.add(info.getInstance());
                }
                accept(() -> instanceInfo.getNamingService().batchRegisterInstance(nacosServiceName, group, instanceListToRegister));
            } else {
                // unregister one
                accept(() -> instanceInfo.getNamingService().deregisterInstance(nacosServiceName, group, instance));
            }
        } finally {
            instancesInfo.unlock();
        }
    }

    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String group) throws NacosException {
        return apply(() -> nacosConnectionManager.getNamingService().getServicesOfServer(pageNo, pageSize, group));
    }

    public List<Instance> selectInstances(String serviceName, String group, boolean healthy) throws NacosException {
        return apply(() -> nacosConnectionManager.getNamingService().selectInstances(handleInnerSymbol(serviceName), group, healthy));
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

    protected static class InstanceId {
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

    protected static class InstancesInfo {
        private final Lock lock = new ReentrantLock();
        private final List<InstanceInfo> instances = new ArrayList<>();
        private volatile boolean batchRegistered = false;
        private volatile boolean valid = true;

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

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }

    protected static class InstanceInfo {
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

    /**
     * @deprecated for uts only
     */
    @Deprecated
    protected Map<InstanceId, InstancesInfo> getRegisterStatus() {
        return registerStatus;
    }


    private <R> R apply(NacosFunction<R> command) throws NacosException {
        NacosException le = null;
        R result = null;
        int times = 0;
        for (; times < retryTimes + 1; times++) {
            try {
                result = command.apply();
                le = null;
                break;
            } catch (NacosException e) {
                le = e;
                logger.warn(LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION, "", "",
                    "Failed to request nacos naming server. " +
                        (times < retryTimes ? "Dubbo will try to retry in " + sleepMsBetweenRetries + ". " : "Exceed retry max times.") +
                        "Try times: " + (times + 1), e);
                if (times < retryTimes) {
                    try {
                        Thread.sleep(sleepMsBetweenRetries);
                    } catch (InterruptedException ex) {
                        logger.warn(LoggerCodeConstants.INTERNAL_INTERRUPTED, "", "", "Interrupted when waiting to retry.", ex);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        if (le != null) {
            throw le;
        }
        if (times > 1) {
            logger.info("Failed to request nacos naming server for " + (times - 1) + " times and finally success. " +
                "This may caused by high stress of nacos server.");
        }
        return result;
    }

    private void accept(NacosConsumer command) throws NacosException {
        NacosException le = null;
        int times = 0;
        for (; times < retryTimes + 1; times++) {
            try {
                command.accept();
                le = null;
                break;
            } catch (NacosException e) {
                le = e;
                logger.warn(LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION, "", "",
                    "Failed to request nacos naming server. " +
                        (times < retryTimes ? "Dubbo will try to retry in " + sleepMsBetweenRetries + ". " : "Exceed retry max times.") +
                        "Try times: " + (times + 1), e);
                if (times < retryTimes) {
                    try {
                        Thread.sleep(sleepMsBetweenRetries);
                    } catch (InterruptedException ex) {
                        logger.warn(LoggerCodeConstants.INTERNAL_INTERRUPTED, "", "", "Interrupted when waiting to retry.", ex);
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        if (le != null) {
            throw le;
        }
        if (times > 1) {
            logger.info("Failed to request nacos naming server for " + (times - 1) + " times and finally success. " +
                "This may caused by high stress of nacos server.");
        }
    }
}
