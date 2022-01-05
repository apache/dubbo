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

package org.apache.dubbo.rpc.cluster.router.mesh.rule;

import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRoute;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.DubboRouteDetail;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceRule;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


public class VirtualServiceRuleTest {

    @Test
    public void parserTest() {
        Yaml yaml = new Yaml();
        VirtualServiceRule virtualServiceRule = yaml.loadAs(this.getClass().getClassLoader().getResourceAsStream("VirtualServiceTest.yaml"), VirtualServiceRule.class);

        System.out.println(virtualServiceRule);
        assertNotNull(virtualServiceRule);

        assertEquals("service.dubbo.apache.org/v1alpha1", virtualServiceRule.getApiVersion());
        assertEquals("VirtualService", virtualServiceRule.getKind());
        assertEquals("demo-route", virtualServiceRule.getMetadata().get("name"));

        List<String> hosts = virtualServiceRule.getSpec().getHosts();
        assertEquals(1, hosts.size());
        assertEquals("demo", hosts.get(0));

        List<DubboRoute> dubboRoutes = virtualServiceRule.getSpec().getDubbo();
        assertEquals(1, dubboRoutes.size());

        DubboRoute dubboRoute = dubboRoutes.get(0);
        assertNull(dubboRoute.getName());

        assertEquals(1, dubboRoute.getServices().size());
        assertEquals("ccc", dubboRoute.getServices().get(0).getRegex());

        List<DubboRouteDetail> routedetail = dubboRoute.getRoutedetail();
        DubboRouteDetail firstDubboRouteDetail = routedetail.get(0);
        DubboRouteDetail secondDubboRouteDetail = routedetail.get(1);
        DubboRouteDetail thirdDubboRouteDetail = routedetail.get(2);

        assertEquals("xxx-project", firstDubboRouteDetail.getName());
        assertEquals("xxx", firstDubboRouteDetail.getMatch().get(0).getSourceLabels().get("trafficLabel"));
        assertEquals("demo", firstDubboRouteDetail.getRoute().get(0).getDestination().getHost());
        assertEquals("isolation", firstDubboRouteDetail.getRoute().get(0).getDestination().getSubset());

        assertEquals("testing-trunk", secondDubboRouteDetail.getName());
        assertEquals("testing-trunk", secondDubboRouteDetail.getMatch().get(0).getSourceLabels().get("trafficLabel"));
        assertEquals("demo", secondDubboRouteDetail.getRoute().get(0).getDestination().getHost());
        assertEquals("testing-trunk", secondDubboRouteDetail.getRoute().get(0).getDestination().getSubset());

        assertEquals("testing", thirdDubboRouteDetail.getName());
        assertNull(thirdDubboRouteDetail.getMatch());
        assertEquals("demo", thirdDubboRouteDetail.getRoute().get(0).getDestination().getHost());
        assertEquals("testing", thirdDubboRouteDetail.getRoute().get(0).getDestination().getSubset());
    }

}
