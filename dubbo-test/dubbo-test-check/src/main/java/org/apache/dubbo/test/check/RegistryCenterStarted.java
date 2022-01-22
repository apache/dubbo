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

import org.apache.dubbo.test.check.registrycenter.GlobalRegistryCenter;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * The entrance to start the mocked registry center.
 */
public class RegistryCenterStarted extends AbstractRegistryCenterTestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            if (needRegistryCenter(testPlan)) {
                GlobalRegistryCenter.startup();
            }
        } catch (Throwable cause) {
            throw new IllegalStateException("Failed to start zookeeper instance in unit test", cause);
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        try {
            if (needRegistryCenter(testIdentifier)) {
                GlobalRegistryCenter.reset();
            }
        } catch (Throwable cause) {
            // ignore the exception
        }
    }
}
