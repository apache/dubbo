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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.utils.StringUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

class ScopeModelTest {

    @Test
    void testCreateOnDestroy() throws InterruptedException {
        FrameworkModel.destroyAll();

        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();

        List<Throwable> errors = new ArrayList<>();
        applicationModel.addDestroyListener(scopeModel -> {
            try {
                try {
                    applicationModel.getDefaultModule();
                    Assertions.fail("Cannot create new module after application model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("ApplicationModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }

                try {
                    applicationModel.newModule();
                    Assertions.fail("Cannot create new module after application model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("ApplicationModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }
            } catch (Throwable e) {
                errors.add(e);
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        frameworkModel.addDestroyListener(scopeModel -> {
            try {
                try {
                    frameworkModel.defaultApplication();
                    Assertions.fail("Cannot create new application after framework model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }

                try {
                    frameworkModel.newApplication();
                    Assertions.fail("Cannot create new application after framework model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }

                try {
                    ApplicationModel.defaultModel();
                    Assertions.fail("Cannot create new application after framework model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }

                try {
                    FrameworkModel.defaultModel().defaultApplication();
                    Assertions.fail("Cannot create new application after framework model destroyed");
                } catch (Exception e) {
                    Assertions.assertEquals("FrameworkModel is destroyed", e.getMessage(), StringUtils.toString(e));
                }
            } catch (Throwable ex) {
                errors.add(ex);
            } finally {
                latch.countDown();
            }
        });

        // destroy frameworkModel
        frameworkModel.destroy();
        latch.await();

        String errorMsg = null;
        for (Throwable throwable : errors) {
            errorMsg = StringUtils.toString(throwable);
            errorMsg += "\n";
        }
        Assertions.assertEquals(0, errors.size(), "Error occurred while destroy FrameworkModel: "+ errorMsg);

        // destroy all FrameworkModel
        FrameworkModel.destroyAll();
        List<String> remainFrameworks = FrameworkModel.getAllInstances().stream().map(m -> m.getDesc()).collect(Collectors.toList());
        Assertions.assertEquals(0, FrameworkModel.getAllInstances().size(), "FrameworkModel is not completely destroyed: " + remainFrameworks);
    }
}
