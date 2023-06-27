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
package org.apache.dubbo.qos.command.util;

import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SerializeCheckUtilsTest {
    @Test
    void testNotify() {
        FrameworkModel frameworkModel = new FrameworkModel();

        SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

        SerializeCheckUtils serializeCheckUtils = new SerializeCheckUtils(frameworkModel);

        ssm.addToAllowed("Test1234");
        Assertions.assertTrue(serializeCheckUtils.getAllowedList().contains("Test1234"));

        ssm.addToDisAllowed("Test4321");
        Assertions.assertTrue(serializeCheckUtils.getDisAllowedList().contains("Test4321"));

        ssm.setCheckSerializable(false);
        Assertions.assertFalse(serializeCheckUtils.isCheckSerializable());

        ssm.setCheckStatus(SerializeCheckStatus.DISABLE);
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, serializeCheckUtils.getStatus());

        frameworkModel.destroy();
    }
}
