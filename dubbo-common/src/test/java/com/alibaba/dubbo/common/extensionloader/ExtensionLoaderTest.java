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
package com.alibaba.dubbo.common.extensionloader;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extensionloader.activate.ActivateExt1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.ActivateExt1Impl1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.GroupActivateExtImpl;
import com.alibaba.dubbo.common.extensionloader.activate.impl.OrderActivateExtImpl1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.OrderActivateExtImpl2;
import com.alibaba.dubbo.common.extensionloader.activate.impl.ValueActivateExtImpl;
import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extensionloader.ext1.Ext1;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.Ext1Impl1;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.Ext1Impl2;
import com.alibaba.dubbo.common.extensionloader.ext2.Ext2;
import com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder;
import com.alibaba.dubbo.common.extensionloader.ext3.Ext3;
import com.alibaba.dubbo.common.extensionloader.ext4.Ext4;
import com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod;
import com.alibaba.dubbo.common.extensionloader.ext5.impl.Ext5Wrapper1;
import com.alibaba.dubbo.common.extensionloader.ext5.impl.Ext5Wrapper2;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.impl.Ext6Impl2;
import com.alibaba.dubbo.common.extensionloader.ext7.Ext7;
import com.alibaba.dubbo.common.utils.LogUtil;

/**
 * @author ding.lid
 */
public class ExtensionLoaderTest {
    @Test
    public void test_getDefault() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getDefaultExtension();
        assertThat(ext, instanceOf(Ext1Impl1.class));
        
        String name = ExtensionLoader.getExtensionLoader(Ext1.class).getDefaultExtensionName();
        assertEquals("impl1", name);
    }
    
    @Test
    public void test_getDefault_NULL() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getDefaultExtension();
        assertNull(ext);
        
        String name = ExtensionLoader.getExtensionLoader(Ext2.class).getDefaultExtensionName();
        assertNull(name);
    }
    
    @Test
    public void test_getExtension() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(Ext1.class).getExtension("impl1") instanceof Ext1Impl1);
        assertTrue(ExtensionLoader.getExtensionLoader(Ext1.class).getExtension("impl2") instanceof Ext1Impl2);
    }
    
    @Test
    public void test_getExtension_WithWrapper() throws Exception {
        Ext5NoAdaptiveMethod impl1 = ExtensionLoader.getExtensionLoader(Ext5NoAdaptiveMethod.class).getExtension("impl1");
        assertThat(impl1, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));
        
        Ext5NoAdaptiveMethod impl2 = ExtensionLoader.getExtensionLoader(Ext5NoAdaptiveMethod.class).getExtension("impl2") ;
        assertThat(impl2, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));
        
        
        URL url = new URL("p1", "1.2.3.4", 1010, "path1");
        int echoCount1 = Ext5Wrapper1.echoCount.get();
        int echoCount2 = Ext5Wrapper2.echoCount.get();
        int yellCount1 = Ext5Wrapper1.yellCount.get();
        int yellCount2 = Ext5Wrapper2.yellCount.get();
        
        assertEquals("Ext5Impl1-echo", impl1.echo(url, "ha"));
        assertEquals(echoCount1 + 1, Ext5Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext5Wrapper2.echoCount.get());
        assertEquals(yellCount1, Ext5Wrapper1.yellCount.get());
        assertEquals(yellCount2, Ext5Wrapper2.yellCount.get());
        
        assertEquals("Ext5Impl2-yell", impl2.yell(url, "ha"));
        assertEquals(echoCount1 + 1, Ext5Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext5Wrapper2.echoCount.get());
        assertEquals(yellCount1 + 1, Ext5Wrapper1.yellCount.get());
        assertEquals(yellCount2 + 1, Ext5Wrapper2.yellCount.get());
    }
    
    @Test
    public void test_getExtension_ExceptionNoExtension() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Ext1.class).getExtension("XXX");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension com.alibaba.dubbo.common.extensionloader.ext1.Ext1 by name XXX"));
        }
    }
    
    @Test
    public void test_getExtension_ExceptionNoExtension_NameOnWrapperNoAffact() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Ext5NoAdaptiveMethod.class).getExtension("XXX");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod by name XXX"));
        }
    }
    
    @Test
    public void test_getExtension_ExceptionNullArg() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Ext1.class).getExtension(null);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }
    
    @Test
    public void test_hasExtension() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(Ext1.class).hasExtension("impl1"));
        assertFalse(ExtensionLoader.getExtensionLoader(Ext1.class).hasExtension("impl1,impl2"));
        assertFalse(ExtensionLoader.getExtensionLoader(Ext1.class).hasExtension("xxx"));
        
        try {
            ExtensionLoader.getExtensionLoader(Ext1.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }
    
    @Test
    public void test_getSupportedExtensions() throws Exception {
        Set<String> exts = ExtensionLoader.getExtensionLoader(Ext1.class).getSupportedExtensions();
        
        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");
        
        assertEquals(expected, exts);
    }
    
    @Test
    public void test_getSupportedExtensions_NoExtension() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(ExtensionLoaderTest.class).getSupportedExtensions();
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), 
                    allOf(containsString("com.alibaba.dubbo.common.extensionloader.ExtensionLoaderTest"),
                            containsString("is not extension"),
                            containsString("WITHOUT @SPI Annotation")));
       
        }
    }
    
    @Test
    public void test_getAdaptiveExtension_defaultExtension() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        String echo = ext.echo(url, "haha");
        assertEquals("Ext1Impl1-echo", echo);
    }

    @Test
    public void test_getAdaptiveExtension() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        map.put("ext1", "impl2");
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        String echo = ext.echo(url, "haha");
        assertEquals("Ext1Impl2-echo", echo);
    }

    @Test
    public void test_getAdaptiveExtension_customizeKey() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        map.put("key2", "impl2");
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        String echo = ext.yell(url, "haha");
        assertEquals("Ext1Impl2-yell", echo);

        url = url.addParameter("key1", "impl3"); // 注意： URL是值类型
        echo = ext.yell(url, "haha");
        assertEquals("Ext1Impl3-yell", echo);
    }

    @Test
    public void test_getAdaptiveExtension_UrlNpe() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("url == null", e.getMessage());
        }
    }
    
    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoAdativeMethodOnInterface() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Ext5NoAdaptiveMethod.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), 
                    allOf(containsString("Can not create adaptive extenstion interface com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod"),
                            containsString("No adaptive method on extension com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod, refuse to create the adaptive class")));
        }
        // 多次get，都会报错且相同
        try {
            ExtensionLoader.getExtensionLoader(Ext5NoAdaptiveMethod.class).getAdaptiveExtension();
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), 
                    allOf(containsString("Can not create adaptive extenstion interface com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod"),
                            containsString("No adaptive method on extension com.alibaba.dubbo.common.extensionloader.ext5.Ext5NoAdaptiveMethod, refuse to create the adaptive class")));
        }
    }

    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNotAdativeMethod() throws Exception {
        Ext1 ext = ExtensionLoader.getExtensionLoader(Ext1.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        try {
            ext.bang(url, 33);
            fail();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface com.alibaba.dubbo.common.extensionloader.ext1.Ext1 is not adaptive method!"));
        }
    }
    
    @Test
    public void test_getAdaptiveExtension_ExceptionWhenNoUrlAttrib() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(Ext4.class).getAdaptiveExtension();
            fail();
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("fail to create adative class for interface "));
            assertThat(expected.getMessage(), containsString(": not found url parameter or url attribute in parameters of method "));
        }
    }
    
    @Test
    public void test_getAdaptiveExtension_protocolKey() throws Exception {
        Ext3 ext = ExtensionLoader.getExtensionLoader(Ext3.class).getAdaptiveExtension();
    
        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("impl3", "1.2.3.4", 1010, "path1", map);
        
        String echo = ext.echo(url, "s");
        assertEquals("Ext3Impl3-echo", echo);
    
        url = url.addParameter("key1", "impl2");
        echo = ext.echo(url, "s");
        assertEquals("Ext3Impl2-echo", echo);
        
        String yell = ext.yell(url, "d");
        assertEquals("Ext3Impl3-yell", yell);
    }
    
    
    @Test
    public void test_getAdaptiveExtension_lastProtocolKey() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();
        
        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("impl1", "1.2.3.4", 1010, "path1", map);
        String yell = ext.yell(url, "s");
        assertEquals("Ext2Impl1-yell", yell);
        
        url = url.addParameter("key1", "impl2");
        yell = ext.yell(url, "s");
        assertEquals("Ext2Impl2-yell", yell);
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();
        
        Map<String, String> map = new HashMap<String, String>();
        map.put("ext2", "impl1");
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);
        
        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);
    
        String echo = ext.echo(holder, "haha");
        assertEquals("Ext2Impl1-echo", echo);
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_noExtension() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);
        
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Fail to get extension("));
        }
        
        url = url.addParameter("ext2", "XXX");
        holder.setUrl(url);
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_UrlNpe() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        try {
            ext.echo(null, "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder argument == null", e.getMessage());
        }
        
        try {
            ext.echo(new UrlHolder(), "haha");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder argument getUrl() == null", e.getMessage());
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNotAdativeMethod() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        Map<String, String> map = new HashMap<String, String>();
        URL url = new URL("p1", "1.2.3.4", 1010, "path1", map);

        try {
            ext.bang(url, 33);
            fail();
        } catch (UnsupportedOperationException expected) {
            assertThat(expected.getMessage(), containsString("method "));
            assertThat(
                    expected.getMessage(),
                    containsString("of interface com.alibaba.dubbo.common.extensionloader.ext2.Ext2 is not adaptive method!"));
        }
    }

    @Test
    public void test_urlHolder_getAdaptiveExtension_ExceptionWhenNameNotProvided() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");

        UrlHolder holder = new UrlHolder();
        holder.setUrl(url);
        
        try {
            ext.echo(holder, "impl1");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Fail to get extension("));
        }
        
        url = url.addParameter("key1", "impl1");
        holder.setUrl(url);
        try {
            ext.echo(holder, "haha");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Fail to get extension(com.alibaba.dubbo.common.extensionloader.ext2.Ext2) name from url"));
        }
    }
    
    @Test
    public void test_getAdaptiveExtension_inject() throws Exception {
        LogUtil.start();
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getAdaptiveExtension();

        URL url = new URL("p1", "1.2.3.4", 1010, "path1");
        url = url.addParameters("ext6", "impl1");
        
        assertEquals("Ext6Impl1-echo-Ext1Impl1-echo", ext.echo(url, "ha"));
        
        Assert.assertTrue("can not find error.", LogUtil.checkNoError());
        LogUtil.stop();
        
        url = url.addParameters("ext1", "impl2");
        assertEquals("Ext6Impl1-echo-Ext1Impl2-echo", ext.echo(url, "ha"));
        
    }
    
    @Test
    public void test_getAdaptiveExtension_InjectNotExtFail() throws Exception {
        Ext6 ext = ExtensionLoader.getExtensionLoader(Ext6.class).getExtension("impl2");
        
        Ext6Impl2 impl = (Ext6Impl2) ext;
        assertNull(impl.getList());
    }
    
    @Test
    public void test_InitError() throws Exception {
        ExtensionLoader<Ext7> loader = ExtensionLoader.getExtensionLoader(Ext7.class);
        
        loader.getExtension("ok");
        
        try {
            loader.getExtension("error");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class(interface: interface com.alibaba.dubbo.common.extensionloader.ext7.Ext7"));
            assertThat(expected.getCause(), instanceOf(ExceptionInInitializerError.class));
        }
    }

    @Test
    public void testLoadActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "default_group");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == ActivateExt1Impl1.class);

        // test group
        url = url.addParameter(Constants.GROUP_KEY, "group1");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "group1");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == GroupActivateExtImpl.class);

        // test value
        url = url.removeParameter(Constants.GROUP_KEY);
        url = url.addParameter(Constants.GROUP_KEY, "value");
        url = url.addParameter("value", "value");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "value");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == ValueActivateExtImpl.class);

        // test order
        url = URL.valueOf("test://localhost/test");
        url = url.addParameter(Constants.GROUP_KEY, "order");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "order");
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0).getClass() == OrderActivateExtImpl1.class);
        Assert.assertTrue(list.get(1).getClass() == OrderActivateExtImpl2.class);
    }

}