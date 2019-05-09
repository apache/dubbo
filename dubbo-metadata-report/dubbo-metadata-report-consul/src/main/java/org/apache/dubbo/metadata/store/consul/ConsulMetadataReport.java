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

package org.apache.dubbo.metadata.store.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import com.ecwid.consul.v1.ConsulClient;

/**
 * metadata report impl for consul
 */
public class ConsulMetadataReport extends AbstractMetadataReport {
    private static final Logger logger = LoggerFactory.getLogger(ConsulMetadataReport.class);
    private static final int DEFAULT_PORT = 8500;

    private ConsulClient client;

    public ConsulMetadataReport(URL url) {
        super(url);

        String host = url.getHost();
        int port = url.getPort() != 0 ? url.getPort() : DEFAULT_PORT;
        client = new ConsulClient(host, port);
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    private void storeMetadata(MetadataIdentifier identifier, String v) {
        try {
            client.setKVValue(identifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), v);
        } catch (Throwable t) {
            logger.error("Failed to put " + identifier + " to consul " + v + ", cause: " + t.getMessage(), t);
            throw new RpcException("Failed to put " + identifier + " to consul " + v + ", cause: " + t.getMessage(), t);
        }
    }
}
