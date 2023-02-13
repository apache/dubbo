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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SerializeSecurityManagerTest {
    @Test
    void testPrefix() {
        TestAllowClassNotifyListener.setCount(0);
        SerializeSecurityManager ssm = new SerializeSecurityManager();
        ssm.registerListener(new TestAllowClassNotifyListener());

        ssm.addToAllowed("java.util.HashMap");
        ssm.addToAllowed("com.example.DemoInterface");
        ssm.addToAllowed("com.sun.Interface1");
        ssm.addToAllowed("com.sun.Interface2");

        Assertions.assertTrue(ssm.getAllowedPrefix().contains("java.util.HashMap"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.example.DemoInterface"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface2"));

        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getAllowedList());
        Assertions.assertEquals(7, TestAllowClassNotifyListener.getCount());

        ssm.addToDisAllowed("com.sun.Interface");
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface1"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface2"));
        Assertions.assertEquals(ssm.getDisAllowedPrefix(), TestAllowClassNotifyListener.getDisAllowedList());
        Assertions.assertEquals(9, TestAllowClassNotifyListener.getCount());

        ssm.addToAllowed("com.sun.Interface3");
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface3"));
        Assertions.assertEquals(9, TestAllowClassNotifyListener.getCount());

        ssm.addToAllowed("java.util.HashMap");
        Assertions.assertEquals(9, TestAllowClassNotifyListener.getCount());

        ssm.addToDisAllowed("com.sun.Interface");
        Assertions.assertEquals(9, TestAllowClassNotifyListener.getCount());

        ssm.addToAlwaysAllowed("com.sun.Interface3");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface3"));
        Assertions.assertEquals(10, TestAllowClassNotifyListener.getCount());

        ssm.addToAlwaysAllowed("com.sun.Interface3");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface3"));
        Assertions.assertEquals(10, TestAllowClassNotifyListener.getCount());
    }

    @Test
    void testStatus1() {
        SerializeSecurityManager ssm = new SerializeSecurityManager();
        ssm.registerListener(new TestAllowClassNotifyListener());

        Assertions.assertEquals(AllowClassNotifyListener.DEFAULT_STATUS, ssm.getCheckStatus());
        Assertions.assertEquals(AllowClassNotifyListener.DEFAULT_STATUS, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.STRICT);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.STRICT, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.WARN);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.WARN, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.STRICT);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.WARN, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.DISABLE);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.STRICT);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, TestAllowClassNotifyListener.getStatus());

        ssm.setCheckStatus(SerializeCheckStatus.WARN);
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.DISABLE, TestAllowClassNotifyListener.getStatus());
    }

    @Test
    void testStatus2() {
        SerializeSecurityManager ssm = new SerializeSecurityManager();

        ssm.setCheckStatus(SerializeCheckStatus.STRICT);
        ssm.registerListener(new TestAllowClassNotifyListener());
        Assertions.assertEquals(ssm.getCheckStatus(), TestAllowClassNotifyListener.getStatus());
        Assertions.assertEquals(SerializeCheckStatus.STRICT, TestAllowClassNotifyListener.getStatus());
    }

    @Test
    void testSerializable() {
        SerializeSecurityManager ssm = new SerializeSecurityManager();
        ssm.registerListener(new TestAllowClassNotifyListener());

        Assertions.assertTrue(ssm.isCheckSerializable());
        Assertions.assertTrue(TestAllowClassNotifyListener.isCheckSerializable());

        ssm.setCheckSerializable(true);
        Assertions.assertTrue(ssm.isCheckSerializable());
        Assertions.assertTrue(TestAllowClassNotifyListener.isCheckSerializable());

        ssm.setCheckSerializable(false);
        Assertions.assertFalse(ssm.isCheckSerializable());
        Assertions.assertFalse(TestAllowClassNotifyListener.isCheckSerializable());

        ssm.setCheckSerializable(true);
        Assertions.assertFalse(ssm.isCheckSerializable());
        Assertions.assertFalse(TestAllowClassNotifyListener.isCheckSerializable());
    }
}
