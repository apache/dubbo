/*
 * Copyright 1999-2101 Alibaba Group.
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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.serialize.support.dubbo.DubboSerialization;

/**
 * @author ding.lid
 * @author tony.chenl
 */
public class ConfigUtilsTest {

    /**
     * 测试点1：用户配置参数在最后 测试点2：用户配置参数如果带-，会删除同名的默认参数 测试点3：default开头的默认参数会被删除
     */
    @Test
    public void testMergeValues() {
        List<String> merged = ConfigUtils.mergeValues(Serialization.class, "aaa,bbb,default.cunstom",
                                                      Arrays.asList(new String[]{"dubbo","default.hessian2","json"}));
        Assert.assertEquals("[dubbo, json, aaa, bbb, default.cunstom]",merged.toString());
    }
    
    /**
     * 测试点1：用户配置-default，会删除所有默认参数
     */
    @Test
    public void testMergeValuesDeleteDefault() {
        List<String> merged = ConfigUtils.mergeValues(DubboSerialization.class, "-default",
                                                      Arrays.asList("ddd,default.eee,ccc"));
        Assert.assertEquals("[]", merged.toString());
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
        Properties p = ConfigUtils.loadProperties("META-INF/services/com.alibaba.dubbo.common.threadpool.ThreadPool", false);
        
        Properties expected = new Properties();
        expected.put("com.alibaba.dubbo.common.threadpool.support.fixed.FixedThreadPool", "");
        expected.put("com.alibaba.dubbo.common.threadpool.support.cached.CachedThreadPool", "");
        
        Assert.assertEquals(expected, p);
    }
    
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
        
        Properties p = ConfigUtils.loadProperties("META-INF/services/com.alibaba.dubbo.common.status.StatusChecker", true);
        
        Properties expected = new Properties();
        expected.put("com.alibaba.dubbo.common.status.support.MemoryStatusChecker", "");
        expected.put("com.alibaba.dubbo.common.status.support.LoadStatusChecker", "");
        expected.put("aa", "12");
        
        Assert.assertEquals(expected, p);
    }
    
}
