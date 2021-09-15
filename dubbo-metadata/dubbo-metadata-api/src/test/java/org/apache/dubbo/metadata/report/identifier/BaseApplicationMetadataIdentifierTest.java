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

class BaseApplicationMetadataIdentifierTest {
    private BaseApplicationMetadataIdentifier baseApplicationMetadataIdentifier;

    {
        baseApplicationMetadataIdentifier = new BaseApplicationMetadataIdentifier();
        baseApplicationMetadataIdentifier.application = "app";
    }

    @Test
    void getUniqueKey() {
        String uniqueKey = baseApplicationMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY, "reversion");
        Assertions.assertEquals(uniqueKey, "app:reversion");

        String uniqueKey2 = baseApplicationMetadataIdentifier.getUniqueKey(KeyTypeEnum.PATH, "reversion");
        Assertions.assertEquals(uniqueKey2, "metadata/app/reversion");
    }

    @Test
    void getIdentifierKey() {
        String identifierKey = baseApplicationMetadataIdentifier.getIdentifierKey( "reversion");
        Assertions.assertEquals(identifierKey, "app:reversion");
    }
}
