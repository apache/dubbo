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
package org.apache.dubbo.rpc.cluster.router.xds.rule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeaderMatcherTest {

    @Test
    public void exactValueMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setName("testHead");
        String value = "testValue";
        headMatcher.setExactValue(value);
        assertTrue(headMatcher.match(value));
    }


    @Test
    public void regexMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setRegex("test.*");
        String value = "testValue";
        headMatcher.setExactValue(value);
        assertTrue(headMatcher.match(value));
    }

    @Test
    public void rangMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        LongRangeMatch range = new LongRangeMatch();
        range.setStart(100);
        range.setEnd(500);
        headMatcher.setRange(range);
        assertTrue(headMatcher.match("300"));
    }


    @Test
    public void presentMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setName("testHead");
        headMatcher.setPresent(true);
        assertTrue(headMatcher.match("value"));
        headMatcher.setPresent(false);
        assertTrue(headMatcher.match(null));
    }

    @Test
    public void prefixMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setName("testHead");
        headMatcher.setPrefix("test");
        assertTrue(headMatcher.match("testValue"));
    }


    @Test
    public void suffixMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setName("testHead");
        headMatcher.setSuffix("Value");
        assertTrue(headMatcher.match("testValue"));
    }

    @Test
    public void invertedMatcherTest() {
        HeaderMatcher headMatcher = new HeaderMatcher();
        headMatcher.setName("testHead");
        String value = "testValue";
        headMatcher.setExactValue(value);
        headMatcher.setInverted(true);
        assertFalse(headMatcher.match("testValue"));
    }
}
