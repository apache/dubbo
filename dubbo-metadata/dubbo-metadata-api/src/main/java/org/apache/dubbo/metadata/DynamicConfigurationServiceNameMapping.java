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
package org.apache.dubbo.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.utils.CollectionUtils.ofSet;
import static org.apache.dubbo.rpc.model.ApplicationModel.getName;

/**
 * The {@link ServiceNameMapping} implementation based on {@link DynamicConfiguration}
 */
public class DynamicConfigurationServiceNameMapping implements ServiceNameMapping {

    private static final List<String> IGNORED_SERVICE_INTERFACES = asList(MetadataService.class.getName());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int PUBLISH_CONFIG_RETRY_TIMES = 6;

    @Override
    public void map(URL url) {
        String serviceInterface = url.getServiceInterface();
        String group = url.getGroup();
        String version = url.getVersion();
        String protocol = url.getProtocol();

        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return;
        }

        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();

        // the Dubbo Service Key as group
        // the service(application) name as key
        // It does matter whatever the content is, we just need a record
        String key = getName();
        String content = valueOf(System.currentTimeMillis());

        execute(() -> {
            dynamicConfiguration.publishConfig(key, ServiceNameMapping.buildGroup(serviceInterface, group, version, protocol), content);
            if (logger.isDebugEnabled()) {
                logger.info(String.format("Dubbo service[%s] mapped to interface name[%s].",
                        group, serviceInterface, group));
            }
        });
    }

    @Override
    public void mapWithCas(URL url) {
        String serviceInterface = url.getServiceInterface();
        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return;
        }
        execute(() -> {
            publishConfigCas(serviceInterface, DEFAULT_MAPPING_GROUP, getName());
        });
    }

    @Override
    public Set<String> getAndListen(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        String group = url.getGroup();
        String version = url.getVersion();
        String protocol = url.getProtocol();
        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();

        Set<String> serviceNames = new LinkedHashSet<>();
        execute(() -> {
            Set<String> keys = dynamicConfiguration
                    .getConfigKeys(ServiceNameMapping.buildGroup(serviceInterface, group, version, protocol));
            serviceNames.addAll(keys);
        });
        return Collections.unmodifiableSet(serviceNames);
    }

    @Override
    public Set<String> getAndListenWithNewStore(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();
        Set<String> serviceNames = new LinkedHashSet<>();
        execute(() -> {
            String configContent = dynamicConfiguration.getConfig(serviceInterface, DEFAULT_MAPPING_GROUP);
            if (null != configContent) {
                String[] split = StringUtils.split(configContent, CommonConstants.COMMA_SEPARATOR_CHAR);
                serviceNames.addAll(ofSet(split));
            }
        });
        return Collections.unmodifiableSet(serviceNames);
    }

    /**
     * publish config with cas.
     *
     * @param key
     * @param group
     * @param appName
     * @return
     */
    private boolean publishConfigCas(String key, String group, String appName) {
        int currentRetryTimes = 1;
        boolean result = false;
        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();
        String newConfigContent = appName;
        do {
            ConfigItem configItem = dynamicConfiguration.getConfigItem(key, group);
            String oldConfigContent = configItem.getContent();
            if (StringUtils.isNotEmpty(oldConfigContent)) {
                boolean contains = StringUtils.isContains(configItem.getContent(), appName);
                if (contains) {
                    return true;
                }
                newConfigContent = oldConfigContent + COMMA_SEPARATOR + appName;
            }
            result = dynamicConfiguration.publishConfigCas(key, group, newConfigContent, configItem.getStat());
        } while (!result && currentRetryTimes++ <= PUBLISH_CONFIG_RETRY_TIMES);

        return result;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
        }
    }
}
