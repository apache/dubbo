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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The abstract implementation provides the basic methods. <p></p>
 * {@link #needRegistryCenter(TestPlan)}: checks if current {@link TestPlan} need registry center.
 */
public abstract class AbstractRegistryCenterTestExecutionListener implements TestExecutionListener {

    /**
     * The registry center should start
     * if we want to run the test cases in the given package.
     */
    private static final Set<String> PACKAGE_NAME = new HashSet<>();

    /**
     * The unique id for the engine.
     */
    private static final String ENGINE_UNIQUE_ID = "[engine:junit-jupiter]";

    static {
        // dubbo-config module
        PACKAGE_NAME.add("org.apache.dubbo.config");
        // dubbo-test
        PACKAGE_NAME.add("org.apache.dubbo.test");
    }

    /**
     * Checks if current {@link TestPlan} need registry center.
     */
    public boolean needRegistryCenter(TestPlan testPlan) {
        if (testPlan.containsTests()) {
            TestIdentifier engineTestIdentifier = this.getEngineTestIdentifier(testPlan.getRoots());
            TestIdentifier childTestIdentifier = this.getFirstTestIdentifier(testPlan.getChildren(engineTestIdentifier));
            TestSource testSource = childTestIdentifier.getSource().orElse(null);
            if (testSource instanceof ClassSource) {
                String packageName = ((ClassSource) testSource).getJavaClass().getPackage().getName();
                for (String pkgName : PACKAGE_NAME) {
                    if (packageName.contains(pkgName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the {@link TestIdentifier} of the test engine.
     */
    private TestIdentifier getEngineTestIdentifier(Set<TestIdentifier> testIdentifiers) {
        for (TestIdentifier testIdentifier : testIdentifiers) {
            if (ENGINE_UNIQUE_ID.equals(testIdentifier.getUniqueId())){
                return testIdentifier;
            }
        }
        return null;
    }

    /**
     * Returns the first {@link TestIdentifier} from the given collections.
     *
     * @param testIdentifiers the collection of {@link TestIdentifier}s
     * @return the first {@link TestIdentifier}
     */
    private TestIdentifier getFirstTestIdentifier(Collection<TestIdentifier> testIdentifiers) {
        if (testIdentifiers != null
            && !testIdentifiers.isEmpty()) {
            for (TestIdentifier testIdentifier : testIdentifiers) {
                return testIdentifier;
            }
        }
        throw new IllegalArgumentException("The collection of TestIdentifier cannot be null or empty");
    }
}
