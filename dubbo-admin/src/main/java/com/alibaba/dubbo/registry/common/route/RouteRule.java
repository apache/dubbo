/**
 * Project: dubbo.registry.server
 * 
 * File Created at Oct 18, 2010
 * $Id: RouteRule.java 182348 2012-06-27 09:16:58Z tony.chenl $
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

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.common.domain.Route;


/**
 * Rule分成两部分，When条件和Then条件。<br>
 * 条件是一组键值（KV）对，表示匹配条件。条件包含的键值中的“值”（Value）可以有多个值，即是一个列表。<br>
 * Rule的含义是，符合了When条件后，则进行Then条件的过虑。<br>
 * 当然被When条件的符合的、被Then条件过滤的，也是一组KV，术语上我们就称为样本（Sample）吧。<br>
 * 使用条件对样本进行的匹配的过程称为“过滤”（或称为“筛选”）（Filter）。
 * 使用When条件过滤和使用Then条件过滤的样本，不需要是相同的集合。如在Dubbo中，分别对应的是Consumer和Provider。
 * 对于RouteRule（路由规则）含义即，符合When条件的Consumer，则对Provider进行Then过滤，出来的Provide即是提供给这个Consumer的Provider。<br>
 * 
 * Rule的字符串格式如下：<code>
 * key1 = value11,value12 & key2 = value21 & key2 != value22 => key3 = value3 & key4 = value41,vlaue42 & key5 !=value51
 * </code>。
 * <code>=></code>之前的称为When条件，是KV对；之后是Then条件，是KV对。KV的Value可以有多个值。<br><br>
 * 
 * 值对象，线程安全。
 * 
 * @author william.liangf
 * @author ding.lid
 */
public class RouteRule {
    public static class MatchPair {
        Set<String> matches   = new HashSet<String>();
        Set<String> unmatches = new HashSet<String>();

        public MatchPair() {
        }
        
        public MatchPair(Set<String> matches, Set<String> unmatches) {
            if(matches == null || unmatches == null) {
                throw new IllegalArgumentException("argument of MatchPair is null!");
            }
            
            this.matches = matches;
            this.unmatches = unmatches;
        }
        
        public Set<String> getMatches() {
            return matches;
        }
        
        public Set<String> getUnmatches() {
            return unmatches;
        }
        
        public MatchPair copy() {
            MatchPair ret = new MatchPair();
            ret.matches.addAll(matches);
            ret.unmatches.addAll(unmatches);
            return ret;
        }
        
        private volatile boolean freezed = false;
        void freeze() {
            if(freezed) return;
            synchronized (this) {
                if(freezed) return;
                matches = Collections.unmodifiableSet(matches);
                unmatches = Collections.unmodifiableSet(unmatches);
            }
        }
        
        public boolean containeValue(String value) {
            return matches.contains(value) || unmatches.contains(value);
        }
        
        /**
         * 给定的值是否通过该{@link MatchPair}匹配。<p>
         * 返回{@code false}，如果
         * <ol>
         * <li>value在unmatches列表中
         * <li>matches列表有值，但value不在matches列表中。
         * </ol>
         * otherwise返回<code>true</code>。
         */
        public boolean pass(String sample) {
            if(unmatches.contains(sample)) return false;
            if(matches.isEmpty()) return true;
            return matches.contains(sample);
        }
        
        @Override
        public String toString() {
            return String.format("{matches=%s,unmatches=%s}", matches.toString(), unmatches.toString());
        }
        
        // 用Eclipse自动生成
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((matches == null) ? 0 : matches.hashCode());
            result = prime * result + ((unmatches == null) ? 0 : unmatches.hashCode());
            return result;
        }

        // 用Eclipse自动生成
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MatchPair other = (MatchPair) obj;
            if (matches == null) {
                if (other.matches != null)
                    return false;
            } else if (!matches.equals(other.matches))
                return false;
            if (unmatches == null) {
                if (other.unmatches != null)
                    return false;
            } else if (!unmatches.equals(other.unmatches))
                return false;
            return true;
        }
    }

    final Map<String, MatchPair> whenCondition;
    final Map<String, MatchPair> thenCondition;

    private static Pattern ROUTE_PATTERN = Pattern.compile("([&!=,]*)\\s*([^&!=,\\s]+)");
    
    private static Pattern CONDITION_SEPERATOR = Pattern.compile("(.*)=>(.*)");
    
    public static Map<String, MatchPair> parseRule(String rule)
            throws ParseException {
    	Map<String, MatchPair> condition = new HashMap<String, RouteRule.MatchPair>();
        if(StringUtils.isBlank(rule)) {
            return condition;
        }        
        // 匹配或不匹配Key-Value对
        MatchPair pair = null;
        // 多个Value值
        Set<String> values = null;
        final Matcher matcher = ROUTE_PATTERN.matcher(rule);
        while (matcher.find()) { // 逐个匹配
            String separator = matcher.group(1);
            String content = matcher.group(2);
            // 表达式开始
            if (separator == null || separator.length() == 0) {
                pair = new MatchPair();
                condition.put(content, pair);
            }
            // KV开始
            else if ("&".equals(separator)) {
                if (condition.get(content) == null) {
                    pair = new MatchPair();
                    condition.put(content, pair);
                } else {
                    condition.put(content, pair);
                }

            }
            // KV的Value部分开始
            else if ("=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());

                values = pair.matches;
                values.add(content);
            }
            // KV的Value部分开始
            else if ("!=".equals(separator)) {
                if (pair == null)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());

                values = pair.unmatches;
                values.add(content);
            }
            // KV的Value部分的多个条目
            else if (",".equals(separator)) { // 如果为逗号表示
                if (values == null || values.size() == 0)
                    throw new ParseException("Illegal route rule \""
                            + rule + "\", The error char '" + separator
                            + "' at index " + matcher.start() + " before \""
                            + content + "\".", matcher.start());
                values.add(content);
            } else {
                throw new ParseException("Illegal route rule \"" + rule
                        + "\", The error char '" + separator + "' at index "
                        + matcher.start() + " before \"" + content + "\".", matcher.start());
            }
        }
        return condition;
    }
    
    // FIXME 集合都要加上unmodified的Wrapper，避免构造后的对象被修改
    private RouteRule(Map<String, MatchPair> when, Map<String, MatchPair> then) {
        for(Map.Entry<String, MatchPair> entry : when.entrySet()) {
            entry.getValue().freeze();
        }
        for(Map.Entry<String, MatchPair> entry : then.entrySet()) {
            entry.getValue().freeze();
        }
        
        // NOTE: When条件是允许为空的，外部业务来保证类似的约束条件
        this.whenCondition = when;
        this.thenCondition = then;
    }

    @SuppressWarnings("unchecked")
    static RouteRule EMPTY = new RouteRule(Collections.EMPTY_MAP, Collections.EMPTY_MAP); 
    
    /**
     * 把字符串形式的RouteRule的解析成对象。
     * 
     * @throws ParseException RouteRule字符串格式不对了。以下输入的情况，RouteRule都是非法的。
     * <ul> <li> 输入是<code>null</code>。
     * <li> 输入是空串，或是空白串。
     * <li> 输入的Rule，没有When条件
     * <li> 输入的Rule，没有Then条件
     * </ul>
     */
    public static RouteRule parse(Route route) throws ParseException {
        if(route == null) 
            throw new ParseException("null route!", 0);
        
        if(route.getMatchRule() == null && route.getFilterRule() == null) {
            return parse(route.getRule());
        }
        
        return parse(route == null ? null : route.getMatchRule(), route == null ? null : route.getFilterRule());
    }
    
    public static RouteRule parse(String whenRule, String thenRule) throws ParseException {
    	/*if (whenRule == null || whenRule.trim().length() == 0) {
    		throw new ParseException("Illegal route rule without when express", 0);
    	}*/
    	if (thenRule == null || thenRule.trim().length() == 0) {
    		throw new ParseException("Illegal route rule without then express", 0);
    	}
    	Map<String, MatchPair> when = parseRule(whenRule.trim());
        Map<String, MatchPair> then = parseRule(thenRule.trim());
        return new RouteRule(when, then);
    }
    
    public static RouteRule parse(String rule) throws ParseException {
        if(StringUtils.isBlank(rule)) {
            throw new ParseException("Illegal blank route rule", 0);
        }
         
        final Matcher matcher = CONDITION_SEPERATOR.matcher(rule);
        if(!matcher.matches()) throw new ParseException("condition seperator => not found!", 0); 
        
        return parse(matcher.group(1), matcher.group(2));
    }
    
    /**
     * @see #parse(String)
     * @throws RuntimeException 解析出错时，Wrap了{@link #parse(String)}方法的抛出的{@link ParseException}的异常。
     */
    public static RouteRule parseQuitely(Route route) {
        try {
            return parse(route);
        } catch (ParseException e) {
           throw new RuntimeException(e);
        }
    }
    
    private static Pattern VALUE_LIST_SEPARATOR = Pattern.compile("\\s*,\\s*");
    
    static Map<String, MatchPair> parseNameAndValueListString2Condition(Map<String, String> params, Map<String, String> notParams) {
        Map<String, MatchPair> condition = new HashMap<String, RouteRule.MatchPair>();
        
        for(Entry<String, String> entry : params.entrySet()) {
            String valueListString = entry.getValue();
            if(StringUtils.isBlank(valueListString)) {
                continue;
            }
            String[] list = VALUE_LIST_SEPARATOR.split(valueListString);
            Set<String> set = new HashSet<String>();
            for(String item : list) {
                if(StringUtils.isBlank(item)) {
                    continue;
                }
                set.add(item.trim());
            }
            if(set.isEmpty()) {
                continue;
            }
            
            String key = entry.getKey();
            MatchPair matchPair = condition.get(key);
            if(null == matchPair) {
                matchPair = new MatchPair();
                condition.put(key, matchPair);
            }
            
            matchPair.matches = set;
        }
        for(Entry<String, String> entry : notParams.entrySet()) {
            String valueListString = entry.getValue();
            if(StringUtils.isBlank(valueListString)) {
                continue;
            }
            String[] list = VALUE_LIST_SEPARATOR.split(valueListString);
            Set<String> set = new HashSet<String>();
            for(String item : list) {
                if(StringUtils.isBlank(item)) {
                    continue;
                }
                set.add(item.trim());
            }
            if(set.isEmpty()) {
                continue;
            }
            
            String key = entry.getKey();
            MatchPair matchPair = condition.get(key);
            if(null == matchPair) {
                matchPair = new MatchPair();
                condition.put(key, matchPair);
            }
            
            matchPair.unmatches = set;
        }
        
        return condition;
    }
    
    public static RouteRule createFromNameAndValueListString(Map<String, String> whenParams, Map<String, String> notWhenParams,
                                                             Map<String, String> thenParams, Map<String, String> notThenParams) {
        Map<String, MatchPair> when = parseNameAndValueListString2Condition(whenParams, notWhenParams);
        Map<String, MatchPair> then = parseNameAndValueListString2Condition(thenParams, notThenParams);
        
        return new RouteRule(when, then);
    }
    
    public static RouteRule createFromCondition(Map<String, MatchPair> whenCondition, Map<String, MatchPair> thenCondition) {
        return new RouteRule(whenCondition, thenCondition);
    }
    
    public static RouteRule copyWithRemove(RouteRule copy, Set<String> whenParams, Set<String> thenParams) {
        Map<String, MatchPair> when = new HashMap<String, RouteRule.MatchPair>();
        for(Entry<String, MatchPair> entry : copy.getWhenCondition().entrySet()) {
            if(whenParams == null || !whenParams.contains(entry.getKey())) {
                when.put(entry.getKey(), entry.getValue());
            }
        }
        
        Map<String, MatchPair> then = new HashMap<String, RouteRule.MatchPair>();
        for(Entry<String, MatchPair> entry : copy.getThenCondition().entrySet()) {
            if(thenParams ==null || !thenParams.contains(entry.getKey())) {
                then.put(entry.getKey(), entry.getValue());
            }
        }
        
        return new RouteRule(when, then);
    }
    
    /**
     * 使用新的条件值来替换。
     * 
     * @param copy 替换的Base
     * @param whenCondition 要替换的whenCondition，如果Base没有项目，则直接插入。
     * @param thenCondition 要替换的thenCondition，如果Base没有项目，则直接插入。
     * @return 替换后的RouteRule
     */
    public static RouteRule copyWithReplace(RouteRule copy, Map<String, MatchPair> whenCondition, Map<String, MatchPair> thenCondition) {
        if(null == copy) {
            throw new NullPointerException("Argument copy is null!");
        }
        
        Map<String, MatchPair> when = new HashMap<String, RouteRule.MatchPair>();
        when.putAll(copy.getWhenCondition());
        if(whenCondition != null) {
            when.putAll(whenCondition);
        }
        
        Map<String, MatchPair> then = new HashMap<String, RouteRule.MatchPair>();
        then.putAll(copy.getThenCondition());
        if(thenCondition != null) {
            then.putAll(thenCondition);
        }
        
        return new RouteRule(when, then);
    }
    
    // TODO 目前ToString出来的列表是乱序的，是否要排序？
    static void join(StringBuilder sb, Set<String> valueSet) {
        boolean isFirst = true;
        for(String s : valueSet) {
           if(isFirst) {
               isFirst = false;
           }
           else {
               sb.append(",");
           }
           
           sb.append(s);
        }
    }
    
    /**
     * 样本是否通过条件。
     * <p>
     * 如果样本的KV中，存在Key有对应的MatchPair，且Value不通过MatchPair里，返回{@code false}；
     * 否则返回{@code true}。
     * 
     * @see MatchPair#pass(String)
     */
    public static boolean matchCondition(Map<String, String> sample,
                                         Map<String, MatchPair> condition) {
        for (Map.Entry<String, String> entry : sample.entrySet()) {
            String key = entry.getKey();

            MatchPair pair = condition.get(key);
            if (pair != null && !pair.pass(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
    
    
    // FIXME 去掉这样的方法调用
    public static String join(Set<String> valueSet) {
        StringBuilder sb = new StringBuilder(128);
        join(sb, valueSet);
        return sb.toString();
    }
    
    // TODO 目前Condition的多个Key是乱序的，是否要排序？
    public static void contidionToString(StringBuilder sb, Map<String, MatchPair> condition) {
        boolean isFirst = true;
        for(Entry<String, MatchPair> entry: condition.entrySet()) {
            String keyName = entry.getKey();
            MatchPair p = entry.getValue();
            
            @SuppressWarnings("unchecked")
            Set<String>[] setArray = new Set[]{p.matches, p.unmatches};
            String[] opArray = {" = ", " != "};
            
            for(int i = 0; i < setArray.length; ++i) {
                if(setArray[i].isEmpty()) {
                    continue;
                }
                if(isFirst) {
                    isFirst = false;
                }
                else {
                    sb.append(" & ");
                }
                
                sb.append(keyName);
                sb.append(opArray[i]);
                join(sb, setArray[i]);
            }
        }
    }
    
    public boolean isWhenContainValue(String key, String value) {
        MatchPair matchPair = whenCondition.get(key);
        if(null == matchPair) {
            return false;
        }
        
        return matchPair.containeValue(value);
    }
    
    public boolean isThenContainValue(String key, String value) {
        MatchPair matchPair = thenCondition.get(key);
        if(null == matchPair) {
            return false;
        }
        
        return matchPair.containeValue(value);
    }
    
    public boolean isContainValue(String key, String value) {
        return isWhenContainValue(key, value) || isThenContainValue(key, value);
    }
    
    public Map<String, MatchPair> getWhenCondition() {
        return whenCondition;
    }

    public Map<String, MatchPair> getThenCondition() {
        return thenCondition;
    }
    
    public String getWhenConditionString() {
    	StringBuilder sb = new StringBuilder(512);
        contidionToString(sb, whenCondition);
    	return sb.toString();
    }

    public String getThenConditionString() {
    	StringBuilder sb = new StringBuilder(512);
        contidionToString(sb, thenCondition);
    	return sb.toString();
    }

    private volatile String tostring = null;
    
    @Override
    public String toString() {
        if(tostring != null)
        	return tostring;
        StringBuilder sb = new StringBuilder(512);
        contidionToString(sb, whenCondition);
        sb.append(" => ");
        contidionToString(sb, thenCondition);
        return tostring = sb.toString();
    }

    // 用Eclipse自动生成
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((thenCondition == null) ? 0 : thenCondition.hashCode());
        result = prime * result + ((whenCondition == null) ? 0 : whenCondition.hashCode());
        return result;
    }

    // 用Eclipse自动生成
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RouteRule other = (RouteRule) obj;
        if (thenCondition == null) {
            if (other.thenCondition != null)
                return false;
        } else if (!thenCondition.equals(other.thenCondition))
            return false;
        if (whenCondition == null) {
            if (other.whenCondition != null)
                return false;
        } else if (!whenCondition.equals(other.whenCondition))
            return false;
        return true;
    }
}
