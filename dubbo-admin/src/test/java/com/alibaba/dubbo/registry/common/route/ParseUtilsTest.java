/**
 * Project: dubbo.registry.server
 * 
 * File Created at Oct 19, 2010
 * $Id: ParseUtilsTest.java 181192 2012-06-21 05:05:47Z tony.chenl $
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.alibaba.dubbo.registry.common.route.ParseUtils;

/**
 * @author william.liangf
 */
public class ParseUtilsTest {
    @Test
    public void testInterpolateDot() throws Exception {
        String regexp = ParseUtils.interpolate("com.alibaba.morgan.MemberService",
                new HashMap<String, String>());
        assertEquals("com.alibaba.morgan.MemberService", regexp);
    }

    @Test
    public void testInterpolateWildcard() throws Exception {
        String regexp = ParseUtils.interpolate("com.alibaba.morgan.*",
                new HashMap<String, String>());
        assertEquals("com.alibaba.morgan.*", regexp);
    }

    @Test
    public void testInterpolateSequence() throws Exception {
        String regexp = ParseUtils.interpolate("1.0.[0-9]", new HashMap<String, String>());
        assertEquals("1.0.[0-9]", regexp.toString());
    }

    @Test
    public void testInterpolateVariable() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("consumer.address", "10.20.130.230");
        String regexp = ParseUtils.interpolate("xx$consumer.address", params);
        assertEquals("xx10.20.130.230", regexp);
    }

    @Test
    public void testInterpolateVariableWithParentheses() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("consumer.address", "10.20.130.230");
        String regexp = ParseUtils.interpolate("xx${consumer.address}yy", params);
        assertEquals("xx10.20.130.230yy", regexp);
    }

    @Test
    public void testInterpolateCollMap_NormalCase() throws Exception {
        List<String> expressions = new ArrayList<String>();
        expressions.add("xx$var1");
        expressions.add("yy${var2}zz");

        Map<String, String> params = new HashMap<String, String>();
        params.put("var1", "CAT");
        params.put("var2", "DOG");

        List<String> interpolate = ParseUtils.interpolate(expressions, params);

        List<String> expected = new ArrayList<String>();
        expected.add("xxCAT");
        expected.add("yyDOGzz");

        assertEquals(expected, interpolate);
    }

    @Test
    public void testIsMatchGlobPattern() throws Exception {
        // Null Case
        assertTrue(ParseUtils.isMatchGlobPattern(null, null));
        assertFalse(ParseUtils.isMatchGlobPattern("abc", null));
        assertFalse(ParseUtils.isMatchGlobPattern(null, "xxx"));
        
        // empty string
        assertTrue(ParseUtils.isMatchGlobPattern("", ""));
        assertFalse(ParseUtils.isMatchGlobPattern("", "xxx"));
        assertFalse(ParseUtils.isMatchGlobPattern("abc", ""));
        assertFalse(ParseUtils.isMatchGlobPattern("a*bc", ""));
        assertFalse(ParseUtils.isMatchGlobPattern("*abc", ""));
        assertFalse(ParseUtils.isMatchGlobPattern("abc*", ""));

        // Star Case
        assertTrue(ParseUtils.isMatchGlobPattern("*", ""));
        assertTrue(ParseUtils.isMatchGlobPattern("*", "xxx"));

        // Normal Case
        assertTrue(ParseUtils.isMatchGlobPattern("abc*123", "abc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("abc*123", "abcXXX123"));
        assertFalse(ParseUtils.isMatchGlobPattern("abc*123", "abcXXX333"));

        assertTrue(ParseUtils.isMatchGlobPattern("*abc123", "abc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("*abc123", "XXXabc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("*abc123", "abc123abc123"));
        assertFalse(ParseUtils.isMatchGlobPattern("*abc123", "abc123abc333"));
        
        assertTrue(ParseUtils.isMatchGlobPattern("abc123*", "abc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("abc123*", "abc123YYY"));
        assertTrue(ParseUtils.isMatchGlobPattern("abc123*", "abc123abc123"));
        assertFalse(ParseUtils.isMatchGlobPattern("abc123*", "abc333abc123"));
        
        // 有两个星号，不支持，行为未定义
        assertFalse(ParseUtils.isMatchGlobPattern("*abc123*", "abc123abc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("*abc123*", "*abc123abc123"));
        assertTrue(ParseUtils.isMatchGlobPattern("*abc123*", "*abc123XXX"));
    }

    @Test
    public void testIsMatchGlobPatternsNeedInterpolate() throws Exception {
        Collection<String> patternsNeedInterpolate = new HashSet<String>();
        Map<String, String> interpolateParams = new HashMap<String, String>();
        
        boolean match = ParseUtils.isMatchGlobPatternsNeedInterpolate(patternsNeedInterpolate, interpolateParams, "abc");
        assertFalse(match);
        
        patternsNeedInterpolate.add("abc*$var1");
        patternsNeedInterpolate.add("123${var2}*");
        
        interpolateParams.put("var1", "CAT");
        interpolateParams.put("var2", "DOG");
        
        match = ParseUtils.isMatchGlobPatternsNeedInterpolate(patternsNeedInterpolate, interpolateParams, "abc");
        assertFalse(match);
        
        match = ParseUtils.isMatchGlobPatternsNeedInterpolate(patternsNeedInterpolate, interpolateParams, "abcXXXCAT");
        assertTrue(match);
        match = ParseUtils.isMatchGlobPatternsNeedInterpolate(patternsNeedInterpolate, interpolateParams, "123DOGYYY");
        assertTrue(match);
    }

    @Test
    public void test_hasIntersection() throws Exception {
        assertFalse(ParseUtils.hasIntersection(null, null));
        assertFalse(ParseUtils.hasIntersection("dog", null));
        assertFalse(ParseUtils.hasIntersection(null, "god"));

        assertTrue(ParseUtils.hasIntersection("hello", "hello*"));
        assertTrue(ParseUtils.hasIntersection("helloxxx", "hello*"));
        assertTrue(ParseUtils.hasIntersection("world", "*world"));
        assertTrue(ParseUtils.hasIntersection("xxxworld", "*world"));
        assertTrue(ParseUtils.hasIntersection("helloworld", "hello*world"));
        assertTrue(ParseUtils.hasIntersection("helloxxxworld", "hello*world"));
        assertFalse(ParseUtils.hasIntersection("Yhelloxxxworld", "hello*world"));

        assertTrue(ParseUtils.hasIntersection("hello*", "hello"));
        assertTrue(ParseUtils.hasIntersection("hello*", "helloxxx"));
        assertTrue(ParseUtils.hasIntersection("*world", "world"));
        assertTrue(ParseUtils.hasIntersection("*world", "xxxworld"));
        assertTrue(ParseUtils.hasIntersection("hello*world", "helloworld"));
        assertTrue(ParseUtils.hasIntersection("hello*world", "helloxxxworld"));
        assertFalse(ParseUtils.hasIntersection("hello*world", "Yhelloxxxworld"));

        assertTrue(ParseUtils.hasIntersection("*world", "hello*world"));
        assertTrue(ParseUtils.hasIntersection("*world", "hello*Zworld"));
        assertTrue(ParseUtils.hasIntersection("helloZ*", "hello*world"));
        assertFalse(ParseUtils.hasIntersection("Zhello*", "hello*world"));
        assertFalse(ParseUtils.hasIntersection("hello*world", "hello*worldZ"));
        assertTrue(ParseUtils.hasIntersection("hello*world", "hello*world"));
        assertTrue(ParseUtils.hasIntersection("hello*world", "hello*Zworld"));
        assertTrue(ParseUtils.hasIntersection("helloZ*world", "hello*world"));
        assertFalse(ParseUtils.hasIntersection("Zhello*world", "hello*world"));
        assertFalse(ParseUtils.hasIntersection("hello*world", "hello*worldZ"));
    }
    
    @Test
    public void testFilterByGlobPattern() throws Exception {
        Collection<String> values = new ArrayList<String>();
        values.add("abc123");
        values.add("JQKxyz");
        values.add("abc123");
        values.add("abcLLL");
        
        Set<String> filter = ParseUtils.filterByGlobPattern("abc*", values);
        Set<String> expected = new HashSet<String>();
        expected.add("abc123");
        expected.add("abcLLL");
        
        assertEquals(expected, filter);

        filter = ParseUtils.filterByGlobPattern((Collection<String>)null, values);
        assertTrue(filter.isEmpty()); 
        
        Collection<String> patterns = new ArrayList<String>();
        patterns.add("000000000");
        patterns.add("abc*");
        patterns.add("*xyz");

        filter = ParseUtils.filterByGlobPattern(patterns, values);
        
        expected.add("JQKxyz");
        assertEquals(expected, filter);
    }
    
    @Test
    public void testParseQueryNull() throws Exception {
        assertEquals(0, ParseUtils.parseQuery(null, null).size());
        assertEquals(0, ParseUtils.parseQuery(null, "").size());
        assertEquals(0, ParseUtils.parseQuery("", null).size());
    }

    @Test
    public void testParseQueryEmpty() throws Exception {
        assertEquals(0, ParseUtils.parseQuery("", "").size());
    }

    @Test
    public void testParseQuerySingleKeyValue() throws Exception {
        assertEquals("1.0.0", ParseUtils.parseQuery("", "verion=1.0.0").get("verion"));
    }

    @Test
    public void testParseQueryPrefixSingleKeyValue() throws Exception {
        assertEquals("1.0.0",
                ParseUtils.parseQuery("consumer.", "verion=1.0.0").get("consumer.verion"));
    }

    @Test
    public void testParseQueryMultiKeyValue() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("verion", "1.0.0");
        result.put("cluster", "china");
        assertEquals(result, ParseUtils.parseQuery("", "verion=1.0.0&cluster=china"));
    }

    @Test
    public void testParseQueryPrefixMultiKeyValue() throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        result.put("consumer.verion", "1.0.0");
        result.put("consumer.cluster", "china");
        assertEquals(result, ParseUtils.parseQuery("consumer.", "verion=1.0.0&cluster=china"));
    }

    @Test
    public void testReplaceWeightNull() throws Exception {
    	assertEquals("weight=1", ParseUtils.replaceParameter(null, "weight", "1"));
        assertEquals("weight=1", ParseUtils.replaceParameter("", "weight", "1"));
    }

    @Test
    public void testReplaceWeight() throws Exception {
        assertEquals(
                "dubbo://172.22.3.91:20880/memberService?version=1.0.0&application=morgan&weight=12",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?version=1.0.0&application=morgan&weight=10", "weight", "12"));
        assertEquals(
                "dubbo://172.22.3.91:20880/memberService?version=1.0.0&weight=12&application=morgan",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?version=1.0.0&weight=10&application=morgan", "weight", "12"));
        assertEquals(
                "dubbo://172.22.3.91:20880/memberService?weight=12&version=1.0.0&application=morgan",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?weight=10&version=1.0.0&application=morgan", "weight", "12"));
    }

    @Test
    public void testReplaceMethods() throws Exception {
    	assertEquals(
                "methods=aaa,bbb",
                ParseUtils.replaceParameter(null, "methods", "aaa,bbb"));
    	assertEquals(
                "methods=aaa,bbb",
                ParseUtils.replaceParameter("", "methods", "aaa,bbb"));
    	assertEquals(
                "version=1.0.0&application=morgan&weight=10&methods=aaa,bbb",
                ParseUtils.replaceParameter("version=1.0.0&application=morgan&weight=10", "methods", "aaa,bbb"));
    	assertEquals(
                "version=1.0.0&methods=ccc,ddd&application=morgan&weight=10",
                ParseUtils.replaceParameter("version=1.0.0&methods=aaa,bbb&application=morgan&weight=10", "methods", "ccc,ddd"));
    	assertEquals(
                "dubbo://172.22.3.91:20880/memberService?version=1.0.0&application=morgan&weight=10&methods=aaa,bbb",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?version=1.0.0&application=morgan&weight=10", "methods", "aaa,bbb"));
    	assertEquals(
                "dubbo://172.22.3.91:20880/memberService?version=1.0.0&methods=ccc,ddd&application=morgan&weight=10",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?version=1.0.0&methods=aaa,bbb&application=morgan&weight=10", "methods", "ccc,ddd"));
    	assertEquals(
                "dubbo://172.22.3.91:20880/memberService?version=1.0.0&methods=$ccc,$ddd&application=morgan&weight=10",
                ParseUtils.replaceParameter("dubbo://172.22.3.91:20880/memberService?version=1.0.0&methods=$aaa,$bbb&application=morgan&weight=10", "methods", "$ccc,$ddd"));
    }
    
    @Test
    public void test_appendParamToUri() throws Exception {
        String append = ParseUtils.appendParamToUri("dubbo://11.22.33.44/serviceName?k1=v1&k2=v2", "k3", "v3");
        assertEquals("dubbo://11.22.33.44/serviceName?k1=v1&k2=v2&k3=v3", append);

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("k3", "v3");
        params.put("k4", "v4");
        append = ParseUtils.appendParamsToUri("dubbo://11.22.33.44/serviceName?k1=v1&k2=v2", params);
        assertEquals("dubbo://11.22.33.44/serviceName?k1=v1&k2=v2&k3=v3&k4=v4", append);
    }
}
