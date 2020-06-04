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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

/**
 * The utilities class for {@link NamingService}
 *
 * @since 2.7.5
 */
public class NacosNamingServiceUtils {

    private static final Logger logger = LoggerFactory.getLogger(NacosNamingServiceUtils.class);

    private static final String[] NACOS_PROPERTY_NAMES;

    static {
        NACOS_PROPERTY_NAMES = initNacosPropertyNames();
    }

    private static String[] initNacosPropertyNames() {
        return Stream.of(PropertyKeyConst.class.getFields())
                .filter(f -> isStatic(f.getModifiers()))         // static
                .filter(f -> isPublic(f.getModifiers()))         // public
                .filter(f -> isFinal(f.getModifiers()))          // final
                .filter(f -> String.class.equals(f.getType()))   // String type
                .map(NacosNamingServiceUtils::getConstantValue)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray(String[]::new);
    }

    private static Object getConstantValue(Field field) {
        Object value = null;
        try {
            value = field.get(null);
        } catch (IllegalAccessException e) {
        }
        return value;
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
        instance.setInstanceId(serviceInstance.getId());
        instance.setServiceName(serviceInstance.getServiceName());
        instance.setIp(serviceInstance.getHost());
        instance.setPort(serviceInstance.getPort());
        instance.setMetadata(serviceInstance.getMetadata());
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
    public static ServiceInstance toServiceInstance(Instance instance) {
        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(instance.getInstanceId(),
                instance.getServiceName(), instance.getIp(), instance.getPort());
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
        return connectionURL.getParameter("nacos.group", DEFAULT_GROUP);
    }

    /**
     * Create an instance of {@link NamingService} from specified {@link URL connection url}
     *
     * @param connectionURL {@link URL connection url}
     * @return {@link NamingService}
     * @since 2.7.5
     */
    public static NamingService createNamingService(URL connectionURL) {
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
        return namingService;
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
        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");

        for (String propertyName : NACOS_PROPERTY_NAMES) {
            putPropertyIfAbsent(url, properties, propertyName);
        }
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName) {
        putPropertyIfAbsent(url, properties, propertyName, null);
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
        String propertyValue = url.getParameter(propertyName);
        putPropertyIfAbsent(properties, propertyName, propertyValue, defaultValue);
    }

    private static void putPropertyIfAbsent(Properties properties, String propertyName, String propertyValue) {
        putPropertyIfAbsent(properties, propertyName, propertyValue, null);
    }

    private static void putPropertyIfAbsent(Properties properties, String propertyName, String propertyValue,
                                            String defaultValue) {
        if (isEmpty(propertyName) && properties.containsKey(propertyName)) {
            return;
        }

        String value = isEmpty(propertyValue) ? defaultValue : propertyValue;

        if (isNotEmpty(value)) {
            properties.setProperty(propertyName, value);
        }
    }
}
