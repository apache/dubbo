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
package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 2019/1/7
 */
public class MetadataIdentifierTest {

    @Test
    public void testGetUniqueKey() {
        String interfaceName = "org.apache.dubbo.metadata.integration.InterfaceNameTestService";
        String version = "1.0.0.zk.md";
        String group = null;
        String application = "vic.zk.md";
        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.PROVIDER_SIDE, application);
        System.out.println(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH));
        Assertions.assertEquals(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.PATH), "metadata" + Constants.PATH_SEPARATOR + interfaceName + Constants.PATH_SEPARATOR + (version == null ? "" : (version + Constants.PATH_SEPARATOR))
                + (group == null ? "" : (group + Constants.PATH_SEPARATOR)) + Constants.PROVIDER_SIDE + Constants.PATH_SEPARATOR + application);
        System.out.println(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY));
        Assertions.assertEquals(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY),
                interfaceName + MetadataIdentifier.SEPARATOR + (version == null ? "" : version + MetadataIdentifier.SEPARATOR) + (group == null ? "" : group + MetadataIdentifier.SEPARATOR) + Constants.PROVIDER_SIDE + MetadataIdentifier.SEPARATOR + application);
    }
}
