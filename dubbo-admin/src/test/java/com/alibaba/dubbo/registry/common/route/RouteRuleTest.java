/**
 * Project: dubbo.registry.server
 * <p>
 * File Created at Oct 18, 2010
 * $Id: RouteRuleTest.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.route;

import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.route.RouteRule.MatchPair;

import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.matchers.JUnitMatchers;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author william.liangf
 * @author ding.lid
 */
public class RouteRuleTest {
    final RouteRule.MatchPair pair;

    {
        Set<String> matches = new HashSet<String>();
        matches.add("+a");
        matches.add("+b");
        matches.add("+c");
        Set<String> unmatches = new HashSet<String>();
        matches.add("-a");
        matches.add("-b");

        pair = new RouteRule.MatchPair(matches, unmatches);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @org.junit.Test
    public void test_MatchPair_copy() throws Exception {
        MatchPair copy = pair.copy();
        assertEquals(pair, copy);

        copy.matches.remove("+a");
        assertFalse(pair.equals(copy));

        copy = pair.copy();
        copy.matches.add("+z");
        assertFalse(pair.equals(copy));
    }

    @org.junit.Test
    public void test_MatchPair_freeze() throws Exception {
        pair.freeze();

        try {
            pair.matches.add("+z");
            fail();
        } catch (UnsupportedOperationException expected) {
        }
        try {
            pair.unmatches.add("+z");
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }


    @org.junit.Test
    public void test_MatchPair_containeValue() throws Exception {
        assertTrue(pair.containeValue("+a"));
        assertTrue(pair.containeValue("+b"));
        assertFalse(pair.containeValue("c"));
        assertTrue(pair.containeValue("-a"));
    }

    @org.junit.Test
    public void testParse_blank() throws Exception {
        try {
            RouteRule.parse((Route) null);
            fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("null route"));
        }

        try {
            RouteRule.parse("", "");
            fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule without then express"));
        }

        try {
            RouteRule.parse("  ", "  ");
            fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule without then express"));
        }
    }
    
    /*@org.junit.Test
    public void testParse_EmptyWhen() {
        try{
            RouteRule.parse("", "provider.application=morgan&provider.host=10.16.26.51&provider.port=1020");
            fail();
        }catch(ParseException expected){
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule without when express"));
        }
    }*/

    @org.junit.Test
    public void testParse_EmptyThen() {
        String expr = "service=com.alibaba.MemberService,AuthService&consumer.host=127.0.0.1&consumer.version != 1.0.0";
        try {
            RouteRule.parse(expr, "");
            Assert.fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule without then express"));
        }
    }

    @org.junit.Test
    public void testParse_NoKeyName() {
        try {
            RouteRule.parse("service=com.alibaba.MemberService,AuthService&consumer.host=127.0.0.1&!=valueOnnnnnly",
                    "provider.application=morgan&provider.host=10.16.26.51&provider.port=1020");
            Assert.fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule"));
            assertThat(expected.getMessage(), JUnitMatchers.containsString("before \"valueOnnnnnly\"."));
        }
        try {
            RouteRule.parse("service=com.alibaba.MemberService,AuthService&consumer.host=127.0.0.1&=valueOnnnnnly",
                    "provider.application=morgan&provider.host=10.16.26.51&provider.port=1020");
            Assert.fail();
        } catch (ParseException expected) {
            assertThat(expected.getMessage(), JUnitMatchers.containsString("Illegal route rule"));
            assertThat(expected.getMessage(), JUnitMatchers.containsString("before \"valueOnnnnnly\"."));
        }
    }


    @org.junit.Test
    public void testParse_MatchRight() throws Exception {
        final Map<String, RouteRule.MatchPair> whenParams = new HashMap<String, RouteRule.MatchPair>();
        final Map<String, RouteRule.MatchPair> thenParams = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("com.alibaba.MemberService");
        p.matches.add("AuthService");
        whenParams.put("service", p);

        p = new RouteRule.MatchPair();
        p.matches.add("127.0.0.1");
        p.matches.add("17.58.25.62");
        whenParams.put("consumer.host", p);

        p = new RouteRule.MatchPair();
        p.matches.add("1.0.0");
        p.matches.add("1.0.6");
        whenParams.put("consumer.version", p);

        p = new RouteRule.MatchPair();
        p.matches.add("morgan");
        p.matches.add("pc2");
        thenParams.put("provider.application", p);

        p = new RouteRule.MatchPair();
        p.matches.add("10.16.26.51");
        thenParams.put("provider.host", p);

        p = new RouteRule.MatchPair();
        p.matches.add("1020");
        thenParams.put("provider.port", p);

        assertEquals(RouteRule.createFromCondition(whenParams, thenParams),
                RouteRule.parse("service=com.alibaba.MemberService,AuthService&consumer.host=127.0.0.1,17.58.25.62&consumer.version = 1.0.0,1.0.6",
                        "provider.application=morgan,pc2&provider.host=10.16.26.51&provider.port=1020"));
    }

    @org.junit.Test
    public void testParse_MisMatchRight() throws Exception {
        final Map<String, RouteRule.MatchPair> whenParams = new HashMap<String, RouteRule.MatchPair>();
        final Map<String, RouteRule.MatchPair> thenParams = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.unmatches.add("com.alibaba.MemberService");
        p.unmatches.add("AuthService");
        whenParams.put("service", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("127.0.0.1");
        whenParams.put("consumer.host", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("1.0.0");
        p.unmatches.add("1.0.6");
        whenParams.put("consumer.version", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("morgan");
        p.unmatches.add("pc2");
        thenParams.put("provider.application", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("10.16.26.51");
        thenParams.put("provider.host", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("1020");
        thenParams.put("provider.port", p);

        assertEquals(RouteRule.createFromCondition(whenParams, thenParams),
                RouteRule.parse("service!=com.alibaba.MemberService,AuthService&consumer.host!=127.0.0.1&consumer.version != 1.0.0,1.0.6",
                        "provider.application!=morgan,pc2&provider.host!=10.16.26.51&provider.port!=1020"));
    }

    @org.junit.Test
    public void testParse_Simple() throws Exception {
        final Map<String, RouteRule.MatchPair> whenParams = new HashMap<String, RouteRule.MatchPair>();
        final Map<String, RouteRule.MatchPair> thenParams = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("com.alibaba.morgan.*");
        p.unmatches.add("com.alibaba.morgan.MemberService");
        whenParams.put("service", p);

        p = new RouteRule.MatchPair();
        p.matches.add("10.16.26.51");
        thenParams.put("provider.host", p);

        assertEquals(RouteRule.createFromCondition(whenParams, thenParams),
                RouteRule.parse("service=com.alibaba.morgan.*&service!=com.alibaba.morgan.MemberService", "provider.host=10.16.26.51"));
    }

    @org.junit.Test
    public void testParse_AllRight() throws Exception {
        final Map<String, RouteRule.MatchPair> whenParams = new HashMap<String, RouteRule.MatchPair>();
        final Map<String, RouteRule.MatchPair> thenParams = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("com.alibaba.MemberService");
        p.matches.add("AuthService");
        p.unmatches.add("com.alibaba.DomainService");
        p.unmatches.add("com.alibaba.ViewCacheService");
        whenParams.put("service", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("127.0.0.1");
        p.unmatches.add("15.11.57.6");
        whenParams.put("consumer.host", p);

        p = new RouteRule.MatchPair();
        p.matches.add("2.0.0");
        p.unmatches.add("1.0.0");
        whenParams.put("consumer.version", p);

        p = new RouteRule.MatchPair();
        p.matches.add("morgan");
        p.matches.add("pc2");
        thenParams.put("provider.application", p);

        p = new RouteRule.MatchPair();
        p.matches.add("10.16.26.51");
        thenParams.put("provider.host", p);

        p = new RouteRule.MatchPair();
        p.matches.add("1020");
        thenParams.put("provider.port", p);

        assertEquals(RouteRule.createFromCondition(whenParams, thenParams),
                RouteRule.parse("service=com.alibaba.MemberService,AuthService&service!=com.alibaba.DomainService,com.alibaba.ViewCacheService" +
                                "&consumer.host!=127.0.0.1,15.11.57.6&consumer.version = 2.0.0&consumer.version != 1.0.0",
                        "provider.application=morgan,pc2&provider.host=10.16.26.51&provider.port=1020"));
    }

    @org.junit.Test
    public void testParseRule_empty() throws Exception {
        Map<String, RouteRule.MatchPair> condtion = RouteRule.parseRule(null);
        assertEquals(0, condtion.size());

        condtion = RouteRule.parseRule("");
        assertEquals(0, condtion.size());

        condtion = RouteRule.parseRule("    ");
        assertEquals(0, condtion.size());
    }

    @org.junit.Test
    public void testParseRule_complex() throws Exception {
        Map<String, RouteRule.MatchPair> when = RouteRule.parseRule("when1=valueW1 ,valueW1 & when2 = valueW2,valueW3, valueW4 & when2 != valueWu,valueWv & when3!=valueWx,valueWy");
        Map<String, RouteRule.MatchPair> then = RouteRule.parseRule("then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");

        Map<String, RouteRule.MatchPair> expectedWhen = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("valueW1");
        expectedWhen.put("when1", p);

        p = new RouteRule.MatchPair();
        p.matches.add("valueW2");
        p.matches.add("valueW3");
        p.matches.add("valueW4");
        p.unmatches.add("valueWu");
        p.unmatches.add("valueWv");
        expectedWhen.put("when2", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("valueWx");
        p.unmatches.add("valueWy");
        expectedWhen.put("when3", p);

        assertEquals(expectedWhen, when);

        Map<String, RouteRule.MatchPair> expectedThen = new HashMap<String, RouteRule.MatchPair>();

        p = new RouteRule.MatchPair();
        p.matches.add("valueT1");
        expectedThen.put("then1", p);

        p = new RouteRule.MatchPair();
        p.matches.add("valueT2");
        p.matches.add("valueT3");
        p.matches.add("valueT4");
        expectedThen.put("then2", p);

        assertEquals(expectedThen, then);
    }

    @org.junit.Test
    public void testParseNameAndValueListString2Condition() throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("key0", "");
        params.put("key1", "value11");
        params.put("key2", "value21,value22");
        params.put("key3", "  value31  ,  value32,  value33, value31   ");
        params.put("key100", null);

        Map<String, String> notParams = new HashMap<String, String>();
        notParams.put("key0", "");
        notParams.put("key1", "v11,v12");
        notParams.put("key2", "   v21, v22, ,,,, v23  ");
        notParams.put("key4", "v41,  v42");
        notParams.put("key100", null);

        Map<String, RouteRule.MatchPair> out = RouteRule.parseNameAndValueListString2Condition(params, notParams);

        Map<String, RouteRule.MatchPair> expected = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("value11");
        p.unmatches.add("v11");
        p.unmatches.add("v12");
        expected.put("key1", p);

        p = new RouteRule.MatchPair();
        p.matches.add("value21");
        p.matches.add("value22");
        p.unmatches.add("v21");
        p.unmatches.add("v22");
        p.unmatches.add("v23");
        expected.put("key2", p);

        p = new RouteRule.MatchPair();
        p.matches.add("value31");
        p.matches.add("value32");
        p.matches.add("value33");
        expected.put("key3", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("v41");
        p.unmatches.add("v42");
        expected.put("key4", p);

        assertEquals(expected, out);
    }


    @org.junit.Test
    public void test_Parse_StringString() throws Exception {
        RouteRule rule = RouteRule.parse("when1=valueW1 ,valueW1 & when2 = valueW2,valueW3, valueW4 & when2 != valueWu,valueWv & when3!=valueWx,valueWy"
                , "then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");

        Map<String, RouteRule.MatchPair> expectedWhen = new HashMap<String, RouteRule.MatchPair>();

        RouteRule.MatchPair p = new RouteRule.MatchPair();
        p.matches.add("valueW1");
        expectedWhen.put("when1", p);

        p = new RouteRule.MatchPair();
        p.matches.add("valueW2");
        p.matches.add("valueW3");
        p.matches.add("valueW4");
        p.unmatches.add("valueWu");
        p.unmatches.add("valueWv");
        expectedWhen.put("when2", p);

        p = new RouteRule.MatchPair();
        p.unmatches.add("valueWx");
        p.unmatches.add("valueWy");
        expectedWhen.put("when3", p);

        assertEquals(expectedWhen, rule.getWhenCondition());

        Map<String, RouteRule.MatchPair> expectedThen = new HashMap<String, RouteRule.MatchPair>();

        p = new RouteRule.MatchPair();
        p.matches.add("valueT1");
        expectedThen.put("then1", p);

        p = new RouteRule.MatchPair();
        p.matches.add("valueT2");
        p.matches.add("valueT3");
        p.matches.add("valueT4");
        expectedThen.put("then2", p);

        assertEquals(expectedThen, rule.getThenCondition());

        RouteRule ruleExpected = RouteRule.createFromCondition(expectedWhen, expectedThen);

        assertEquals(ruleExpected, rule);

        RouteRule ruleFromToString = RouteRule.parse(rule.getWhenConditionString(), rule.getThenConditionString());
        System.out.println(ruleFromToString);
        assertEquals(ruleExpected, ruleFromToString);
    }

    @org.junit.Test
    public void test_Parse_String() throws Exception {
        {
            RouteRule rule1 = RouteRule.parse(""
                    , "then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");
            RouteRule rule2 = RouteRule.parse(" => then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");

            assertEquals(rule1, rule2);
        }

        {
            RouteRule rule1 = RouteRule.parse("when1=valueW1 ,valueW1 & when2 = valueW2,valueW3, valueW4 & when2 != valueWu,valueWv & when3!=valueWx,valueWy"
                    , "then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");
            RouteRule rule2 = RouteRule.parse("when1=valueW1 ,valueW1 & when2 = valueW2,valueW3, valueW4 & when2 != valueWu,valueWv & when3!=valueWx,valueWy"
                    + " => then1 = valueT1 & then2 = valueT2,valueT3,   valueT4");

            assertEquals(rule1, rule2);

            assertEquals(rule1, RouteRule.parse(rule2.toString()));
        }
    }

    @org.junit.Test
    public void test_isWhenContainValue() throws Exception {
        RouteRule r = RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                "then2 = valueT2,valueT4,valueT3 & then1 = valueT1");

        assertTrue(r.isWhenContainValue("when1", "valueW1"));
        assertTrue(r.isWhenContainValue("when2", "valueW2"));
        assertTrue(r.isWhenContainValue("when2", "valueW3"));
        assertTrue(r.isWhenContainValue("when2", "valueW4"));

        assertTrue(r.isContainValue("when1", "valueW1"));
        assertTrue(r.isContainValue("when2", "valueW2"));
        assertTrue(r.isContainValue("when2", "valueW3"));
        assertTrue(r.isContainValue("when2", "valueW4"));

        assertFalse(r.isWhenContainValue("when1", "BlaBla"));
        assertFalse(r.isWhenContainValue("Bbbbb", "BlaBla"));

        assertFalse(r.isContainValue("when1", "BlaBla"));
        assertFalse(r.isContainValue("Bbbbb", "BlaBla"));
    }

    @org.junit.Test
    public void test_isThenContainValue() throws Exception {
        RouteRule r = RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv", "then2 = valueT2,valueT4,valueT3 & then1 = valueT1");

        assertTrue(r.isThenContainValue("then1", "valueT1"));
        assertTrue(r.isThenContainValue("then2", "valueT2"));
        assertTrue(r.isThenContainValue("then2", "valueT3"));
        assertTrue(r.isThenContainValue("then2", "valueT4"));

        assertTrue(r.isContainValue("then1", "valueT1"));
        assertTrue(r.isContainValue("then2", "valueT2"));
        assertTrue(r.isContainValue("then2", "valueT3"));
        assertTrue(r.isContainValue("then2", "valueT4"));

        assertFalse(r.isThenContainValue("then2", "BlaBla"));
        assertFalse(r.isThenContainValue("Bbbbb", "BlaBla"));

        assertFalse(r.isContainValue("then2", "BlaBla"));
        assertFalse(r.isContainValue("Bbbbb", "BlaBla"));
    }

    @org.junit.Test
    public void test_copyWithRemove() throws Exception {
        RouteRule r = RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                "then2 = valueT2,valueT4,valueT3 & then1 = valueT1");

        {
            RouteRule c = RouteRule.copyWithRemove(r, new HashSet<String>(Arrays.asList("when0", "when1")), null);
            assertEquals(RouteRule.parse("when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                    "then2 = valueT2,valueT4,valueT3 & then1 = valueT1"), c);
        }
        {
            RouteRule c = RouteRule.copyWithRemove(r, null, new HashSet<String>(Arrays.asList("then1", "then0")));
            assertEquals(RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                    "then2 = valueT2,valueT4,valueT3"), c);
        }
        {
            RouteRule c = RouteRule.copyWithRemove(r, new HashSet<String>(Arrays.asList("when0", "when1")),
                    new HashSet<String>(Arrays.asList("then1", "then0")));
            assertEquals(RouteRule.parse("when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                    "then2 = valueT2,valueT4,valueT3"), c);
        }
    }

    @org.junit.Test
    public void test_copyWithReplace() throws Exception {
        RouteRule r = RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                "then2 = valueT2,valueT4,valueT3 & then1 = valueT1");

        {
            Map<String, RouteRule.MatchPair> whenCondition = new HashMap<String, RouteRule.MatchPair>();
            RouteRule.MatchPair p = new MatchPair();
            p.matches.add("w1a");
            p.matches.add("w1b");
            p.matches.add("w1c");
            p.unmatches.add("w1-a");
            whenCondition.put("when1", p);

            p = new MatchPair();
            p.matches.add("w9a");
            p.matches.add("w9b");
            whenCondition.put("when9", p);

            RouteRule c = RouteRule.copyWithReplace(r, whenCondition, null);
            assertEquals(RouteRule.parse("when1 = w1a,w1b,w1c & when1 != w1-a & when9 = w9a,w9b " +
                            " & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv",
                    "then2 = valueT2,valueT4,valueT3 & then1 = valueT1"), c);
        }
        {
            Map<String, RouteRule.MatchPair> thenCondition = new HashMap<String, RouteRule.MatchPair>();
            RouteRule.MatchPair p = new MatchPair();
            p.matches.add("t1a");
            p.matches.add("t1b");
            p.matches.add("t1c");
            p.unmatches.add("t1-a");
            thenCondition.put("then1", p);

            RouteRule c = RouteRule.copyWithReplace(r, null, thenCondition);
            assertEquals(RouteRule.parse("when1 = valueW1 & when3 != valueWx,valueWy & when2 = valueW4,valueW2,valueW3 & when2 != valueWu,valueWv"
                    , "then2 = valueT2,valueT4,valueT3 & then1 = t1a,t1b,t1c & then1 != t1-a"), c);
        }
    }

    @org.junit.Test
    public void test_join() throws Exception {
        Set<String> set = new LinkedHashSet<String>();
        assertEquals("", RouteRule.join(set));

        set.add("1");
        set.add("2");
        set.add("3");

        assertEquals("1,2,3", RouteRule.join(set));
    }
}
