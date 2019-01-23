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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.Constants;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilsTest {
    @Test
    public void testLength() throws Exception {
        assertThat(StringUtils.length(null), equalTo(0));
        assertThat(StringUtils.length("abc"), equalTo(3));
    }

    @Test
    public void testRepeat() throws Exception {
        assertThat(StringUtils.repeat(null, 2), nullValue());
        assertThat(StringUtils.repeat("", 0), equalTo(""));
        assertThat(StringUtils.repeat("", 2), equalTo(""));
        assertThat(StringUtils.repeat("a", 3), equalTo("aaa"));
        assertThat(StringUtils.repeat("ab", 2), equalTo("abab"));
        assertThat(StringUtils.repeat("a", -2), equalTo(""));
        assertThat(StringUtils.repeat(null, null, 2), nullValue());
        assertThat(StringUtils.repeat(null, "x", 2), nullValue());
        assertThat(StringUtils.repeat("", null, 0), equalTo(""));
        assertThat(StringUtils.repeat("", "", 2), equalTo(""));
        assertThat(StringUtils.repeat("", "x", 3), equalTo("xx"));
        assertThat(StringUtils.repeat("?", ", ", 3), equalTo("?, ?, ?"));
        assertThat(StringUtils.repeat('e', 0), equalTo(""));
        assertThat(StringUtils.repeat('e', 3), equalTo("eee"));
    }

    @Test
    public void testStripEnd() throws Exception {
        assertThat(StringUtils.stripEnd(null, "*"), nullValue());
        assertThat(StringUtils.stripEnd("", null), equalTo(""));
        assertThat(StringUtils.stripEnd("abc", ""), equalTo("abc"));
        assertThat(StringUtils.stripEnd("abc", null), equalTo("abc"));
        assertThat(StringUtils.stripEnd("  abc", null), equalTo("  abc"));
        assertThat(StringUtils.stripEnd("abc  ", null), equalTo("abc"));
        assertThat(StringUtils.stripEnd(" abc ", null), equalTo(" abc"));
        assertThat(StringUtils.stripEnd("  abcyx", "xyz"), equalTo("  abc"));
        assertThat(StringUtils.stripEnd("120.00", ".0"), equalTo("12"));
    }

    @Test
    public void testReplace() throws Exception {
        assertThat(StringUtils.replace(null, "*", "*"), nullValue());
        assertThat(StringUtils.replace("", "*", "*"), equalTo(""));
        assertThat(StringUtils.replace("any", null, "*"), equalTo("any"));
        assertThat(StringUtils.replace("any", "*", null), equalTo("any"));
        assertThat(StringUtils.replace("any", "", "*"), equalTo("any"));
        assertThat(StringUtils.replace("aba", "a", null), equalTo("aba"));
        assertThat(StringUtils.replace("aba", "a", ""), equalTo("b"));
        assertThat(StringUtils.replace("aba", "a", "z"), equalTo("zbz"));
        assertThat(StringUtils.replace(null, "*", "*", 64), nullValue());
        assertThat(StringUtils.replace("", "*", "*", 64), equalTo(""));
        assertThat(StringUtils.replace("any", null, "*", 64), equalTo("any"));
        assertThat(StringUtils.replace("any", "*", null, 64), equalTo("any"));
        assertThat(StringUtils.replace("any", "", "*", 64), equalTo("any"));
        assertThat(StringUtils.replace("any", "*", "*", 0), equalTo("any"));
        assertThat(StringUtils.replace("abaa", "a", null, -1), equalTo("abaa"));
        assertThat(StringUtils.replace("abaa", "a", "", -1), equalTo("b"));
        assertThat(StringUtils.replace("abaa", "a", "z", 0), equalTo("abaa"));
        assertThat(StringUtils.replace("abaa", "a", "z", 1), equalTo("zbaa"));
        assertThat(StringUtils.replace("abaa", "a", "z", 2), equalTo("zbza"));
    }

    @Test
    public void testIsBlank() throws Exception {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertFalse(StringUtils.isBlank("abc"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("abc"));
    }

    @Test
    public void testIsNoneEmpty() throws Exception {
        assertFalse(StringUtils.isNoneEmpty(null));
        assertFalse(StringUtils.isNoneEmpty(""));
        assertTrue(StringUtils.isNoneEmpty(" "));
        assertTrue(StringUtils.isNoneEmpty("abc"));
        assertTrue(StringUtils.isNoneEmpty("abc", "def"));
        assertFalse(StringUtils.isNoneEmpty("abc", null));
        assertFalse(StringUtils.isNoneEmpty("abc", ""));
        assertTrue(StringUtils.isNoneEmpty("abc", " "));
    }

    @Test
    public void testIsAnyEmpty() throws Exception {
        assertTrue(StringUtils.isAnyEmpty(null));
        assertTrue(StringUtils.isAnyEmpty(""));
        assertFalse(StringUtils.isAnyEmpty(" "));
        assertFalse(StringUtils.isAnyEmpty("abc"));
        assertFalse(StringUtils.isAnyEmpty("abc", "def"));
        assertTrue(StringUtils.isAnyEmpty("abc", null));
        assertTrue(StringUtils.isAnyEmpty("abc", ""));
        assertFalse(StringUtils.isAnyEmpty("abc", " "));
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
        assertTrue(StringUtils.isNotEmpty("abc"));
    }

    @Test
    public void testIsEquals() throws Exception {
        assertTrue(StringUtils.isEquals(null, null));
        assertFalse(StringUtils.isEquals(null, ""));
        assertTrue(StringUtils.isEquals("abc", "abc"));
        assertFalse(StringUtils.isEquals("abc", "ABC"));
    }

    @Test
    public void testIsInteger() throws Exception {
        assertFalse(StringUtils.isInteger(null));
        assertFalse(StringUtils.isInteger(""));
        assertTrue(StringUtils.isInteger("123"));
    }

    @Test
    public void testParseInteger() throws Exception {
        assertThat(StringUtils.parseInteger(null), equalTo(0));
        assertThat(StringUtils.parseInteger("123"), equalTo(123));
    }

    @Test
    public void testIsJavaIdentifier() throws Exception {
        assertThat(StringUtils.isJavaIdentifier(""), is(false));
        assertThat(StringUtils.isJavaIdentifier("1"), is(false));
        assertThat(StringUtils.isJavaIdentifier("abc123"), is(true));
        assertThat(StringUtils.isJavaIdentifier("abc(23)"), is(false));
    }

    @Test
    public void testExceptionToString() throws Exception {
        assertThat(StringUtils.toString(new RuntimeException("abc")), containsString("java.lang.RuntimeException: abc"));
    }

    @Test
    public void testExceptionToStringWithMessage() throws Exception {
        String s = StringUtils.toString("greeting", new RuntimeException("abc"));
        assertThat(s, containsString("greeting"));
        assertThat(s, containsString("java.lang.RuntimeException: abc"));
    }

    @Test
    public void testParseQueryString() throws Exception {
        assertThat(StringUtils.getQueryStringValue("key1=value1&key2=value2", "key1"), equalTo("value1"));
        assertThat(StringUtils.getQueryStringValue("key1=value1&key2=value2", "key2"), equalTo("value2"));
        assertThat(StringUtils.getQueryStringValue("", "key1"), isEmptyOrNullString());
    }

    @Test
    public void testGetServiceKey() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put(Constants.GROUP_KEY, "dubbo");
        map.put(Constants.INTERFACE_KEY, "a.b.c.Foo");
        map.put(Constants.VERSION_KEY, "1.0.0");
        assertThat(StringUtils.getServiceKey(map), equalTo("dubbo/a.b.c.Foo:1.0.0"));
    }

    @Test
    public void testToQueryString() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        String queryString = StringUtils.toQueryString(map);
        assertThat(queryString, containsString("key1=value1"));
        assertThat(queryString, containsString("key2=value2"));
    }

    @Test
    public void testJoin() throws Exception {
        String[] s = {"1", "2", "3"};
        assertEquals(StringUtils.join(s), "123");
        assertEquals(StringUtils.join(s, ','), "1,2,3");
        assertEquals(StringUtils.join(s, ","), "1,2,3");
    }

    @Test
    public void testSplit() throws Exception {
        String s = "d,1,2,4";
        assertEquals(StringUtils.split(s, ',').length, 4);
    }

    @Test
    public void testTranslate() throws Exception {
        String s = "16314";
        assertEquals(StringUtils.translate(s, "123456", "abcdef"), "afcad");
        assertEquals(StringUtils.translate(s, "123456", "abcd"), "acad");
    }

    @Test
    public void testIsContains() throws Exception {
        assertThat(StringUtils.isContains("a,b, c", "b"), is(true));
        assertThat(StringUtils.isContains("", "b"), is(false));
        assertThat(StringUtils.isContains(new String[]{"a", "b", "c"}, "b"), is(true));
        assertThat(StringUtils.isContains((String[]) null, null), is(false));
    }

    @Test
    public void testIsNumeric() throws Exception {
        assertThat(StringUtils.isNumeric("123"), is(true));
        assertThat(StringUtils.isNumeric("1a3"), is(false));
        assertThat(StringUtils.isNumeric(null), is(false));
    }

    @Test
    public void testJoinCollectionString() throws Exception {
        List<String> list = new ArrayList<String>();
        assertEquals("", StringUtils.join(list, ","));

        list.add("v1");
        assertEquals("v1", StringUtils.join(list, "-"));

        list.add("v2");
        list.add("v3");
        String out = StringUtils.join(list, ":");
        assertEquals("v1:v2:v3", out);
    }

    @Test
    public void testCamelToSplitName() throws Exception {
        assertEquals("ab-cd-ef", StringUtils.camelToSplitName("abCdEf", "-"));
        assertEquals("ab-cd-ef", StringUtils.camelToSplitName("AbCdEf", "-"));
        assertEquals("ab-cd-ef", StringUtils.camelToSplitName("ab-cd-ef", "-"));
        assertEquals("abcdef", StringUtils.camelToSplitName("abcdef", "-"));
    }

    @Test
    public void testToArgumentString() throws Exception {
        String s = StringUtils.toArgumentString(new Object[]{"a", 0, Collections.singletonMap("enabled", true)});
        assertThat(s, containsString("a,"));
        assertThat(s, containsString("0,"));
        assertThat(s, containsString("{\"enabled\":true}"));
    }
}