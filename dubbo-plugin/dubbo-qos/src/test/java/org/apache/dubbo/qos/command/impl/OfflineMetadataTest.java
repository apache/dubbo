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
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class OfflineMetadataTest {
    private FrameworkModel frameworkModel;

    @BeforeEach
    public void setUp() throws Exception {
        frameworkModel = new FrameworkModel();
        for (int i = 0; i < 3; i++) {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            applicationModel.getApplicationConfigManager().setApplication(new ApplicationConfig("APP_" + i));
        }

    }

    @AfterEach
    public void reset() {
        frameworkModel.destroy();
    }

    @Test
    public void testExecute() {
        OfflineMetadata offlineMetadata = new OfflineMetadata(frameworkModel);

        String result = offlineMetadata.execute(Mockito.mock(CommandContext.class), new String[0]);
        String expectResult = "offline metadata succeeded. App:APP_0\n" +
            "offline metadata succeeded. App:APP_1\n" +
            "offline metadata succeeded. App:APP_2\n";
        Assertions.assertEquals(result, expectResult);

        // delay 5s
        result = offlineMetadata.execute(Mockito.mock(CommandContext.class), new String[]{"5"});
        expectResult = "offline task submitted, will offline in 5 seconds. App:APP_0\n" +
            "offline task submitted, will offline in 5 seconds. App:APP_1\n" +
            "offline task submitted, will offline in 5 seconds. App:APP_2\n";
        Assertions.assertEquals(result, expectResult);

        // wrong delay param
        result = offlineMetadata.execute(Mockito.mock(CommandContext.class), new String[]{"A"});
        expectResult = "offlineMetadata failed! Wrong delay param!";
        Assertions.assertEquals(result, expectResult);

    }
}
