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
package org.apache.dubbo.registry.client.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.apache.dubbo.registry.client.migration.DefaultMigrationAddressComparator.NEW_ADDRESS_SIZE;
import static org.apache.dubbo.registry.client.migration.DefaultMigrationAddressComparator.OLD_ADDRESS_SIZE;

public class DefaultMigrationAddressComparatorTest {

    @Test
    public void test() {
        DefaultMigrationAddressComparator comparator = new DefaultMigrationAddressComparator();

        ClusterInvoker newInvoker = Mockito.mock(ClusterInvoker.class);
        ClusterInvoker oldInvoker = Mockito.mock(ClusterInvoker.class);
        Directory newDirectory = Mockito.mock(Directory.class);
        Directory oldDirectory = Mockito.mock(Directory.class);
        MigrationRule rule = Mockito.mock(MigrationRule.class);
        URL url = Mockito.mock(URL.class);

        Mockito.when(url.getDisplayServiceKey()).thenReturn("test");
        Mockito.when(newInvoker.getDirectory()).thenReturn(newDirectory);
        Mockito.when(oldInvoker.getDirectory()).thenReturn(oldDirectory);
        Mockito.when(newInvoker.getUrl()).thenReturn(url);
        Mockito.when(oldInvoker.getUrl()).thenReturn(url);

        Mockito.when(newInvoker.hasProxyInvokers()).thenReturn(false);
        Mockito.when(newDirectory.getAllInvokers()).thenReturn(Collections.emptyList());

        Assertions.assertFalse(comparator.shouldMigrate(newInvoker, oldInvoker, rule));
        Assertions.assertEquals(-1, comparator.getAddressSize("test").get(NEW_ADDRESS_SIZE));
        Assertions.assertEquals(0, comparator.getAddressSize("test").get(OLD_ADDRESS_SIZE));

        Mockito.when(newInvoker.hasProxyInvokers()).thenReturn(true);
        Mockito.when(oldInvoker.hasProxyInvokers()).thenReturn(false);
        Mockito.when(oldDirectory.getAllInvokers()).thenReturn(Collections.emptyList());

        Assertions.assertTrue(comparator.shouldMigrate(newInvoker, oldInvoker, rule));
        Assertions.assertEquals(0, comparator.getAddressSize("test").get(NEW_ADDRESS_SIZE));
        Assertions.assertEquals(-1, comparator.getAddressSize("test").get(OLD_ADDRESS_SIZE));

        Mockito.when(oldInvoker.hasProxyInvokers()).thenReturn(true);

        List<Invoker> newInvokerList = new LinkedList<>();
        newInvokerList.add(Mockito.mock(Invoker.class));
        newInvokerList.add(Mockito.mock(Invoker.class));
        newInvokerList.add(Mockito.mock(Invoker.class));
        Mockito.when(newDirectory.getAllInvokers()).thenReturn(newInvokerList);

        List<Invoker> oldInvokerList = new LinkedList<>();
        oldInvokerList.add(Mockito.mock(Invoker.class));
        oldInvokerList.add(Mockito.mock(Invoker.class));
        Mockito.when(oldDirectory.getAllInvokers()).thenReturn(oldInvokerList);

        Assertions.assertTrue(comparator.shouldMigrate(newInvoker, oldInvoker, null));

        Mockito.when(rule.getThreshold(url)).thenReturn(0.5f);
        newInvokerList.clear();
        newInvokerList.add(Mockito.mock(Invoker.class));
        Assertions.assertTrue(comparator.shouldMigrate(newInvoker, oldInvoker, rule));

        newInvokerList.clear();
        // hasProxyInvokers will check if invokers list is empty
        // if hasProxyInvokers return true, comparator will directly because default threshold is 0.0
        Assertions.assertTrue(comparator.shouldMigrate(newInvoker, oldInvoker, null));
        Assertions.assertFalse(comparator.shouldMigrate(newInvoker, oldInvoker, rule));

        Assertions.assertEquals(0, comparator.getAddressSize("test").get(NEW_ADDRESS_SIZE));
        Assertions.assertEquals(2, comparator.getAddressSize("test").get(OLD_ADDRESS_SIZE));
    }
}
