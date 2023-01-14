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

import java.util.List;

import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
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

    private final NamingService namingService;

    private final int retryTimes;

    private final int sleepMsBetweenRetries;

    public NacosNamingServiceWrapper(NamingService namingService, int retryTimes, int sleepMsBetweenRetries) {
        this.namingService = namingService;
        this.retryTimes = Math.max(retryTimes, 0);
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
    }


    public String getServerStatus() {
        return namingService.getServerStatus();
    }

    public void subscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        accept(naming -> naming.subscribe(handleInnerSymbol(serviceName), group, eventListener));
    }

    public void unsubscribe(String serviceName, String group, EventListener eventListener) throws NacosException {
        accept(naming -> naming.unsubscribe(handleInnerSymbol(serviceName), group, eventListener));
    }

    public List<Instance> getAllInstances(String serviceName, String group) throws NacosException {
        return apply(naming -> naming.getAllInstances(handleInnerSymbol(serviceName), group));
    }

    public void registerInstance(String serviceName, String group, Instance instance) throws NacosException {
        accept(naming -> naming.registerInstance(handleInnerSymbol(serviceName), group, instance));
    }

    public void deregisterInstance(String serviceName, String group, String ip, int port) throws NacosException {
        accept(naming -> naming.deregisterInstance(handleInnerSymbol(serviceName), group, ip, port));
    }


    public void deregisterInstance(String serviceName, String group, Instance instance) throws NacosException {
        accept(naming -> naming.deregisterInstance(handleInnerSymbol(serviceName), group, instance));
    }

    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String group) throws NacosException {
        return apply(naming -> naming.getServicesOfServer(pageNo, pageSize, group));
    }

    public List<Instance> selectInstances(String serviceName, String group, boolean healthy) throws NacosException {
        return apply(naming -> naming.selectInstances(handleInnerSymbol(serviceName), group, healthy));
    }

    public void shutdown() throws NacosException {
        this.namingService.shutDown();
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

    private <R> R apply(NacosFunction<NamingService, R> command) throws NacosException {
        NacosException le = null;
        R result = null;
        int times = 0;
        for (; times < retryTimes + 1; times++) {
            try {
                result = command.apply(namingService);
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

    private void accept(NacosConsumer<NamingService> command) throws NacosException {
        NacosException le = null;
        int times = 0;
        for (; times < retryTimes + 1; times++) {
            try {
                command.accept(namingService);
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
