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
package org.apache.dubbo.common.url;

import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class URLParamTest {
    @Test
    void testParseWithRawParam() {
        URLParam urlParam1 = URLParam.parse("aaa=aaa&bbb&version=1.0&default.ccc=123");
        Assertions.assertEquals("aaa", urlParam1.getParameter("aaa"));
        Assertions.assertEquals("bbb", urlParam1.getParameter("bbb"));
        Assertions.assertEquals("1.0", urlParam1.getParameter("version"));
        Assertions.assertEquals("123", urlParam1.getParameter("default.ccc"));
        Assertions.assertEquals("123", urlParam1.getParameter("ccc"));
        Assertions.assertEquals(urlParam1, URLParam.parse(urlParam1.getRawParam()));
        Assertions.assertEquals(urlParam1, URLParam.parse(urlParam1.toString()));

        URLParam urlParam2 = URLParam.parse("aaa%3dtest", true, null);
        Assertions.assertEquals("test", urlParam2.getParameter("aaa"));

        Map<String, String> overrideMap = Collections.singletonMap("aaa", "bbb");
        URLParam urlParam3 = URLParam.parse("aaa%3dtest", true, overrideMap);
        Assertions.assertEquals("bbb", urlParam3.getParameter("aaa"));

        URLParam urlParam4 = URLParam.parse("ccc=456&&default.ccc=123");
        Assertions.assertEquals("456", urlParam4.getParameter("ccc"));

        URLParam urlParam5 = URLParam.parse("version=2.0&&default.version=1.0");
        Assertions.assertEquals("2.0", urlParam5.getParameter("version"));
    }

    @Test
    void testParseWithMap() {
        Map<String, String> map = new HashMap<>();
        map.put("aaa", "aaa");
        map.put("bbb", "bbb");
        map.put("version", "2.0");
        map.put("side", "consumer");

        URLParam urlParam1 = URLParam.parse(map);
        Assertions.assertEquals("aaa", urlParam1.getParameter("aaa"));
        Assertions.assertEquals("bbb", urlParam1.getParameter("bbb"));
        Assertions.assertEquals("2.0", urlParam1.getParameter("version"));
        Assertions.assertEquals("consumer", urlParam1.getParameter("side"));
        Assertions.assertEquals(urlParam1, URLParam.parse(urlParam1.getRawParam()));

        map.put("bbb", "ccc");

        Assertions.assertEquals("bbb", urlParam1.getParameter("bbb"));

        URLParam urlParam2 = URLParam.parse(map);
        Assertions.assertEquals("ccc", urlParam2.getParameter("bbb"));

        URLParam urlParam3 = URLParam.parse(null, null);
        Assertions.assertFalse(urlParam3.hasParameter("aaa"));
        Assertions.assertEquals(urlParam3, URLParam.parse(urlParam3.getRawParam()));
    }

    @Test
    void testDefault() {
        Map<String, String> map = new HashMap<>();
        map.put("aaa", "aaa");
        map.put("bbb", "bbb");
        map.put("version", "2.0");
        map.put("timeout", "1234");
        map.put("default.timeout", "5678");

        URLParam urlParam1 = URLParam.parse(map);
        Assertions.assertEquals("1234", urlParam1.getParameter("timeout"));
        Assertions.assertEquals("5678", urlParam1.getParameter("default.timeout"));

        map.remove("timeout");
        URLParam urlParam2 = URLParam.parse(map);
        Assertions.assertEquals("5678", urlParam2.getParameter("timeout"));
        Assertions.assertEquals("5678", urlParam2.getParameter("default.timeout"));

        URLParam urlParam3 = URLParam.parse("timeout=1234&default.timeout=5678");
        Assertions.assertEquals("1234", urlParam3.getParameter("timeout"));
        Assertions.assertEquals("5678", urlParam3.getParameter("default.timeout"));

        URLParam urlParam4 = URLParam.parse("default.timeout=5678");
        Assertions.assertEquals("5678", urlParam4.getParameter("timeout"));
        Assertions.assertEquals("5678", urlParam4.getParameter("default.timeout"));
    }

    @Test
    void testGetParameter() {
        URLParam urlParam1 = URLParam.parse("aaa=aaa&bbb&version=1.0&default.ccc=123");
        Assertions.assertNull(urlParam1.getParameter("abcde"));

        URLParam urlParam2 = URLParam.parse("aaa=aaa&bbb&default.ccc=123");
        Assertions.assertNull(urlParam2.getParameter("version"));

        URLParam urlParam3 = URLParam.parse("aaa=aaa&side=consumer");
        Assertions.assertEquals("consumer", urlParam3.getParameter("side"));

        URLParam urlParam4 = URLParam.parse("aaa=aaa&side=provider");
        Assertions.assertEquals("provider", urlParam4.getParameter("side"));

    }

    @Test
    void testHasParameter() {
        URLParam urlParam1 = URLParam.parse("aaa=aaa&side=provider");
        Assertions.assertTrue(urlParam1.hasParameter("aaa"));
        Assertions.assertFalse(urlParam1.hasParameter("bbb"));
        Assertions.assertTrue(urlParam1.hasParameter("side"));
        Assertions.assertFalse(urlParam1.hasParameter("version"));
    }

    @Test
    void testRemoveParameters() {
        URLParam urlParam1 = URLParam.parse("aaa=aaa&side=provider&version=1.0");
        Assertions.assertTrue(urlParam1.hasParameter("aaa"));
        Assertions.assertTrue(urlParam1.hasParameter("side"));
        Assertions.assertTrue(urlParam1.hasParameter("version"));

        URLParam urlParam2 = urlParam1.removeParameters("side");
        Assertions.assertFalse(urlParam2.hasParameter("side"));

        URLParam urlParam3 = urlParam1.removeParameters("aaa", "version");
        Assertions.assertFalse(urlParam3.hasParameter("aaa"));
        Assertions.assertFalse(urlParam3.hasParameter("version"));

        URLParam urlParam4 = urlParam1.removeParameters();
        Assertions.assertTrue(urlParam4.hasParameter("aaa"));
        Assertions.assertTrue(urlParam4.hasParameter("side"));
        Assertions.assertTrue(urlParam4.hasParameter("version"));

        URLParam urlParam5 = urlParam1.clearParameters();
        Assertions.assertFalse(urlParam5.hasParameter("aaa"));
        Assertions.assertFalse(urlParam5.hasParameter("side"));
        Assertions.assertFalse(urlParam5.hasParameter("version"));

        URLParam urlParam6 = urlParam1.removeParameters("aaa");
        Assertions.assertFalse(urlParam6.hasParameter("aaa"));

        URLParam urlParam7 = URLParam.parse("side=consumer").removeParameters("side");
        Assertions.assertFalse(urlParam7.hasParameter("side"));
    }

    @Test
    void testAddParameters() {
        URLParam urlParam1 = URLParam.parse("aaa=aaa&side=provider");
        Assertions.assertTrue(urlParam1.hasParameter("aaa"));
        Assertions.assertTrue(urlParam1.hasParameter("side"));

        URLParam urlParam2 = urlParam1.addParameter("bbb", "bbb");
        Assertions.assertEquals("aaa", urlParam2.getParameter("aaa"));
        Assertions.assertEquals("bbb", urlParam2.getParameter("bbb"));

        URLParam urlParam3 = urlParam1.addParameter("aaa", "ccc");
        Assertions.assertEquals("aaa", urlParam1.getParameter("aaa"));
        Assertions.assertEquals("ccc", urlParam3.getParameter("aaa"));

        URLParam urlParam4 = urlParam1.addParameter("aaa", "aaa");
        Assertions.assertEquals("aaa", urlParam4.getParameter("aaa"));

        URLParam urlParam5 = urlParam1.addParameter("version", "0.1");
        Assertions.assertEquals("0.1", urlParam5.getParameter("version"));

        URLParam urlParam6 = urlParam5.addParameterIfAbsent("version", "0.2");
        Assertions.assertEquals("0.1", urlParam6.getParameter("version"));

        URLParam urlParam7 = urlParam1.addParameterIfAbsent("version", "0.2");
        Assertions.assertEquals("0.2", urlParam7.getParameter("version"));

        Map<String, String> map = new HashMap<>();
        map.put("version", "1.0");
        map.put("side", "provider");

        URLParam urlParam8 = urlParam1.addParameters(map);
        Assertions.assertEquals("1.0", urlParam8.getParameter("version"));
        Assertions.assertEquals("provider", urlParam8.getParameter("side"));

        map.put("side", "consumer");

        Assertions.assertEquals("provider", urlParam8.getParameter("side"));

        URLParam urlParam9 = urlParam8.addParameters(map);
        Assertions.assertEquals("consumer", urlParam9.getParameter("side"));

        URLParam urlParam10 = urlParam8.addParametersIfAbsent(map);
        Assertions.assertEquals("provider", urlParam10.getParameter("side"));

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> urlParam1.addParameter("side", "unrecognized"));
    }

    @Test
    void testURLParamMap() {
        URLParam urlParam1 = URLParam.parse("");
        Assertions.assertTrue(urlParam1.getParameters().isEmpty());
        Assertions.assertEquals(0, urlParam1.getParameters().size());
        Assertions.assertFalse(urlParam1.getParameters().containsKey("aaa"));
        Assertions.assertFalse(urlParam1.getParameters().containsKey("version"));
        Assertions.assertFalse(urlParam1.getParameters().containsKey(new Object()));

        URLParam urlParam2 = URLParam.parse("aaa=aaa&version=1.0");
        URLParam.URLParamMap urlParam2Map = (URLParam.URLParamMap) urlParam2.getParameters();
        Assertions.assertTrue(urlParam2Map.containsKey("version"));
        Assertions.assertFalse(urlParam2Map.containsKey("side"));

        Assertions.assertTrue(urlParam2Map.containsValue("1.0"));
        Assertions.assertFalse(urlParam2Map.containsValue("2.0"));

        Assertions.assertEquals("1.0", urlParam2Map.get("version"));
        Assertions.assertEquals("aaa", urlParam2Map.get("aaa"));
        Assertions.assertNull(urlParam2Map.get(new Object()));

        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map.put("version", "1.0");
        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map.putAll(Collections.singletonMap("version", "1.0"));
        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map.put("side", "consumer");
        Assertions.assertNotEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map = (URLParam.URLParamMap) urlParam2.getParameters();
        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map.remove("version");
        Assertions.assertNotEquals(urlParam2, urlParam2Map.getUrlParam());
        Assertions.assertFalse(urlParam2Map.containsValue("version"));
        Assertions.assertNull(urlParam2Map.getUrlParam().getParameter("version"));

        urlParam2Map = (URLParam.URLParamMap) urlParam2.getParameters();
        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        urlParam2Map.clear();
        Assertions.assertTrue(urlParam2Map.isEmpty());
        Assertions.assertEquals(0, urlParam2Map.size());
        Assertions.assertNull(urlParam2Map.getUrlParam().getParameter("aaa"));
        Assertions.assertNull(urlParam2Map.getUrlParam().getParameter("version"));

        urlParam2Map = (URLParam.URLParamMap) urlParam2.getParameters();
        Assertions.assertEquals(urlParam2, urlParam2Map.getUrlParam());

        URLParam urlParam3 = URLParam.parse("aaa=aaa&version=1.0");
        Assertions.assertTrue(CollectionUtils.mapEquals(urlParam2Map, urlParam3.getParameters()));
        Assertions.assertTrue(CollectionUtils.equals(urlParam2Map.entrySet(), urlParam3.getParameters().entrySet()));
        Assertions.assertTrue(CollectionUtils.equals(urlParam2Map.keySet(), urlParam3.getParameters().keySet()));
        Assertions.assertTrue(CollectionUtils.equals(urlParam2Map.values(), urlParam3.getParameters().values()));

        URLParam urlParam4 = URLParam.parse("aaa=aaa&version=1.0&side=consumer");
        Assertions.assertFalse(CollectionUtils.mapEquals(urlParam2Map, urlParam4.getParameters()));
        Assertions.assertFalse(CollectionUtils.equals(urlParam2Map.entrySet(), urlParam4.getParameters().entrySet()));
        Assertions.assertFalse(CollectionUtils.equals(urlParam2Map.keySet(), urlParam4.getParameters().keySet()));
        Assertions.assertFalse(CollectionUtils.equals(urlParam2Map.values(), urlParam4.getParameters().values()));

        Set<Map<String,String>> set = new HashSet<>();

        set.add(urlParam2Map);
        set.add(urlParam3.getParameters());
        Assertions.assertEquals(1,set.size());

        set.add(urlParam4.getParameters());
        Assertions.assertEquals(2,set.size());
    }

    @Test
    void testMethodParameters() {
        URLParam urlParam1 = URLParam.parse("aaa.method1=aaa&bbb.method2=bbb");
        Assertions.assertEquals("aaa",urlParam1.getAnyMethodParameter("method1"));
        Assertions.assertEquals("bbb",urlParam1.getAnyMethodParameter("method2"));


        URLParam urlParam2 = URLParam.parse("methods=aaa&aaa.method1=aaa&bbb.method2=bbb");
        Assertions.assertEquals("aaa",urlParam2.getAnyMethodParameter("method1"));
        Assertions.assertNull(urlParam2.getAnyMethodParameter("method2"));
    }
}
