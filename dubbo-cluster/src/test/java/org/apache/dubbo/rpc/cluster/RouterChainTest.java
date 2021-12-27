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


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.cluster.filter.DemoService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;

public class RouterChainTest {

    /**
     * verify the router and state router loaded by default
     */
    @Test
    public void testBuildRouterChain() {

        Map<String, String> parameters = new HashMap<>();
        parameters.put(INTERFACE_KEY, DemoService.class.getName());
        parameters.put("registry", "zookeeper");
        URL url = new ServiceConfigURL("dubbo",
            "127.0.0.1",
            20881,
            DemoService.class.getName(),
            parameters);

        RouterChain<DemoService> routerChain = RouterChain.buildChain(DemoService.class, url);
        Assertions.assertEquals(0, routerChain.getRouters().size());
        Assertions.assertEquals(5, routerChain.getStateRouters().size());
    }
}
