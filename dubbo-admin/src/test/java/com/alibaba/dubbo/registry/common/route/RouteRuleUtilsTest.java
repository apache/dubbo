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
package com.alibaba.dubbo.registry.common.route;

import com.alibaba.dubbo.registry.common.domain.Route;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RouteRuleUtilsTest {

    @Test
    public void test_expandCondition() throws Exception {
        Map<String, RouteRule.MatchPair> condition = new HashMap<String, RouteRule.MatchPair>();
        {
            // key1
            Set<String> matches = new HashSet<String>();
            matches.add("value1_1");
            matches.add("value1_2");
            matches.add("value1_3");
            Set<String> unmatches = new HashSet<String>();
            unmatches.add("v1_1");
            unmatches.add("v1_2");
            unmatches.add("v1_3");

            RouteRule.MatchPair p = new RouteRule.MatchPair(matches, unmatches);
            condition.put("key1", p);

            // key2 
            matches = new HashSet<String>();
            matches.add("value2_1");
            matches.add("value2_2");
            matches.add("value2_3");
            unmatches = new HashSet<String>();
            unmatches.add("v2_1");
            unmatches.add("v2_2");
            unmatches.add("v2_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            condition.put("key2", p);

            // key3 
            matches = new HashSet<String>();
            matches.add("value3_1");
            matches.add("value3_2");
            matches.add("value3_3");
            unmatches = new HashSet<String>();
            unmatches.add("v3_1");
            unmatches.add("v3_2");
            unmatches.add("v3_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            condition.put("key3 ", p);
        }
        Map<String, Set<String>> expandName2Set = new HashMap<String, Set<String>>();
        {
            expandName2Set.put("value1_1", new HashSet<String>(Arrays.asList("cat1", "dog1")));
            expandName2Set.put("value1_2", new HashSet<String>(Arrays.asList("cat2", "dog2")));
            expandName2Set.put("value1_3", new HashSet<String>(Arrays.asList("cat3", "dog3")));
            expandName2Set.put("v1_1", new HashSet<String>(Arrays.asList("pig1", "rat1")));
            expandName2Set.put("v1_2", new HashSet<String>(Arrays.asList("pig2", "rat2")));
        }

        Map<String, RouteRule.MatchPair> output = RouteRuleUtils.expandCondition(condition, "key1", "key2", expandName2Set);

        Map<String, RouteRule.MatchPair> expected = new HashMap<String, RouteRule.MatchPair>();
        {
            // key2 
            Set<String> matches = new HashSet<String>();
            matches.add("value2_1");
            matches.add("value2_2");
            matches.add("value2_3");

            matches.add("cat1");
            matches.add("cat2");
            matches.add("cat3");
            matches.add("dog1");
            matches.add("dog2");
            matches.add("dog3");

            Set<String> unmatches = new HashSet<String>();
            unmatches.add("v2_1");
            unmatches.add("v2_2");
            unmatches.add("v2_3");
            unmatches.add("v2_3");

            unmatches.add("pig1");
            unmatches.add("pig2");
            unmatches.add("rat1");
            unmatches.add("rat2");

            RouteRule.MatchPair p = new RouteRule.MatchPair(matches, unmatches);
            expected.put("key2", p);

            // key3 
            matches = new HashSet<String>();
            matches.add("value3_1");
            matches.add("value3_2");
            matches.add("value3_3");
            unmatches = new HashSet<String>();
            unmatches.add("v3_1");
            unmatches.add("v3_2");
            unmatches.add("v3_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            expected.put("key3 ", p);
        }

        assertEquals(expected, output);


        output = RouteRuleUtils.expandCondition(condition, "key1", "keyX", expandName2Set);

        expected = new HashMap<String, RouteRule.MatchPair>();
        {
            // key1
            Set<String> matches = new HashSet<String>();
            matches.add("cat1");
            matches.add("cat2");
            matches.add("cat3");
            matches.add("dog1");
            matches.add("dog2");
            matches.add("dog3");
            Set<String> unmatches = new HashSet<String>();
            unmatches.add("pig1");
            unmatches.add("pig2");
            unmatches.add("rat1");
            unmatches.add("rat2");

            RouteRule.MatchPair p = new RouteRule.MatchPair(matches, unmatches);
            expected.put("keyX", p);

            // key2 
            matches = new HashSet<String>();
            matches.add("value2_1");
            matches.add("value2_2");
            matches.add("value2_3");
            unmatches = new HashSet<String>();
            unmatches.add("v2_1");
            unmatches.add("v2_2");
            unmatches.add("v2_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            expected.put("key2", p);

            // key3 
            matches = new HashSet<String>();
            matches.add("value3_1");
            matches.add("value3_2");
            matches.add("value3_3");
            unmatches = new HashSet<String>();
            unmatches.add("v3_1");
            unmatches.add("v3_2");
            unmatches.add("v3_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            expected.put("key3 ", p);
        }


        output = RouteRuleUtils.expandCondition(condition, "keyNotExsited", "key3", expandName2Set);
        assertSame(condition, output);
    }

    @Test
    public void test_filterServiceByRule() throws Exception {
        List<String> services = new ArrayList<String>();
        services.add("com.alibaba.MemberService");
        services.add("com.alibaba.ViewCacheService");
        services.add("com.alibaba.PC2Service");
        services.add("service2");

        Route route = new Route();
        route.setMatchRule("service=com.alibaba.*,AuthService&service!=com.alibaba.DomainService,com.alibaba.ViewCacheService&consumer.host!=127.0.0.1,15.11.57.6&consumer.version = 2.0.0&consumer.version != 1.0.0");
        route.setFilterRule("provider.application=morgan,pc2&provider.host=10.16.26.51&provider.port=1020");
        RouteRule rr = RouteRule.parse(route);
        Collection<String> changedService = RouteRuleUtils.filterServiceByRule(services, rr);
        assertEquals(3, changedService.size());
    }

    @Test
    public void test_isMatchCondition() throws Exception {
        Map<String, RouteRule.MatchPair> condition = new HashMap<String, RouteRule.MatchPair>();
        Map<String, String> valueParams = new HashMap<String, String>();
        Map<String, String> kv = new HashMap<String, String>();

        boolean output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertTrue(output);

        {
            // key1
            Set<String> matches = new HashSet<String>();
            matches.add("value1_1");
            matches.add("value1_2");
            matches.add("value1_3");
            Set<String> unmatches = new HashSet<String>();
            unmatches.add("v1_1");
            unmatches.add("v1_2");
            unmatches.add("v1_3");

            RouteRule.MatchPair p = new RouteRule.MatchPair(matches, unmatches);
            condition.put("key1", p);

            // key2 
            matches = new HashSet<String>();
            matches.add("value2_1");
            matches.add("value2_2");
            matches.add("value2_3");
            unmatches = new HashSet<String>();
            unmatches.add("v2_1");
            unmatches.add("v2_2");
            unmatches.add("v2_3");

            p = new RouteRule.MatchPair(matches, unmatches);
            condition.put("key2", p);
        }

        kv.put("kkk", "vvv");
        output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertFalse(output);

        kv.put("key1", "vvvXXX");
        output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertFalse(output);

        kv.put("key1", "value1_1");
        output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertFalse(output);

        kv.put("key2", "value2_1");
        output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertTrue(output);

        kv.put("key1", "v1_2");
        output = RouteRuleUtils.isMatchCondition(condition, valueParams, kv);
        assertFalse(output);
    }

    @Test
    public void test_filterServiceByRule2() throws Exception {
        List<String> services = new ArrayList<String>();
        services.add("com.alibaba.MemberService");
        services.add("com.alibaba.ViewCacheService");
        services.add("com.alibaba.PC2Service");
        services.add("service2");

        Route route = new Route();
        route.setMatchRule("service=com.alibaba.PC2Service&service!=com.alibaba.DomainService,com.alibaba.ViewCacheService&consumer.host!=127.0.0.1,15.11.57.6&consumer.version = 2.0.0&consumer.version != 1.0.0");
        route.setFilterRule("provider.application=morgan,pc2&provider.host=10.16.26.51&provider.port=1020");
        RouteRule rr = RouteRule.parse(route);
        Collection<String> changedService = RouteRuleUtils.filterServiceByRule(services, rr);
        assertEquals(2, changedService.size());
    }
}
