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
package org.apache.dubbo.test.check;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.HashSet;
import java.util.Set;

/**
 * The abstract implementation provides the basic methods. <p></p>
 * {@link #needRegistryCenter(TestPlan)}: checks if current {@link TestPlan} need registry center.
 */
public abstract class AbstractRegistryCenterTestExecutionListener implements TestExecutionListener {

    /**
     * The JVM arguments to set if it can use embedded zookeeper, the default value is {@code true}.
     */
    private static final String CONFIG_ENABLE_EMBEDDED_ZOOKEEPER = "enableEmbeddedZookeeper";

    /**
     * The registry center should start
     * if we want to run the test cases in the given package.
     */
    private static final Set<String> PACKAGE_NAME = new HashSet<>();

    /**
     * Use embedded zookeeper or not.
     */
    private static boolean enableEmbeddedZookeeper;

    static {
        // dubbo-config module
        PACKAGE_NAME.add("org.apache.dubbo.config");
        // dubbo-test module
        PACKAGE_NAME.add("org.apache.dubbo.test");
        // dubbo-registry
        PACKAGE_NAME.add("org.apache.dubbo.registry");
        // dubbo-remoting-zookeeper
        PACKAGE_NAME.add("org.apache.dubbo.remoting.zookeeper");
        // dubbo-metadata-report-zookeeper
        PACKAGE_NAME.add("org.apache.dubbo.metadata.store.zookeeper");

        enableEmbeddedZookeeper = Boolean.valueOf(System.getProperty(CONFIG_ENABLE_EMBEDDED_ZOOKEEPER, "true"));
    }

    /**
     * Checks if current {@link TestPlan} need registry center.
     */
    public boolean needRegistryCenter(TestPlan testPlan) {
        return testPlan.getRoots().stream()
            .flatMap(testIdentifier -> testPlan.getChildren(testIdentifier).stream())
            .filter(testIdentifier -> testIdentifier.getSource().isPresent())
            .filter(testIdentifier -> supportEmbeddedZookeeper(testIdentifier))
            .count() > 0;
    }

    /**
     * Checks if current {@link TestIdentifier} need registry center.
     */
    public boolean needRegistryCenter(TestIdentifier testIdentifier) {
        return supportEmbeddedZookeeper(testIdentifier);
    }

    /**
     * Checks if the current {@link TestIdentifier} need embedded zookeeper.
     */
    private boolean supportEmbeddedZookeeper(TestIdentifier testIdentifier) {
        if (!enableEmbeddedZookeeper) {
            return false;
        }
        TestSource testSource = testIdentifier.getSource().orElse(null);
        if (testSource instanceof ClassSource) {
            String packageName = ((ClassSource) testSource).getJavaClass().getPackage().getName();
            for (String pkgName : PACKAGE_NAME) {
                if (packageName.contains(pkgName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
