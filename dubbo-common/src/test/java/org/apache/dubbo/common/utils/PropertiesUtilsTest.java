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

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.threadpool.ThreadPool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertiesUtilsTest {
    @BeforeEach
    public void setUp() throws Exception {
        PropertiesUtils.setProperties(null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        PropertiesUtils.setProperties(null);
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertThat(PropertiesUtils.isNotEmpty("abc"), is(true));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(PropertiesUtils.isEmpty(null), is(true));
        assertThat(PropertiesUtils.isEmpty(""), is(true));
        assertThat(PropertiesUtils.isEmpty("false"), is(true));
        assertThat(PropertiesUtils.isEmpty("FALSE"), is(true));
        assertThat(PropertiesUtils.isEmpty("0"), is(true));
        assertThat(PropertiesUtils.isEmpty("null"), is(true));
        assertThat(PropertiesUtils.isEmpty("NULL"), is(true));
        assertThat(PropertiesUtils.isEmpty("n/a"), is(true));
        assertThat(PropertiesUtils.isEmpty("N/A"), is(true));
    }

    @Test
    public void testIsDefault() throws Exception {
        assertThat(PropertiesUtils.isDefault("true"), is(true));
        assertThat(PropertiesUtils.isDefault("TRUE"), is(true));
        assertThat(PropertiesUtils.isDefault("default"), is(true));
        assertThat(PropertiesUtils.isDefault("DEFAULT"), is(true));
    }

    @Test
    public void testMergeValues() {
        List<String> merged = PropertiesUtils.mergeValues(ThreadPool.class, "aaa,bbb,default.custom",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("fixed", "cached", "aaa", "bbb", "default.custom"), merged);
    }

    @Test
    public void testMergeValuesAddDefault() {
        List<String> merged = PropertiesUtils.mergeValues(ThreadPool.class, "aaa,bbb,default,zzz",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa", "bbb", "fixed", "cached", "zzz"), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = PropertiesUtils.mergeValues(ThreadPool.class, "-default", asList("fixed", "default.limited", "cached"));
        assertEquals(asList(), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault_2() {
        List<String> merged = PropertiesUtils.mergeValues(ThreadPool.class, "-default,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa"), merged);
    }

    /**
     * The user configures -default, which will delete all the default parameters
     */
    @Test
    public void testMergeValuesDelete() {
        List<String> merged = PropertiesUtils.mergeValues(ThreadPool.class, "-fixed,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("cached", "aaa"), merged);
    }

    @Test
    public void testReplaceProperty() throws Exception {
        String s = PropertiesUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.singletonMap("a.b.c", "ABC"));
        assertEquals(s, "1ABC2ABC3");
        s = PropertiesUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.<String, String>emptyMap());
        assertEquals(s, "123");
    }

    @Test
    public void testGetProperties1() throws Exception {
        try {
            System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, "properties.load");
            Properties p = PropertiesUtils.getProperties();
            assertThat((String) p.get("a"), equalTo("12"));
            assertThat((String) p.get("b"), equalTo("34"));
            assertThat((String) p.get("c"), equalTo("56"));
        } finally {
            System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
        }
    }

    @Test
    public void testGetProperties2() throws Exception {
        System.clearProperty(CommonConstants.DUBBO_PROPERTIES_KEY);
        Properties p = PropertiesUtils.getProperties();
        assertThat((String) p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testAddProperties() throws Exception {
        Properties p = new Properties();
        p.put("key1", "value1");
        PropertiesUtils.addProperties(p);
        assertThat((String) PropertiesUtils.getProperties().get("key1"), equalTo("value1"));
    }

    @Test
    public void testLoadPropertiesNoFile() throws Exception {
        Properties p = PropertiesUtils.loadProperties("notExisted", true);
        Properties expected = new Properties();
        assertEquals(expected, p);

        p = PropertiesUtils.loadProperties("notExisted", false);
        assertEquals(expected, p);
    }

    @Test
    public void testGetProperty() throws Exception {
        assertThat(PropertiesUtils.getProperty("dubbo"), equalTo("properties"));
    }

    @Test
    public void testGetPropertyDefaultValue() throws Exception {
        assertThat(PropertiesUtils.getProperty("not-exist", "default"), equalTo("default"));
    }

    @Test
    public void testGetPropertyFromSystem() throws Exception {
        try {
            System.setProperty("dubbo", "system");
            assertThat(PropertiesUtils.getProperty("dubbo"), equalTo("system"));
        } finally {
            System.clearProperty("dubbo");
        }
    }

    @Test
    public void testGetSystemProperty() throws Exception {
        try {
            System.setProperty("dubbo", "system-only");
            assertThat(ConfigurationUtils.getSystemProperty("dubbo"), equalTo("system-only"));
        } finally {
            System.clearProperty("dubbo");
        }
    }

    @Test
    public void testLoadProperties() throws Exception {
        Properties p = PropertiesUtils.loadProperties("dubbo.properties");
        assertThat((String)p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testLoadPropertiesOneFile() throws Exception {
        Properties p = PropertiesUtils.loadProperties("properties.load", false);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileAllowMulti() throws Exception {
        Properties p = PropertiesUtils.loadProperties("properties.load", true);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileNotRootPath() throws Exception {
        Properties p = PropertiesUtils.loadProperties("META-INF/dubbo/internal/org.apache.dubbo.common.threadpool.ThreadPool", false);

        Properties expected = new Properties();
        expected.put("fixed", "org.apache.dubbo.common.threadpool.support.fixed.FixedThreadPool");
        expected.put("cached", "org.apache.dubbo.common.threadpool.support.cached.CachedThreadPool");
        expected.put("limited", "org.apache.dubbo.common.threadpool.support.limited.LimitedThreadPool");
        expected.put("eager", "org.apache.dubbo.common.threadpool.support.eager.EagerThreadPool");

        assertEquals(expected, p);
    }


    @Disabled("see http://code.alibabatech.com/jira/browse/DUBBO-133")
    @Test
    public void testLoadPropertiesMultiFileNotRootPathException() throws Exception {
        try {
            PropertiesUtils.loadProperties("META-INF/services/org.apache.dubbo.common.status.StatusChecker", false);
            Assertions.fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("only 1 META-INF/services/org.apache.dubbo.common.status.StatusChecker file is expected, but 2 dubbo.properties files found on class path:"));
        }
    }

    @Test
    public void testLoadPropertiesMultiFileNotRootPath() throws Exception {

        Properties p = PropertiesUtils.loadProperties("META-INF/dubbo/internal/org.apache.dubbo.common.status.StatusChecker", true);

        Properties expected = new Properties();
        expected.put("memory", "org.apache.dubbo.common.status.support.MemoryStatusChecker");
        expected.put("load", "org.apache.dubbo.common.status.support.LoadStatusChecker");
        expected.put("aa", "12");

        assertEquals(expected, p);
    }

    @Test
    public void testGetPid() throws Exception {
        assertThat(PropertiesUtils.getPid(), greaterThan(0));
    }
}
