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

import org.apache.dubbo.common.config.CompositeConfiguration;
import org.apache.dubbo.common.config.InmemoryConfiguration;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigUtilsTest {
    private Properties properties;
    
    @BeforeEach
    public void setUp() throws Exception {
        properties = ConfigUtils.getProperties(Collections.emptySet());
    }

    @AfterEach
    public void tearDown() throws Exception {
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
        List<String> merged = ConfigUtils.mergeValues(ApplicationModel.defaultModel().getExtensionDirector(), ThreadPool.class, "aaa,bbb,default.custom",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("fixed", "cached", "aaa", "bbb", "default.custom"), merged);
    }

    @Test
    public void testMergeValuesAddDefault() {
        List<String> merged = ConfigUtils.mergeValues(ApplicationModel.defaultModel().getExtensionDirector(), ThreadPool.class, "aaa,bbb,default,zzz",
                asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa", "bbb", "fixed", "cached", "zzz"), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = ConfigUtils.mergeValues(ApplicationModel.defaultModel().getExtensionDirector(), ThreadPool.class, "-default", asList("fixed", "default.limited", "cached"));
        assertEquals(Collections.emptyList(), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault_2() {
        List<String> merged = ConfigUtils.mergeValues(ApplicationModel.defaultModel().getExtensionDirector(), ThreadPool.class, "-default,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("aaa"), merged);
    }

    /**
     * The user configures -default, which will delete all the default parameters
     */
    @Test
    public void testMergeValuesDelete() {
        List<String> merged = ConfigUtils.mergeValues(ApplicationModel.defaultModel().getExtensionDirector(), ThreadPool.class, "-fixed,aaa", asList("fixed", "default.limited", "cached"));
        assertEquals(asList("cached", "aaa"), merged);
    }

    @Test
    public void testReplaceProperty() throws Exception {
        String s = ConfigUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.singletonMap("a.b.c", "ABC"));
        assertEquals( "1ABC2ABC3", s);
        s = ConfigUtils.replaceProperty("1${a.b.c}2${a.b.c}3", Collections.<String, String>emptyMap());
        assertEquals("1${a.b.c}2${a.b.c}3", s);
    }

    @Test
    public void testReplaceProperty2() {

        InmemoryConfiguration configuration1 = new InmemoryConfiguration();
        configuration1.getProperties().put("zookeeper.address", "127.0.0.1");

        InmemoryConfiguration configuration2 = new InmemoryConfiguration();
        configuration2.getProperties().put("zookeeper.port", "2181");

        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(configuration1);
        compositeConfiguration.addConfiguration(configuration2);

        String s = ConfigUtils.replaceProperty("zookeeper://${zookeeper.address}:${zookeeper.port}", compositeConfiguration);
        assertEquals("zookeeper://127.0.0.1:2181", s);

        // should not replace inner class name
        String interfaceName = "dubbo.service.io.grpc.examples.helloworld.DubboGreeterGrpc$IGreeter";
        s = ConfigUtils.replaceProperty(interfaceName, compositeConfiguration);
        Assertions.assertEquals(interfaceName, s);
    }

    @Test
    public void testGetProperties1() throws Exception {
        try {
            System.setProperty(CommonConstants.DUBBO_PROPERTIES_KEY, "properties.load");
            Properties p = ConfigUtils.getProperties(Collections.emptySet());
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
        Properties p = ConfigUtils.getProperties(Collections.emptySet());
        assertThat((String) p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testLoadPropertiesNoFile() throws Exception {
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "notExisted", true);
        Properties expected = new Properties();
        assertEquals(expected, p);

        p = ConfigUtils.loadProperties(Collections.emptySet(), "notExisted", false);
        assertEquals(expected, p);
    }

    @Test
    public void testGetProperty() throws Exception {
        assertThat(properties.getProperty("dubbo"), equalTo("properties"));
    }

    @Test
    public void testGetPropertyDefaultValue() throws Exception {
        assertThat(properties.getProperty("not-exist", "default"), equalTo("default"));
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
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "dubbo.properties");
        assertThat((String)p.get("dubbo"), equalTo("properties"));
    }

    @Test
    public void testLoadPropertiesOneFile() throws Exception {
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "properties.load", false);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileAllowMulti() throws Exception {
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "properties.load", true);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadPropertiesOneFileNotRootPath() throws Exception {
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "META-INF/dubbo/internal/org.apache.dubbo.common.threadpool.ThreadPool", false);

        Properties expected = new Properties();
        expected.put("fixed", "org.apache.dubbo.common.threadpool.support.fixed.FixedThreadPool");
        expected.put("cached", "org.apache.dubbo.common.threadpool.support.cached.CachedThreadPool");
        expected.put("limited", "org.apache.dubbo.common.threadpool.support.limited.LimitedThreadPool");
        expected.put("eager", "org.apache.dubbo.common.threadpool.support.eager.EagerThreadPool");

        assertEquals(expected, p);
    }


    @Disabled("Not know why disabled, the original link explaining this was reachable.")
    @Test
    public void testLoadPropertiesMultiFileNotRootPathException() throws Exception {
        try {
            ConfigUtils.loadProperties(Collections.emptySet(), "META-INF/services/org.apache.dubbo.common.status.StatusChecker", false);
            Assertions.fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("only 1 META-INF/services/org.apache.dubbo.common.status.StatusChecker file is expected, but 2 dubbo.properties files found on class path:"));
        }
    }

    @Test
    public void testLoadPropertiesMultiFileNotRootPath() throws Exception {

        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "META-INF/dubbo/internal/org.apache.dubbo.common.status.StatusChecker", true);

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

    @Test
    public void testPropertiesWithStructedValue() throws Exception {
        Properties p = ConfigUtils.loadProperties(Collections.emptySet(), "parameters.properties", false);

        Properties expected = new Properties();
        expected.put("dubbo.parameters", "[{a:b},{c_.d: r*}]");

        assertEquals(expected, p);
    }

    @Test
    public void testLoadMigrationRule() {
        Set<ClassLoader> classLoaderSet = new HashSet<>();
        classLoaderSet.add(ClassUtils.getClassLoader());
        String rule = ConfigUtils.loadMigrationRule(classLoaderSet, "dubbo-migration.yaml");
        Assertions.assertNotNull(rule);
    }
}
