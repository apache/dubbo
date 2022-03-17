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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublishMetadataTest {

    @BeforeEach
    public void setUp() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("app"));
    }

    @AfterEach
    public void clear(){
        ApplicationModel.reset();
    }

    @Test
    public void testNoDelay() {
        PublishMetadata publishMetadata = new PublishMetadata();
        String msg = publishMetadata.execute(null, null);
        Assertions.assertEquals(msg, "publish metadata succeeded.");
    }

    @Test
    public void testWithDelay() {
        PublishMetadata publishMetadata = new PublishMetadata();
        String[] args = {"10"};
        String msg = publishMetadata.execute(null, args);
        Assertions.assertEquals(msg,"publish task submitted, will publish in " + args[0] + " seconds.");
    }
}
