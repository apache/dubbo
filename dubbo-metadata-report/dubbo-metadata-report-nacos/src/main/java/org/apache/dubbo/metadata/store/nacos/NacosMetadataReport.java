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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NacosUtils;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import java.util.Properties;


/**
 * metadata report impl for nacos
 */
public class NacosMetadataReport extends AbstractMetadataReport {
    private static final Logger logger = LoggerFactory.getLogger(NacosMetadataReport.class);
    private ConfigService configService;

    public NacosMetadataReport(URL url) {
        super(url);
        this.configService = buildConfigService(url);
    }

    public ConfigService buildConfigService(URL url) {
        Properties nacosProperties = NacosUtils.buildNacosProperties(url);
        try {
            configService = NacosFactory.createConfigService(nacosProperties);
        } catch (NacosException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getErrMsg(), e);
            }
            throw new IllegalStateException(e);
        }
        return configService;
    }


    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    private void storeMetadata(MetadataIdentifier identifier, String value) {
        try {
            boolean publishResult = configService.publishConfig(identifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), identifier.getGroup(), value);
            if (!publishResult) {
                throw new RuntimeException("publish nacos metadata failed");
            }
        } catch (Throwable t) {
            logger.error("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
            throw new RpcException("Failed to put " + identifier + " to nacos " + value + ", cause: " + t.getMessage(), t);
        }
    }
}
