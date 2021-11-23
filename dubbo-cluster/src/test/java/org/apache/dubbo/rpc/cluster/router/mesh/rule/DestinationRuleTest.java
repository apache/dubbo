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

import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.DestinationRule;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.destination.loadbalance.SimpleLB;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.VirtualServiceRule;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.DESTINATION_RULE_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.KIND_KEY;
import static org.apache.dubbo.rpc.cluster.router.mesh.route.MeshRuleConstants.VIRTUAL_SERVICE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class DestinationRuleTest {

    @Test
    public void parserTest() {
        Yaml yaml = new Yaml();
        DestinationRule destinationRule = yaml.loadAs(this.getClass().getClassLoader().getResourceAsStream("DestinationRuleTest.yaml"), DestinationRule.class);

        System.out.println(destinationRule);


//        apiVersion: service.dubbo.apache.org/v1alpha1
//        kind: DestinationRule
//        metadata: { name: demo-route }
//        spec:
//        host: demo
//        subsets:
//        - labels: { env-sign: xxx,tag1: hello }
//        name: isolation
//                - labels: { env-sign: yyy }
//        name: testing-trunk
//                - labels: { env-sign: zzz }
//        name: testing


        assertEquals("service.dubbo.apache.org/v1alpha1", destinationRule.getApiVersion());
        assertEquals(DESTINATION_RULE_KEY, destinationRule.getKind());
        assertEquals("demo-route", destinationRule.getMetadata().get("name"));
        assertEquals("demo", destinationRule.getSpec().getHost());
        assertEquals(3, destinationRule.getSpec().getSubsets().size());

        assertEquals("isolation", destinationRule.getSpec().getSubsets().get(0).getName());
        assertEquals(2, destinationRule.getSpec().getSubsets().get(0).getLabels().size());
        assertEquals("xxx", destinationRule.getSpec().getSubsets().get(0).getLabels().get("env-sign"));
        assertEquals("hello", destinationRule.getSpec().getSubsets().get(0).getLabels().get("tag1"));


        assertEquals("testing-trunk", destinationRule.getSpec().getSubsets().get(1).getName());
        assertEquals(1, destinationRule.getSpec().getSubsets().get(1).getLabels().size());
        assertEquals("yyy", destinationRule.getSpec().getSubsets().get(1).getLabels().get("env-sign"));


        assertEquals("testing", destinationRule.getSpec().getSubsets().get(2).getName());
        assertEquals(1, destinationRule.getSpec().getSubsets().get(2).getLabels().size());
        assertEquals("zzz", destinationRule.getSpec().getSubsets().get(2).getLabels().get("env-sign"));

        assertEquals(SimpleLB.ROUND_ROBIN, destinationRule.getSpec().getTrafficPolicy().getLoadBalancer().getSimple());
        assertEquals(null, destinationRule.getSpec().getTrafficPolicy().getLoadBalancer().getConsistentHash());
    }


    @Test
    public void parserMultiRuleTest() {
        Yaml yaml = new Yaml();
        Yaml yaml2 = new Yaml();
        Iterable objectIterable = yaml.loadAll(this.getClass().getClassLoader().getResourceAsStream("DestinationRuleTest2.yaml"));
        for (Object result : objectIterable) {

            Map resultMap = (Map) result;
            if (resultMap.get("kind").equals(DESTINATION_RULE_KEY)) {
                DestinationRule destinationRule = yaml2.loadAs(yaml2.dump(result), DestinationRule.class);
                System.out.println(destinationRule);
                assertNotNull(destinationRule);
            } else if (resultMap.get(KIND_KEY).equals(VIRTUAL_SERVICE_KEY)) {
                VirtualServiceRule virtualServiceRule = yaml2.loadAs(yaml2.dump(result), VirtualServiceRule.class);
                System.out.println(virtualServiceRule);
                assertNotNull(virtualServiceRule);
            }
        }
    }

}
