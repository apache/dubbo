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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that missing optional types do not prevent discovery of independent DTOs.
 * @date 2026-11-05
 */
class SerializeSecurityConfiguratorMissingTypeTest {
    private static final String FIXTURES = "com.serialization.MissingTypeFixtures$";

    @ParameterizedTest
    @ValueSource(
            strings = {
                "FieldService",
                "TransientFieldService",
                "GenericParameterService",
                "GenericFieldService",
                "GenericInterfaceService",
                "GenericSuperclassService",
                "BoundService",
                "GenericMethodService"
            })
    void continuesScanningAfterMissingType(String serviceName) throws Exception {
        ClassLoader loader = new MissingDependencyClassLoader();
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass(FIXTURES + "OptionalDependency"));
        Class<?> service = loader.loadClass(FIXTURES + serviceName);
        Class<?> healthy = loader.loadClass(FIXTURES + "Healthy");
        Class<?> nested = loader.loadClass(FIXTURES + "Nested");

        FrameworkModel framework = new FrameworkModel();
        try {
            ApplicationModel application = framework.newApplication();
            ApplicationConfig config = new ApplicationConfig("missing-type-test");
            config.setSerializeCheckStatus(SerializeCheckStatus.STRICT.name());
            config.setTrustSerializeClassLevel(Integer.MAX_VALUE);
            application.getApplicationConfigManager().setApplication(config);
            SerializeSecurityConfigurator configurator = new SerializeSecurityConfigurator(application.newModule());
            DefaultSerializeClassChecker checker =
                    framework.getBeanFactory().getBean(DefaultSerializeClassChecker.class);

            assertThrows(IllegalArgumentException.class, () -> checker.loadClass(loader, healthy.getName()));
            assertDoesNotThrow(() -> configurator.registerInterface(service));
            assertEquals(healthy, checker.loadClass(loader, healthy.getName()));
            assertEquals(nested, checker.loadClass(loader, nested.getName()));
            assertThrows(IllegalArgumentException.class, () -> checker.loadClass(loader, FIXTURES + "Unrelated"));
            assertDoesNotThrow(() -> configurator.registerInterface(service));
        } finally {
            framework.destroy();
        }
    }

    private static class MissingDependencyClassLoader extends ClassLoader {
        MissingDependencyClassLoader() {
            super(SerializeSecurityConfiguratorMissingTypeTest.class.getClassLoader());
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(FIXTURES + "OptionalDependency")) {
                throw new ClassNotFoundException(name);
            }
            if (!name.startsWith(FIXTURES)) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try (InputStream stream = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (stream == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] bytes = IOUtils.toByteArray(stream);
                    loaded = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
