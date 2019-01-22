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
package org.apache.dubbo.rpc.cluster;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class RouterTest {

    private static List<Router> routers = new ArrayList<>();

    @BeforeClass
    public static void setUp () {
        CompatibleRouter compatibleRouter = new CompatibleRouter();
        routers.add(compatibleRouter);
        CompatibleRouter2 compatibleRouter2 = new CompatibleRouter2();
        routers.add(compatibleRouter2);
        NewRouter newRouter = new NewRouter();
        routers.add(newRouter);
    }

    @Test
    public void testCompareTo () {
        try {
            Collections.sort(routers);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertFalse(false);
        }
    }
}
