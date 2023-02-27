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
package org.apache.dubbo.registry.nacos.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
<<<<<<< HEAD
import org.apache.dubbo.registry.nacos.NacosNamingServiceWrapper;
=======
import org.apache.dubbo.registry.nacos.NacosConnectionManager;
import org.apache.dubbo.registry.nacos.NacosNamingServiceWrapper;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
>>>>>>> origin/3.2

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.utils.NamingUtils;

<<<<<<< HEAD
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;
=======
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
>>>>>>> origin/3.2

/**
 * The utilities class for {@link NamingService}
 *
 * @since 2.7.5
 */
public class NacosNamingServiceUtils {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosNamingServiceUtils.class);
    private static final String NACOS_GROUP_KEY = "nacos.group";

    private static final String NACOS_RETRY_KEY = "nacos.retry";

    private static final String NACOS_RETRY_WAIT_KEY = "nacos.retry-wait";

    private static final String NACOS_CHECK_KEY = "nacos.check";

    private NacosNamingServiceUtils() {
        throw new IllegalStateException("NacosNamingServiceUtils should not be instantiated");
    }

    /**
     * Convert the {@link ServiceInstance} to {@link Instance}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return non-null
     * @since 2.7.5
     */
    public static Instance toInstance(ServiceInstance serviceInstance) {
        Instance instance = new Instance();
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setIp(serviceInstance.getHost());
        instance.setPort(serviceInstance.getPort());
        instance.setMetadata(serviceInstance.getSortedMetadata());
        instance.setEnabled(serviceInstance.isEnabled());
        instance.setHealthy(serviceInstance.isHealthy());
        return instance;
    }

    /**
     * Convert the {@link Instance} to {@link ServiceInstance}
     *
     * @param instance {@link Instance}
     * @return non-null
     * @since 2.7.5
     */
    public static ServiceInstance toServiceInstance(URL registryUrl, Instance instance) {
        DefaultServiceInstance serviceInstance =
            new DefaultServiceInstance(
                NamingUtils.getServiceName(instance.getServiceName()),
                instance.getIp(), instance.getPort(),
                ScopeModelUtil.getApplicationModel(registryUrl.getScopeModel()));
        serviceInstance.setMetadata(instance.getMetadata());
        serviceInstance.setEnabled(instance.isEnabled());
        serviceInstance.setHealthy(instance.isHealthy());
        return serviceInstance;
    }

    /**
     * The group of {@link NamingService} to register
     *
     * @param connectionURL {@link URL connection url}
     * @return non-null, "default" as default
     * @since 2.7.5
     */
    public static String getGroup(URL connectionURL) {
        // Compatible with nacos grouping via group.
        String group = connectionURL.getParameter(GROUP_KEY, DEFAULT_GROUP);
        return connectionURL.getParameter(NACOS_GROUP_KEY, group);
    }

    /**
     * Create an instance of {@link NamingService} from specified {@link URL connection url}
     *
     * @param connectionURL {@link URL connection url}
     * @return {@link NamingService}
     * @since 2.7.5
     */
    public static NacosNamingServiceWrapper createNamingService(URL connectionURL) {
<<<<<<< HEAD
        Properties nacosProperties = buildNacosProperties(connectionURL);
        NamingService namingService;
        try {
            namingService = NacosFactory.createNamingService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return new NacosNamingServiceWrapper(namingService);
    }

    private static Properties buildNacosProperties(URL url) {
        Properties properties = new Properties();
        setServerAddr(url, properties);
        setProperties(url, properties);
        return properties;
    }

    private static void setServerAddr(URL url, Properties properties) {
        StringBuilder serverAddrBuilder =
                new StringBuilder(url.getHost()) // Host
                        .append(":")
                        .append(url.getPort()); // Port

        // Append backup parameter as other servers
        String backup = url.getParameter(BACKUP_KEY);
        if (backup != null) {
            serverAddrBuilder.append(",").append(backup);
        }

        String serverAddr = serverAddrBuilder.toString();
        properties.put(SERVER_ADDR, serverAddr);
    }

    private static void setProperties(URL url, Properties properties) {
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME);

        // @since 2.7.8 : Refactoring
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);
        if (StringUtils.isNotEmpty(url.getUsername())){
            properties.put(USERNAME, url.getUsername());
        }
        if (StringUtils.isNotEmpty(url.getPassword())){
            properties.put(PASSWORD, url.getPassword());
        }
        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        }
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
        String propertyValue = url.getParameter(propertyName);
        if (StringUtils.isNotEmpty(propertyValue)) {
            properties.setProperty(propertyName, propertyValue);
        } else {
            properties.setProperty(propertyName, defaultValue);
        }
=======
        boolean check = connectionURL.getParameter(NACOS_CHECK_KEY, true);
        int retryTimes = connectionURL.getPositiveParameter(NACOS_RETRY_KEY, 10);
        int sleepMsBetweenRetries = connectionURL.getPositiveParameter(NACOS_RETRY_WAIT_KEY, 10);
        NacosConnectionManager nacosConnectionManager = new NacosConnectionManager(connectionURL, check, retryTimes, sleepMsBetweenRetries);
        return new NacosNamingServiceWrapper(nacosConnectionManager, retryTimes, sleepMsBetweenRetries);
>>>>>>> origin/3.2
    }

    public static Map<String, String> getNacosPreservedParam(URL registryUrl) {
        Map<String, String> map = new HashMap<>();
        if (registryUrl.getParameter(PreservedMetadataKeys.REGISTER_SOURCE) != null) {
            map.put(PreservedMetadataKeys.REGISTER_SOURCE, registryUrl.getParameter(PreservedMetadataKeys.REGISTER_SOURCE));
        }
        if (registryUrl.getParameter(PreservedMetadataKeys.HEART_BEAT_TIMEOUT) != null) {
            map.put(PreservedMetadataKeys.HEART_BEAT_TIMEOUT, registryUrl.getParameter(PreservedMetadataKeys.HEART_BEAT_TIMEOUT));
        }
        if (registryUrl.getParameter(PreservedMetadataKeys.IP_DELETE_TIMEOUT) != null) {
            map.put(PreservedMetadataKeys.IP_DELETE_TIMEOUT, registryUrl.getParameter(PreservedMetadataKeys.IP_DELETE_TIMEOUT));
        }
        if (registryUrl.getParameter(PreservedMetadataKeys.HEART_BEAT_INTERVAL) != null) {
            map.put(PreservedMetadataKeys.HEART_BEAT_INTERVAL, registryUrl.getParameter(PreservedMetadataKeys.HEART_BEAT_INTERVAL));
        }
        if (registryUrl.getParameter(PreservedMetadataKeys.INSTANCE_ID_GENERATOR) != null) {
            map.put(PreservedMetadataKeys.INSTANCE_ID_GENERATOR, registryUrl.getParameter(PreservedMetadataKeys.INSTANCE_ID_GENERATOR));
        }
        return map;
    }
}
