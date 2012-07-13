/**
 * Project: dubbo.registry.server
 * 
 * File Created at Oct 20, 2010
 * $Id: RouteRuleUtils.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.route;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.alibaba.dubbo.common.utils.StringUtils;

/**
 * @author william.liangf
 * @author ding.lid
 */
public class RouteRuleUtils {
    private RouteRuleUtils() {}
    
    /**
     * 把条件的一个键值展开后，合并到另外指定的键值中。
     * @param <T> 集合类型
     * @param condition 条件
     * @param srcKeyName 要展开的键值
     * @param destKeyName 合并到的键值
     * @param expandName2Set 进行展开的值到值的映射
     */
    public static <T extends Collection<String>> Map<String, RouteRule.MatchPair> expandCondition(
            Map<String, RouteRule.MatchPair> condition, String srcKeyName, String destKeyName,
            Map<String, T> expandName2Set) {
        if(null == condition || StringUtils.isEmpty(srcKeyName) || StringUtils.isEmpty(destKeyName)) {
            return condition;
        }
        
        RouteRule.MatchPair matchPair = condition.get(srcKeyName);
        if(matchPair == null) {
            return condition;
        }
    
        Map<String, RouteRule.MatchPair> ret = new HashMap<String, RouteRule.MatchPair>();
        
        Iterator<Entry<String, RouteRule.MatchPair>> iterator = condition.entrySet().iterator();
        for(; iterator.hasNext();) {
            Entry<String, RouteRule.MatchPair> entry = iterator.next();
            String condName = entry.getKey();
            
            // 即不是源也不目的
            if(!condName.equals(srcKeyName) && !condName.equals(destKeyName)) {
                RouteRule.MatchPair p = entry.getValue();
                if(p != null) ret.put(condName, p);
            }
            // 等于源
            else if(condName.equals(srcKeyName)) {
                RouteRule.MatchPair from = condition.get(srcKeyName);
                RouteRule.MatchPair to = condition.get(destKeyName);
                
                // 没有可Expand条目
                if(from == null || from.getMatches().isEmpty() && from.getUnmatches().isEmpty()) {
                    if(to != null) ret.put(destKeyName, to);
                    continue;
                }
                
                Set<String> matches = new HashSet<String>();
                Set<String> unmatches = new HashSet<String>();
                // 添加上Expand来的条目
                for(String s : from.getMatches()) {
                    if(expandName2Set == null || !expandName2Set.containsKey(s)) continue;
                    
                    matches.addAll(expandName2Set.get(s));
                }
                for(String s : from.getUnmatches()) {
                    if(expandName2Set == null || !expandName2Set.containsKey(s)) continue;
                    
                    unmatches.addAll(expandName2Set.get(s));
                }
                // 添加原来的条目
                if(to != null) {
                    matches.addAll(to.getMatches());
                    unmatches.addAll(to.getUnmatches());
                }
                
                ret.put(destKeyName, new RouteRule.MatchPair(matches, unmatches));
            }
            // else 是 Key == destKeyName 的情况，无操作
        }
        
        return ret;
    }
    
    /**
     * 判断KV(即条件对应的样本)是否符合条件。
     * 
     * @param condition 条件，可以包含变量声明。 如<code>{key1={matches={value1,value2,$var1},unmatches={Vx,Vy,$var2}}}</code>
     * @param valueParams 条件中插值变量的值集合
     * @param kv 校验条件的样本
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
	                    && ! ParseUtils.isMatchGlobPatternsNeedInterpolate(matches, valueParams, value)) { // 如何Value为null，返回False
	                // 不满足 匹配
	                return false;
	            }
	            Set<String> unmatches = p.getUnmatches();
	            if (unmatches != null && unmatches.size() > 0
	                    && ParseUtils.isMatchGlobPatternsNeedInterpolate(unmatches, valueParams, value)) {
	                // 满足了 不匹配
	                return false;
	            }
	        }
    	}
        return true;
    }


    /**
     * 返回被RouteRule的When的service匹配到的Service。使用Glob匹配。
     */
    public static Set<String> filterServiceByRule(List<String> services, RouteRule rule) {
        if(null == services || services.isEmpty() || rule == null) {
            return new HashSet<String>();
        }
        
        RouteRule.MatchPair p = rule.getWhenCondition().get("service");
        if(p == null) {
            return new HashSet<String>();
        }
        
        Set<String> filter = ParseUtils.filterByGlobPattern(p.getMatches(), services);
        Set<String> set = ParseUtils.filterByGlobPattern(p.getUnmatches(), services);
        filter.addAll(set);
        return filter;
    }
}
