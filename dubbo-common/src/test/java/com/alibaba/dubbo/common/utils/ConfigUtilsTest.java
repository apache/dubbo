/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.dubbo.common.serialize.Serialization;

/**
 * @author ding.lid
 * @author tony.chenl
 */
public class ConfigUtilsTest {

    public static <T> List<T> toArray(T... args) {
        List<T> ret = new ArrayList<T>();
        for(T a : args) {
            ret.add(a);
        }
        return ret;
    }
    
    /**
     * 测试点1：用户配置参数在最后 测试点2：用户配置参数如果带-，会删除同名的默认参数 测试点3：default开头的默认参数会被删除
     */
    @Test
    public void testMergeValues() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "aaa,bbb,default.cunstom",
                toArray("dubbo","default.hessian2","json"));
        Assert.assertEquals(toArray("dubbo", "json", "aaa", "bbb", "default.cunstom"), merged);
    }
    
    /**
     * 测试点1：用户配置参数在最后 测试点2：用户配置参数如果带-，会删除同名的默认参数 测试点3：default开头的默认参数会被删除
     */
    @Test
    public void testMergeValues_addDefault() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "aaa,bbb,default,zzz",
                toArray("dubbo","default.hessian2","json"));
        Assert.assertEquals(toArray("aaa", "bbb","dubbo", "json",  "zzz"), merged);
    }
    
    /**
     * 测试点1：用户配置-default，会删除所有默认参数
     */
    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "-default", toArray("dubbo","default.hessian2","json"));
        Assert.assertEquals(toArray(), merged);
    }
    
    /**
     * 测试点1：用户配置-default，会删除所有默认参数
     */
    @Test
    public void testMergeValuesDeleteDefault_2() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "-default,aaa", toArray("dubbo","default.hessian2","json"));
        Assert.assertEquals(toArray("aaa"), merged);
    }
    
    /**
     * 测试点1：用户配置-default，会删除所有默认参数
     */
    @Test
    public void testMergeValuesDelete() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "-dubbo,aaa", toArray("dubbo","default.hessian2","json"));
        Assert.assertEquals(toArray("json", "aaa"), merged);
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