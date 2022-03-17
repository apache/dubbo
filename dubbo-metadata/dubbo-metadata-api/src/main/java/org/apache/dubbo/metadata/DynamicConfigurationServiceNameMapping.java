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
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.rpc.model.ApplicationModel.getName;

/**
 * The {@link ServiceNameMapping} implementation based on {@link DynamicConfiguration}
 */
public class DynamicConfigurationServiceNameMapping implements ServiceNameMapping {

    public static String DEFAULT_MAPPING_GROUP = "mapping";

    private static final List<String> IGNORED_SERVICE_INTERFACES = asList(MetadataService.class.getName());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void map(URL url) {
        //org.apache.dubbo.demo.GreetingService
        String serviceInterface = url.getServiceInterface();
        String group = url.getParameter(GROUP_KEY);

        /**
         * 过滤MetadataService
         */
        if (IGNORED_SERVICE_INTERFACES.contains(serviceInterface)) {
            return;
        }

        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();

        // the Dubbo Service Key as group
        // the service(application) name as key
        // It does matter whatever the content is, we just need a record
        /**
         * application名称
         */
        String key = getName();
        /**
         * 记录当前时间   用作比对
         */
        String content = valueOf(System.currentTimeMillis());

        execute(() -> {
            /**
             * 存储到配置中心   CompositeDynamicConfiguration
             * buildGroup     mapping-serviceInterface
             */
            dynamicConfiguration.publishConfig(key, ServiceNameMapping.buildGroup(serviceInterface), content);
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Dubbo service[%s] mapped to interface name[%s].",
                        group, serviceInterface));
            }
        });
    }

    /**
     * 在配置中心中  获取subscribedURL中group对应的dataId
     * @param subscribedURL the {@link URL} that the Dubbo consumer subscribed
     * @return
     */
    @Override
    public Set<String> getAndListen(URL url, MappingListener mappingListener) {
        String serviceInterface = url.getServiceInterface();
        DynamicConfiguration dynamicConfiguration = DynamicConfiguration.getDynamicConfiguration();

        Set<String> serviceNames = new LinkedHashSet<>();
        execute(() -> {
            /**
             * nacos：从配置中心中按group分页检索  并获取对应配置的dataId
             *
             * ps :	mapping-org.apache.dubbo.demo.DemoService
             *
             * {
             * 	"totalCount": 1,
             * 	"pageNumber": 1,
             * 	"pagesAvailable": 1,
             * 	"pageItems": [{
             * 		"id": "75",
             * 		"dataId": "dubbo-demo-api-provider",
             * 		"group": "mapping-org.apache.dubbo.demo.DemoService",
             * 		"content": "1605250745600",
             * 		"md5": null,
             * 		"tenant": "",
             * 		"appName": "",
             * 		"type": null
             *        }]
             * }
             *
             * CompositeDynamicConfiguration
             */
            Set<String> keys = dynamicConfiguration
                    .getConfigKeys(ServiceNameMapping.buildGroup(serviceInterface));
            if (CollectionUtils.isNotEmpty(keys)) {
                serviceNames.addAll(keys);
            }
        });
        return Collections.unmodifiableSet(serviceNames);
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
