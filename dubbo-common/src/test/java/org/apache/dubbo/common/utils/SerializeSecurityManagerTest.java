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

import org.apache.dubbo.rpc.model.FrameworkModel;

import com.service.DemoService1;
import com.service.DemoService2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class SerializeSecurityManagerTest {
    @Test
    public void test() {
        SerializeSecurityManager ssm = new SerializeSecurityManager(FrameworkModel.defaultModel());
        ssm.registerListener(new TestAllowClassNotifyListener());
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("java.util.HashMap"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.example.DemoInterface"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.sun.Interface1"));
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface2"));

        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());
    }

    @Test
    public void addToAllow() {
        SerializeSecurityManager ssm = new SerializeSecurityManager(FrameworkModel.defaultModel());
        ssm.registerListener(new TestAllowClassNotifyListener());
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface2"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());

        ssm.addToAllow("com.sun.Interface2");
        Assertions.assertFalse(ssm.getAllowedPrefix().contains("com.sun.Interface2"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());

        ssm.addToAllow("java.util.Interface1");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("java.util.Interface1"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());

        ssm.addToAllow("java.util.package.Interface1");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("java.util.package.Interface1"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());

        ssm.addToAllow("com.example.Interface2");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.example.Interface2"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());

        ssm.addToAllow("com.example.package.Interface1");
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.example.package"));
        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());
    }

    @Test
    public void testRegister1() {
        SerializeSecurityManager ssm = new SerializeSecurityManager(FrameworkModel.defaultModel());
        ssm.registerListener(new TestAllowClassNotifyListener());

        ssm.registerInterface(DemoService1.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.DemoService1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo3"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo4"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo5"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo6"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo7"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo8"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Simple"));

        Assertions.assertTrue(ssm.getAllowedPrefix().contains(List.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Set.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Map.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(LinkedList.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Vector.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(HashSet.class.getName()));

        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());
    }


    @Test
    public void testRegister2() {
        SerializeSecurityManager ssm = new SerializeSecurityManager(FrameworkModel.defaultModel());
        ssm.registerListener(new TestAllowClassNotifyListener());

        ssm.registerInterface(DemoService2.class);
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.service.DemoService2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo1"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo2"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo3"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo4"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo5"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo6"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo7"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Demo8"));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains("com.pojo.Simple"));

        Assertions.assertTrue(ssm.getAllowedPrefix().contains(List.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Set.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Map.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(LinkedList.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(Vector.class.getName()));
        Assertions.assertTrue(ssm.getAllowedPrefix().contains(HashSet.class.getName()));

        Assertions.assertEquals(ssm.getAllowedPrefix(), TestAllowClassNotifyListener.getPrefixList());
    }
}
