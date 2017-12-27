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

import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 */
public class RouteRuleUtils {
    private RouteRuleUtils() {
    }

    /**
     * When one of the value that is bound to a specific key of a condition is expanded, it is merged into another value of a specified key.
     * @param <T> generic type
     * @param condition
     * @param srcKeyName the key to expand
     * @param destKeyName the key to merge into
     * @param expandName2Set the mapping of values to values that are carried out
     */
    public static <T extends Collection<String>> Map<String, RouteRule.MatchPair> expandCondition(
            Map<String, RouteRule.MatchPair> condition, String srcKeyName, String destKeyName,
            Map<String, T> expandName2Set) {
        if (null == condition || StringUtils.isEmpty(srcKeyName) || StringUtils.isEmpty(destKeyName)) {
            return condition;
        }

        RouteRule.MatchPair matchPair = condition.get(srcKeyName);
        if (matchPair == null) {
            return condition;
        }

        Map<String, RouteRule.MatchPair> ret = new HashMap<String, RouteRule.MatchPair>();

        Iterator<Entry<String, RouteRule.MatchPair>> iterator = condition.entrySet().iterator();
        for (; iterator.hasNext(); ) {
            Entry<String, RouteRule.MatchPair> entry = iterator.next();
            String condName = entry.getKey();

            // Neither source nor destination
            if (!condName.equals(srcKeyName) && !condName.equals(destKeyName)) {
                RouteRule.MatchPair p = entry.getValue();
                if (p != null) ret.put(condName, p);
            }
            // equals with source
            else if (condName.equals(srcKeyName)) {
                RouteRule.MatchPair from = condition.get(srcKeyName);
                RouteRule.MatchPair to = condition.get(destKeyName);

                // no items to Expand
                if (from == null || from.getMatches().isEmpty() && from.getUnmatches().isEmpty()) {
                    if (to != null) ret.put(destKeyName, to);
                    continue;
                }

                Set<String> matches = new HashSet<String>();
                Set<String> unmatches = new HashSet<String>();
                // add items from source Expand key
                for (String s : from.getMatches()) {
                    if (expandName2Set == null || !expandName2Set.containsKey(s)) continue;

                    matches.addAll(expandName2Set.get(s));
                }
                for (String s : from.getUnmatches()) {
                    if (expandName2Set == null || !expandName2Set.containsKey(s)) continue;

                    unmatches.addAll(expandName2Set.get(s));
                }
                // add the original items
                if (to != null) {
                    matches.addAll(to.getMatches());
                    unmatches.addAll(to.getUnmatches());
                }

                ret.put(destKeyName, new RouteRule.MatchPair(matches, unmatches));
            }
            // else, it must be Key == destKeyName, do nothing
        }

        return ret;
    }

    /**
     * Check whether the KV (key=value pair of Provider or Consumer) matches the conditions.
     *
     * @param condition can contains variable definition. For example, <code>{key1={matches={value1,value2,$var1},unmatches={Vx,Vy,$var2}}}</code>
     * @param valueParams Set of values of interpolated variables in a condition
     * @param kv key=value pair of Provider or Consumer
     * @see RouteRule
     */
    public static boolean isMatchCondition(Map<String, RouteRule.MatchPair> condition,
                                           Map<String, String> valueParams, Map<String, String> kv) {
        if (condition != null && condition.size() > 0) {
            for (Map.Entry<String, RouteRule.MatchPair> entry : condition.entrySet()) {
                String condName = entry.getKey();
                RouteRule.MatchPair p = entry.getValue();
                String value = kv.get(condName);
                Set<String> matches = p.getMatches();
                if (matches != null && matches.size() > 0
                        && !ParseUtils.isMatchGlobPatternsNeedInterpolate(matches, valueParams, value)) { // if V is null, return false
                    // don't match matches
                    return false;
                }
                Set<String> unmatches = p.getUnmatches();
                if (unmatches != null && unmatches.size() > 0
                        && ParseUtils.isMatchGlobPatternsNeedInterpolate(unmatches, valueParams, value)) {
                    // match unmatches
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Return services that can match When Condition in Route Rule, use Glob Pattern.
     */
    public static Set<String> filterServiceByRule(List<String> services, RouteRule rule) {
        if (null == services || services.isEmpty() || rule == null) {
            return new HashSet<String>();
        }

        RouteRule.MatchPair p = rule.getWhenCondition().get("service");
        if (p == null) {
            return new HashSet<String>();
        }

        Set<String> filter = ParseUtils.filterByGlobPattern(p.getMatches(), services);
        Set<String> set = ParseUtils.filterByGlobPattern(p.getUnmatches(), services);
        filter.addAll(set);
        return filter;
    }
}
