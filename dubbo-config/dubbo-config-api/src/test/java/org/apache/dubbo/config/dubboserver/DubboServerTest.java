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
package org.apache.dubbo.config.dubboserver;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DubboServer} Test
 *
 * @since 2.7.5
 */
public class DubboServerTest {

    private static File dubboProperties;

    @BeforeAll
    public static void setUp(@TempDir Path folder) {
        ApplicationModel.reset();
        dubboProperties = folder.resolve(CommonConstants.DUBBO_PROPERTIES_KEY).toFile();
        System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, dubboProperties.getAbsolutePath());
    }

    @AfterEach
    public void tearDown() throws IOException {
        ApplicationModel.reset();
    }

    @Test
    public void checkApplication() {
        System.setProperty("dubbo.application.name", "demo");
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.refresh();
        Assertions.assertEquals("demo", applicationConfig.getName());
        System.clearProperty("dubbo.application.name");
    }

    @Test
    public void basicConfiguration() {
//        DubboServer dubboServer = DubboServer.getInstance();
//        assertEquals(dubboServer.isStarted(), false);
//        assertEquals(dubboServer.isInitialized(), false);
//        assertNotEquals(dubboServer.getApplication(), null);
//        dubboServer.start();

    }
}
