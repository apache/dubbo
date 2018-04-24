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
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.threadpool.ThreadPool;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class ConfigUtilsTest {

    public static <T> List<T> toArray(T... args) {
        List<T> ret = new ArrayList<T>();
        for (T a : args) {
            ret.add(a);
        }
        return ret;
    }

    @Test
    public void testMergeValues() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "aaa,bbb,default.cunstom",
                toArray("fixed", "default.limited", "cached"));
        Assert.assertEquals(toArray("fixed", "cached", "aaa", "bbb", "default.cunstom"), merged);
    }

    @Test
    public void testMergeValues_addDefault() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "aaa,bbb,default,zzz",
                toArray("fixed", "default.limited", "cached"));
        Assert.assertEquals(toArray("aaa", "bbb", "fixed", "cached", "zzz"), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-default", toArray("fixed", "default.limited", "cached"));
        Assert.assertEquals(toArray(), merged);
    }

    @Test
    public void testMergeValuesDeleteDefault_2() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-default,aaa", toArray("fixed", "default.limited", "cached"));
        Assert.assertEquals(toArray("aaa"), merged);
    }

    /**
     * Test Point 1: The user configures -default, which will delete all the default parameters
     */
    @Test
    public void testMergeValuesDelete() {
        List<String> merged = ConfigUtils.mergeValues(ThreadPool.class, "-fixed,aaa", toArray("fixed", "default.limited", "cached"));
        Assert.assertEquals(toArray("cached", "aaa"), merged);
    }

    @Test
    public void test_loadProperties_noFile() throws Exception {
        Properties p = ConfigUtils.loadProperties("notExisted", true);
        Properties expected = new Properties();
        Assert.assertEquals(expected, p);

        p = ConfigUtils.loadProperties("notExisted", false);
        Assert.assertEquals(expected, p);
    }

    @Test
    public void test_loadProperties_oneFile() throws Exception {
        Properties p = ConfigUtils.loadProperties("properties.load", false);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        Assert.assertEquals(expected, p);
    }

    @Test
    public void test_loadProperties_oneFile_allowMulti() throws Exception {
        Properties p = ConfigUtils.loadProperties("properties.load", true);

        Properties expected = new Properties();
        expected.put("a", "12");
        expected.put("b", "34");
        expected.put("c", "56");

        Assert.assertEquals(expected, p);
    }

    @Test
    public void test_loadProperties_oneFile_notRootPath() throws Exception {
        Properties p = ConfigUtils.loadProperties("META-INF/dubbo/internal/com.alibaba.dubbo.common.threadpool.ThreadPool", false);

        Properties expected = new Properties();
        expected.put("fixed", "com.alibaba.dubbo.common.threadpool.support.fixed.FixedThreadPool");
        expected.put("cached", "com.alibaba.dubbo.common.threadpool.support.cached.CachedThreadPool");
        expected.put("limited", "com.alibaba.dubbo.common.threadpool.support.limited.LimitedThreadPool");
        expected.put("eager", "com.alibaba.dubbo.common.threadpool.support.eager.EagerThreadPool");

        Assert.assertEquals(expected, p);
    }


    @Ignore("see http://code.alibabatech.com/jira/browse/DUBBO-133")
    @Test
    public void test_loadProperties_multiFile_notRootPath_Exception() throws Exception {
        try {
            ConfigUtils.loadProperties("META-INF/services/com.alibaba.dubbo.common.status.StatusChecker", false);
            Assert.fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("only 1 META-INF/services/com.alibaba.dubbo.common.status.StatusChecker file is expected, but 2 dubbo.properties files found on class path:"));
        }
    }

    @Test
    public void test_loadProperties_multiFile_notRootPath() throws Exception {

        Properties p = ConfigUtils.loadProperties("META-INF/dubbo/internal/com.alibaba.dubbo.common.status.StatusChecker", true);

        Properties expected = new Properties();
        expected.put("memory", "com.alibaba.dubbo.common.status.support.MemoryStatusChecker");
        expected.put("load", "com.alibaba.dubbo.common.status.support.LoadStatusChecker");
        expected.put("aa", "12");

        Assert.assertEquals(expected, p);
    }

}