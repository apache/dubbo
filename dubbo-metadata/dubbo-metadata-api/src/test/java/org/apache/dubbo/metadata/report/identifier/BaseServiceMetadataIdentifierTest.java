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
package org.apache.dubbo.metadata.report.identifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BaseServiceMetadataIdentifierTest {

    private BaseServiceMetadataIdentifier baseServiceMetadataIdentifier;

    {
        baseServiceMetadataIdentifier = new BaseServiceMetadataIdentifier();
        baseServiceMetadataIdentifier.version = "1.0.0";
        baseServiceMetadataIdentifier.group = "test";
        baseServiceMetadataIdentifier.side = "provider";
        baseServiceMetadataIdentifier.serviceInterface = "BaseServiceMetadataIdentifierTest";
    }

    @Test
    void getUniqueKey() {
        String uniqueKey = baseServiceMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY, "appName");
        Assertions.assertEquals(uniqueKey, "BaseServiceMetadataIdentifierTest:1.0.0:test:provider:appName");

        String uniqueKey2 = baseServiceMetadataIdentifier.getUniqueKey(KeyTypeEnum.PATH, "appName");
        Assertions.assertEquals(uniqueKey2, "metadata/BaseServiceMetadataIdentifierTest/1.0.0/test/provider/appName");
    }

    @Test
    void getIdentifierKey() {
        String identifierKey = baseServiceMetadataIdentifier.getIdentifierKey( "appName");
        Assertions.assertEquals(identifierKey, "BaseServiceMetadataIdentifierTest:1.0.0:test:provider:appName");
    }
}
