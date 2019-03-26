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
package org.apache.dubbo.metadata.store.test;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.support.AbstractMetadataReport;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZookeeperRegistry
 */
public class JTestMetadataReport4Test extends AbstractMetadataReport {

    private final static Logger logger = LoggerFactory.getLogger(JTestMetadataReport4Test.class);


    public JTestMetadataReport4Test(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
    }

    public Map<String, String> store = new ConcurrentHashMap<>();


    private static String getProtocol(URL url) {
        String protocol = url.getParameter(Constants.SIDE_KEY);
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        store.put(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String serviceParameterString) {
        store.put(consumerMetadataIdentifier.getIdentifierKey(), serviceParameterString);
    }

    public static String getProviderKey(URL url) {
        return new MetadataIdentifier(url).getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY);
    }

    public static String getConsumerKey(URL url) {
        return new MetadataIdentifier(url).getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY);
    }

}
