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
package org.apache.dubbo.registry.nacos.util;

import com.alibaba.nacos.api.naming.pojo.Instance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test for NacosInstanceManageUtil
 */
public class NacosInstanceManageUtilTest {

    @Test
    public void testSetCorrespondingServiceNames() {
        String serviceName = "serviceName";
        Set<String> serviceNames = new HashSet<String>() {{
            add("serviceName1");
            add("serviceName2");
            add("serviceName3");
        }};
        NacosInstanceManageUtil.setCorrespondingServiceNames(serviceName, serviceNames);
        List<Instance> allCorrespondingServiceInstanceList = NacosInstanceManageUtil.getAllCorrespondingServiceInstanceList(serviceName);
        Assertions.assertEquals(0, allCorrespondingServiceInstanceList.size());
    }

    @Test
    public void testInitOrRefreshServiceInstanceList() {
        String serviceName = "serviceName";
        Set<String> serviceNames = new HashSet<String>() {{
            add("serviceName1");
            add("serviceName2");
            add("serviceName3");
        }};
        NacosInstanceManageUtil.setCorrespondingServiceNames(serviceName, serviceNames);

        Instance instance1 = new Instance();
        instance1.setInstanceId("1");
        Instance instance2 = new Instance();
        instance2.setInstanceId("2");
        Instance instance3 = new Instance();
        instance3.setInstanceId("3");

        List<Instance> instanceList = new ArrayList<Instance>() {{
            add(instance1);
            add(instance2);
            add(instance3);
        }};
        NacosInstanceManageUtil.initOrRefreshServiceInstanceList(serviceName, instanceList);
        List<Instance> allCorrespondingServiceInstanceList =
            NacosInstanceManageUtil.getAllCorrespondingServiceInstanceList(serviceName);
        Assertions.assertEquals(0, allCorrespondingServiceInstanceList.size());
    }

    @Test
    public void testGetAllCorrespondingServiceInstanceList() {
        String serviceName = "serviceName";
        Set<String> serviceNames = new HashSet<String>() {{
            add("serviceName");
            add("serviceName1");
        }};
        NacosInstanceManageUtil.setCorrespondingServiceNames(serviceName, serviceNames);

        Instance instance1 = new Instance();
        instance1.setInstanceId("1");
        Instance instance2 = new Instance();
        instance2.setInstanceId("2");
        Instance instance3 = new Instance();
        instance3.setInstanceId("3");

        List<Instance> instanceList = new ArrayList<Instance>() {{
            add(instance1);
            add(instance2);
            add(instance3);
        }};
        NacosInstanceManageUtil.initOrRefreshServiceInstanceList(serviceName, instanceList);
        List<Instance> allCorrespondingServiceInstanceList =
            NacosInstanceManageUtil.getAllCorrespondingServiceInstanceList(serviceName);
        Assertions.assertEquals(3, allCorrespondingServiceInstanceList.size());
    }
}
