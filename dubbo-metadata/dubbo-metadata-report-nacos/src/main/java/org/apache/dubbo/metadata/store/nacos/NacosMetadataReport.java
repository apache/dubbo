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

package org.apache.dubbo.metadata.store.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;
import static org.apache.dubbo.common.utils.StringConstantFieldValuePredicate.of;

/**
 * metadata report impl for nacos
 */
public class NacosMetadataReport extends AbstractMetadataReport {

    private NacosConfigServiceWrapper configService;

    private Gson gson = new Gson();

    /**
     * The group used to store metadata in Nacos
     */
    private String group;


    public NacosMetadataReport(URL url) {
        super(url);
        this.configService = buildConfigService(url);
        group = url.getParameter(GROUP_KEY, DEFAULT_ROOT);
    }

    public NacosConfigServiceWrapper buildConfigService(URL url) {
        Properties nacosProperties = buildNacosProperties(url);
        try {
            configService = new NacosConfigServiceWrapper(NacosFactory.createConfigService(nacosProperties));
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return configService;
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
        // Get the parameters from constants
        Map<String, String> parameters = url.getParameters(of(PropertyKeyConst.class));
        // Put all parameters
        properties.putAll(parameters);
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
    }

    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        String content = gson.toJson(metadataInfo);
        try {
            configService.publishConfig(identifier.getApplication(), identifier.getRevision(), content);
        } catch (NacosException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        try {
            String content = configService.getConfig(identifier.getApplication(), identifier.getRevision(), 3000L);
            return gson.fromJson(content, MetadataInfo.class);
        } catch (NacosException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        deleteMetadata(serviceMetadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = getConfig(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(Arrays.asList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return getConfig(subscriberMetadataIdentifier);
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return getConfig(metadataIdentifier);
    }

    private void storeMetadata(BaseMetadataIdentifier identifier, String value) {
        try {
            boolean publishResult = configService.publishConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, value);
            if (!publishResult) {
                throw new RuntimeException("publish nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
            throw new RpcException("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
        }
    }

    private void deleteMetadata(BaseMetadataIdentifier identifier) {
        try {
            boolean publishResult = configService.removeConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group);
            if (!publishResult) {
                throw new RuntimeException("remove nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to remove " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }

    private String getConfig(BaseMetadataIdentifier identifier) {
        try {
            return configService.getConfig(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), group, 3000L);
        } catch (Throwable t) {
            logger.error("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to get " + identifier + " from nacos , cause: " + t.getMessage(), t);
        }
    }

}
