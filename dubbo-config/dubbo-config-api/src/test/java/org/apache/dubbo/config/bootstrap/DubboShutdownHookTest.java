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
package org.apache.dubbo.config.bootstrap;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;
import org.apache.dubbo.config.SysProps;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DubboShutdownHookTest {
    private DubboShutdownHook dubboShutdownHook;
    private ApplicationModel applicationModel;

    @BeforeEach
    public void init() {
        FrameworkModel frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
        ModuleModel moduleModel = applicationModel.newModule();
        dubboShutdownHook = DubboShutdownHook.getInstance();
    }

    @AfterEach
    public void clear() {
        SysProps.clear();
    }

    @Test
    void testDestoryNoModuleManagedExternally() {
        boolean hasModuleManagedExternally = false;
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            if (moduleModel.isLifeCycleManagedExternally()) {
                hasModuleManagedExternally = true;
                break;
            }
        }
        Assertions.assertFalse(hasModuleManagedExternally);
        dubboShutdownHook.run();
        Assertions.assertTrue(applicationModel.isDestroyed());
    }

    @Test
    void testDestoryWithModuleManagedExternally() throws Exception {
        Field loggerField = ReflectionUtils.findFields(
                        DubboShutdownHook.class,
                        f -> f.getName().equals("logger"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        loggerField.setAccessible(true);
        ErrorTypeAwareLogger hookLogger = (ErrorTypeAwareLogger) loggerField.get(dubboShutdownHook);
        Method getLogger = ReflectionUtils.findMethod(FailsafeLogger.class, "getLogger", new Class<?>[0])
                .get();
        Method setLogger = ReflectionUtils.findMethod(FailsafeLogger.class, "setLogger", Logger.class)
                .get();
        Logger internalLogger = (Logger) getLogger.invoke(hookLogger, new Object[0]);

        Logger spyLogger = Mockito.spy(internalLogger);
        when(spyLogger.isInfoEnabled()).thenReturn(true);
        setLogger.invoke(hookLogger, spyLogger);

        ArgumentCaptor<String> loggerCaptor = ArgumentCaptor.forClass(String.class);

        applicationModel.getModuleModels().get(0).setLifeCycleManagedExternally(true);

        new Thread(() -> {
                    long now = System.currentTimeMillis();
                    Awaitility.await()
                            .atLeast(100, TimeUnit.MILLISECONDS)
                            .atMost(200, TimeUnit.MILLISECONDS)
                            .until(() -> {
                                return System.currentTimeMillis() - now > 100L;
                            });
                    applicationModel.getModuleModels().get(0).destroy();
                })
                .start();
        dubboShutdownHook.run();
        Assertions.assertTrue(applicationModel.isDestroyed());

        verify(spyLogger, atLeastOnce()).info(loggerCaptor.capture());

        StringBuffer logBuf = new StringBuffer();
        for (String row : loggerCaptor.getAllValues()) {
            logBuf.append(row).append("\n");
        }

        String logs = logBuf.toString();

        Assertions.assertTrue(logs.contains("Waiting for modules"));
        Assertions.assertTrue(logs.contains("managed by Spring to be shutdown failed")
                || logs.contains("managed by Spring has been destroyed successfully."));
        Assertions.assertTrue(logs.contains("Complete shutdown hook now."));
    }
}
