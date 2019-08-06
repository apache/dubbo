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

public class ConfigUtilsTest {
    @BeforeEach
    public void setUp() throws Exception {
        ConfigUtils.setProperties(null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        ConfigUtils.setProperties(null);
    }

    @Test
    public void testIsNotEmpty() throws Exception {
        assertThat(ConfigUtils.isNotEmpty("abc"), is(true));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(ConfigUtils.isEmpty(null), is(true));
        assertThat(ConfigUtils.isEmpty(""), is(true));
        assertThat(ConfigUtils.isEmpty("false"), is(true));
        assertThat(ConfigUtils.isEmpty("FALSE"), is(true));
        assertThat(ConfigUtils.isEmpty("0"), is(true));
        assertThat(ConfigUtils.isEmpty("null"), is(true));
        assertThat(ConfigUtils.isEmpty("NULL"), is(true));
        assertThat(ConfigUtils.isEmpty("n/a"), is(true));
        assertThat(ConfigUtils.isEmpty("N/A"), is(true));
    }

    @Test
    public void testIsDefault() throws Exception {
        assertThat(ConfigUtils.isDefault("true"), is(true));
        assertThat(ConfigUtils.isDefault("TRUE"), is(true));
        assertThat(ConfigUtils.isDefault("default"), is(true));
        assertThat(ConfigUtils.isDefault("DEFAULT"), is(true));
    }

    @Test
    public void testMergeValues() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "aaa,bbb,default.custom",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("fixed", "cached", "aaa", "bbb", "default.custom"), merged);
    }

    @Test
    public void testMergeValuesAddDefault() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "aaa,bbb,default,zzz",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa", "bbb", "fixed", "cached", "zzz"), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-default", asList("fixed", "default.limited", "cached"));
        assertEquals(asList(), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault_2() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-default,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa"), merged);
    }

    /**
     * The user configures -default, which will delete all the default parameters
     */
    @Test
    public void testMergeValuesDelete() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-fixed,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("cached", "aaa"), merged);
    }

    @Test
    public void testReplaceProperty() throws Exception {
        String s = ConfigUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.singletonMap("a.b.c", "ABC"));
        assertEquals(s, "1ABC2ABC3");
        s = ConfigUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.<String, String>emptyMap());
        assertEquals(s, "123");
    }

    @Test
    public void testGetProperties1() throws Exception {
        try {
            System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, "properties.load");
            Properties p = ConfigUtils.getProperties();
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
        Properties p = ConfigUtils.getProperties();
        assertThat((String) p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testAddProperties() throws Exception {
        Properties p = new Properties();
        p.put("key1", "value1");
        ConfigUtils.addProperties(p);
        assertThat((String) ConfigUtils.getProperties().get("key1"), equalTo("value1"));
    }

    @Test
    public void testLoadPropertiesNoFile() throws Exception {
        Properties p = ConfigUtils.loadProperties("notExisted", true);
        Properties expected = new Properties();
        assertEquals(expected, p);

        p = ConfigUtils.loadProperties("notExisted", false);
        assertEquals(expected, p);
    }

    @Test
    public void testGetProperty() throws Exception {
        assertThat(ConfigUtils.getProperty("dubbo"), equalTo("properties"));
    }

    @Test
    public void testGetPropertyDefaultValue() throws Exception {
        assertThat(ConfigUtils.getProperty("not-exist", "default"), equalTo("default"));
    }

    @Test
    public void testGetPropertyFromSystem() throws Exception {
        try {
            System.setProperty("dubbo", "system");
            assertThat(ConfigUtils.getProperty("dubbo"), equalTo("system"));
        } finally {
            System.clearProperty("dubbo");
        }
    }

    @Test
    public void testGetSystemProperty() throws Exception {
        try {
            System.setProperty("dubbo", "system-only");
            assertThat(ConfigUtils.getSystemProperty("dubbo"), equalTo("system-only"));
        } finally {
            System.clearProperty("dubbo");
        }
    }

    @Test
    public void testLoadProperties() throws Exception {
        Properties p = ConfigUtils.loadProperties("dubbo.properties");
        assertThat((String)p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testLoadPropertiesOneFile() throws Exception {
        Properties p = ConfigUtils.loadProperties("properties.load", false);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileAllowMulti() throws Exception {
        Properties p = ConfigUtils.loadProperties("properties.load", true);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileNotRootPath() throws Exception {
        Properties p = ConfigUtils.loadProperties("META-INF/dubbo/internal/org.apache.dubbo.common.threadpool.ThreadPool", false);

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
            ConfigUtils.loadProperties("META-INF/services/org.apache.dubbo.common.status.StatusChecker", false);
            Assertions.fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("only 1 META-INF/services/org.apache.dubbo.common.status.StatusChecker file is expected, but 2 dubbo.properties files found on class path:"));
        }
    }

    @Test
    public void testLoadPropertiesMultiFileNotRootPath() throws Exception {

        Properties p = ConfigUtils.loadProperties("META-INF/dubbo/internal/org.apache.dubbo.common.status.StatusChecker", true);

        Properties expected = new Properties();
        expected.put("memory", "org.apache.dubbo.common.status.support.MemoryStatusChecker");
        expected.put("load", "org.apache.dubbo.common.status.support.LoadStatusChecker");
        expected.put("aa", "12");

        assertEquals(expected, p);
    }

    @Test
    public void testGetPid() throws Exception {
        assertThat(ConfigUtils.getPid(), greaterThan(0));
    }
}
