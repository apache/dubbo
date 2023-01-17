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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.nacos.util.NacosNamingServiceUtils;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
import static com.alibaba.nacos.client.constant.Constants.HealthCheck.UP;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_INTERRUPTED;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;

public class NacosConnectionManager {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosNamingServiceUtils.class);


    private final URL connectionURL;

    private final List<NamingService> namingServiceList = new LinkedList<>();

    private final int retryTimes;

    private final int sleepMsBetweenRetries;

    private final boolean check;

    public NacosConnectionManager(URL connectionURL, boolean check, int retryTimes, int sleepMsBetweenRetries) {
        this.connectionURL = connectionURL;
        this.check = check;
        this.retryTimes = retryTimes;
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
        // create default one
        this.namingServiceList.add(createNamingService());
    }

    /**
     * @deprecated for ut only
     */
    @Deprecated
    protected NacosConnectionManager(NamingService namingService) {
        this.connectionURL = null;
        this.retryTimes = 0;
        this.sleepMsBetweenRetries = 0;
        this.check = false;
        // create default one
        this.namingServiceList.add(namingService);
    }

    public synchronized NamingService getNamingService() {
        if (namingServiceList.isEmpty()) {
            this.namingServiceList.add(createNamingService());
        }
        return namingServiceList.get(ThreadLocalRandom.current().nextInt(namingServiceList.size()));
    }

    public synchronized NamingService getNamingService(Set<NamingService> selected) {
        List<NamingService> copyOfNamingService = new LinkedList<>(namingServiceList);
        copyOfNamingService.removeAll(selected);
        if (copyOfNamingService.isEmpty()) {
            this.namingServiceList.add(createNamingService());
            return getNamingService(selected);
        }
        return copyOfNamingService.get(ThreadLocalRandom.current().nextInt(copyOfNamingService.size()));
    }

    public synchronized void shutdownAll() {
        for (NamingService namingService : namingServiceList) {
            try {
                namingService.shutDown();
            } catch (Exception e) {
                logger.warn(REGISTRY_NACOS_EXCEPTION, "", "", "Unable to shutdown nacos naming service", e);
            }
        }
        this.namingServiceList.clear();
    }

    /**
     * Create an instance of {@link NamingService} from specified {@link URL connection url}
     *
     * @return {@link NamingService}
     */
    protected NamingService createNamingService() {
        Properties nacosProperties = buildNacosProperties(this.connectionURL);
        NamingService namingService = null;
        try {
            for (int i = 0; i < retryTimes + 1; i++) {
                namingService = NacosFactory.createNamingService(nacosProperties);
                if (!check || (UP.equals(namingService.getServerStatus()) && testNamingService(namingService))) {
                    break;
                } else {
                    logger.warn(LoggerCodeConstants.REGISTRY_NACOS_EXCEPTION, "", "",
                        "Failed to connect to nacos naming server. " +
                            (i < retryTimes ? "Dubbo will try to retry in " + sleepMsBetweenRetries + ". " : "Exceed retry max times.") +
                            "Try times: " + (i + 1));
                }
                namingService.shutDown();
                namingService = null;
                Thread.sleep(sleepMsBetweenRetries);
            }
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(REGISTRY_NACOS_EXCEPTION, "", "", e.getErrMsg(), e);
            }
        } catch (InterruptedException e) {
            logger.error(INTERNAL_INTERRUPTED, "", "", "Interrupted when creating nacos naming service client.", e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }

        if (namingService == null) {
            logger.error(REGISTRY_NACOS_EXCEPTION, "", "", "Failed to create nacos naming service client. Reason: server status check failed.");
            throw new IllegalStateException("Failed to create nacos naming service client. Reason: server status check failed.");
        }

        return namingService;
    }

    private boolean testNamingService(NamingService namingService) {
        try {
            namingService.getAllInstances("Dubbo-Nacos-Test", false);
            return true;
        } catch (NacosException e) {
            return false;
        }
    }


    private Properties buildNacosProperties(URL url) {
        Properties properties = new Properties();
        setServerAddr(url, properties);
        setProperties(url, properties);
        return properties;
    }

    private void setServerAddr(URL url, Properties properties) {
        StringBuilder serverAddrBuilder =
            new StringBuilder(url.getHost()) // Host
                .append(':')
                .append(url.getPort()); // Port

        // Append backup parameter as other servers
        String backup = url.getParameter(BACKUP_KEY);
        if (StringUtils.isNotEmpty(backup)) {
            serverAddrBuilder.append(',').append(backup);
        }

        String serverAddr = serverAddrBuilder.toString();
        properties.put(SERVER_ADDR, serverAddr);
    }

    private void setProperties(URL url, Properties properties) {
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME, null);

        // @since 2.7.8 : Refactoring
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);
        if (StringUtils.isNotEmpty(url.getUsername())) {
            properties.put(USERNAME, url.getUsername());
        }
        if (StringUtils.isNotEmpty(url.getPassword())) {
            properties.put(PASSWORD, url.getPassword());
        }

        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");
    }

    private void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        } else {
            // when defaultValue is empty, we should not set empty value
            if (StringUtils.isNotEmpty(defaultValue)) {
                properties.setProperty(propertyName, defaultValue);
            }
        }
    }
}
