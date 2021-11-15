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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.nacos.NacosNamingServiceWrapper;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.utils.NamingUtils;

import java.util.Map;
import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMING_LOAD_CACHE_AT_START;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.client.naming.utils.UtilAndComs.NACOS_NAMING_LOG_NAME;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;

/**
 * The utilities class for {@link NamingService}
 *
 * @since 2.7.5
 */
public class NacosNamingServiceUtils {

    private static final Logger logger = LoggerFactory.getLogger(NacosNamingServiceUtils.class);
    private static String NACOS_GROUP_KEY = "nacos.group";

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
     * Create an instance of {@link NamingService} from specified {@link URL connection url}
     *
     * @param connectionURL {@link URL connection url}
     * @return {@link NamingService}
     * @since 2.7.5
     */
    public static NacosNamingServiceWrapper createNamingService(URL connectionURL) {
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

    private static void setProperties(URL url, Properties properties) {
        putPropertyIfAbsent(url, properties, NACOS_NAMING_LOG_NAME, null);

        // @since 2.7.8 : Refactoring
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);

        putPropertyIfAbsent(url, properties, NAMING_LOAD_CACHE_AT_START, "true");
    }

    private static void putPropertyIfAbsent(URL url, Properties properties, String propertyName, String defaultValue) {
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
