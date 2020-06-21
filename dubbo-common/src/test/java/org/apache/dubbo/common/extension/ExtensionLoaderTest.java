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
package org.apache.dubbo.common.extension;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.convert.Converter;
import org.apache.dubbo.common.convert.StringToBooleanConverter;
import org.apache.dubbo.common.convert.StringToDoubleConverter;
import org.apache.dubbo.common.convert.StringToIntegerConverter;
import org.apache.dubbo.common.extension.activate.ActivateExt1;
import org.apache.dubbo.common.extension.activate.impl.ActivateExt1Impl1;
import org.apache.dubbo.common.extension.activate.impl.GroupActivateExtImpl;
import org.apache.dubbo.common.extension.activate.impl.OldActivateExt1Impl2;
import org.apache.dubbo.common.extension.activate.impl.OldActivateExt1Impl3;
import org.apache.dubbo.common.extension.activate.impl.OrderActivateExtImpl1;
import org.apache.dubbo.common.extension.activate.impl.OrderActivateExtImpl2;
import org.apache.dubbo.common.extension.activate.impl.ValueActivateExtImpl;
import org.apache.dubbo.common.extension.convert.String2BooleanConverter;
import org.apache.dubbo.common.extension.convert.String2DoubleConverter;
import org.apache.dubbo.common.extension.convert.String2IntegerConverter;
import org.apache.dubbo.common.extension.ext1.SimpleExt;
import org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl1;
import org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl2;
import org.apache.dubbo.common.extension.ext10_multi_names.Ext10MultiNames;
import org.apache.dubbo.common.extension.ext2.Ext2;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExt;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext5Wrapper1;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext5Wrapper2;
import org.apache.dubbo.common.extension.ext7.InitErrorExt;
import org.apache.dubbo.common.extension.ext8_add.AddExt1;
import org.apache.dubbo.common.extension.ext8_add.AddExt2;
import org.apache.dubbo.common.extension.ext8_add.AddExt3;
import org.apache.dubbo.common.extension.ext8_add.AddExt4;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt1Impl1;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt1_ManualAdaptive;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt1_ManualAdd1;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt1_ManualAdd2;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt2_ManualAdaptive;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt3_ManualAdaptive;
import org.apache.dubbo.common.extension.ext8_add.impl.AddExt4_ManualAdaptive;
import org.apache.dubbo.common.extension.ext9_empty.Ext9Empty;
import org.apache.dubbo.common.extension.ext9_empty.impl.Ext9EmptyImpl;
import org.apache.dubbo.common.extension.injection.InjectExt;
import org.apache.dubbo.common.extension.injection.impl.InjectExtImpl;
import org.apache.dubbo.common.lang.Prioritized;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;
import static org.apache.dubbo.common.extension.ExtensionLoader.getLoadingStrategies;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExtensionLoaderTest {
    @Test
    public void test_getExtensionLoader_Null() throws Exception {
        try {
            getExtensionLoader(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("Extension type == null"));
        }
    }

    @Test
    public void test_getExtensionLoader_NotInterface() throws Exception {
        try {
            getExtensionLoader(ExtensionLoaderTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("Extension type (class org.apache.dubbo.common.extension.ExtensionLoaderTest) is not an interface"));
        }
    }

    @Test
    public void test_getExtensionLoader_NotSpiAnnotation() throws Exception {
        try {
            getExtensionLoader(NoSpiExt.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("org.apache.dubbo.common.extension.NoSpiExt"),
                            containsString("is not an extension"),
                            containsString("NOT annotated with @SPI")));
        }
    }

    @Test
    public void test_getDefaultExtension() throws Exception {
        SimpleExt ext = getExtensionLoader(SimpleExt.class).getDefaultExtension();
        assertThat(ext, instanceOf(SimpleExtImpl1.class));

        String name = getExtensionLoader(SimpleExt.class).getDefaultExtensionName();
        assertEquals("impl1", name);
    }

    @Test
    public void test_getDefaultExtension_NULL() throws Exception {
        Ext2 ext = getExtensionLoader(Ext2.class).getDefaultExtension();
        assertNull(ext);

        String name = getExtensionLoader(Ext2.class).getDefaultExtensionName();
        assertNull(name);
    }

    @Test
    public void test_getExtension() throws Exception {
        assertTrue(getExtensionLoader(SimpleExt.class).getExtension("impl1") instanceof SimpleExtImpl1);
        assertTrue(getExtensionLoader(SimpleExt.class).getExtension("impl2") instanceof SimpleExtImpl2);
    }

    @Test
    public void test_getExtension_WithWrapper() throws Exception {
        WrappedExt impl1 = getExtensionLoader(WrappedExt.class).getExtension("impl1");
        assertThat(impl1, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));

        WrappedExt impl2 = getExtensionLoader(WrappedExt.class).getExtension("impl2");
        assertThat(impl2, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));


        URL url = new URL("p1", "1.2.3.4", 1010, "path1");
        int echoCount1 = Ext5Wrapper1.echoCount.get();
        int echoCount2 = Ext5Wrapper2.echoCount.get();

        assertEquals("Ext5Impl1-echo", impl1.echo(url, "ha"));
        assertEquals(echoCount1 + 1, Ext5Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext5Wrapper2.echoCount.get());
    }

    @Test
    public void test_getExtension_ExceptionNoExtension() throws Exception {
        try {
            getExtensionLoader(SimpleExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext1.SimpleExt by name XXX"));
        }
    }

    @Test
    public void test_getExtension_ExceptionNoExtension_WrapperNotAffactName() throws Exception {
        try {
            getExtensionLoader(WrappedExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext6_wrap.WrappedExt by name XXX"));
        }
    }

    @Test
    public void test_getExtension_ExceptionNullArg() throws Exception {
        try {
            getExtensionLoader(SimpleExt.class).getExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_hasExtension() throws Exception {
        assertTrue(getExtensionLoader(SimpleExt.class).hasExtension("impl1"));
        assertFalse(getExtensionLoader(SimpleExt.class).hasExtension("impl1,impl2"));
        assertFalse(getExtensionLoader(SimpleExt.class).hasExtension("xxx"));

        try {
            getExtensionLoader(SimpleExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_hasExtension_wrapperIsNotExt() throws Exception {
        assertTrue(getExtensionLoader(WrappedExt.class).hasExtension("impl1"));
        assertFalse(getExtensionLoader(WrappedExt.class).hasExtension("impl1,impl2"));
        assertFalse(getExtensionLoader(WrappedExt.class).hasExtension("xxx"));

        assertFalse(getExtensionLoader(WrappedExt.class).hasExtension("wrapper1"));

        try {
            getExtensionLoader(WrappedExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_getSupportedExtensions() throws Exception {
        Set<String> exts = getExtensionLoader(SimpleExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");

        assertEquals(expected, exts);
    }

    @Test
    public void test_getSupportedExtensions_wrapperIsNotExt() throws Exception {
        Set<String> exts = getExtensionLoader(WrappedExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");

        assertEquals(expected, exts);
    }

    @Test
    public void test_AddExtension() throws Exception {
        try {
            getExtensionLoader(AddExt1.class).getExtension("Manual1");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext8_add.AddExt1 by name Manual"));
        }

        getExtensionLoader(AddExt1.class).addExtension("Manual1", AddExt1_ManualAdd1.class);
        AddExt1 ext = getExtensionLoader(AddExt1.class).getExtension("Manual1");

        assertThat(ext, instanceOf(AddExt1_ManualAdd1.class));
        assertEquals("Manual1", getExtensionLoader(AddExt1.class).getExtensionName(AddExt1_ManualAdd1.class));
    }

    @Test
    public void test_AddExtension_NoExtend() throws Exception {
//        ExtensionLoader.getExtensionLoader(Ext9Empty.class).getSupportedExtensions();
        getExtensionLoader(Ext9Empty.class).addExtension("ext9", Ext9EmptyImpl.class);
        Ext9Empty ext = getExtensionLoader(Ext9Empty.class).getExtension("ext9");

        assertThat(ext, instanceOf(Ext9Empty.class));
        assertEquals("ext9", getExtensionLoader(Ext9Empty.class).getExtensionName(Ext9EmptyImpl.class));
    }

    @Test
    public void test_AddExtension_ExceptionWhenExistedExtension() throws Exception {
        SimpleExt ext = getExtensionLoader(SimpleExt.class).getExtension("impl1");

        try {
            getExtensionLoader(AddExt1.class).addExtension("impl1", AddExt1_ManualAdd1.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Extension name impl1 already exists (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt1)!"));
        }
    }

    @Test
    public void test_AddExtension_Adaptive() throws Exception {
        ExtensionLoader<AddExt2> loader = getExtensionLoader(AddExt2.class);
        loader.addExtension(null, AddExt2_ManualAdaptive.class);

        AddExt2 adaptive = loader.getAdaptiveExtension();
        assertTrue(adaptive instanceof AddExt2_ManualAdaptive);
    }

    @Test
    public void test_AddExtension_Adaptive_ExceptionWhenExistedAdaptive() throws Exception {
        ExtensionLoader<AddExt1> loader = getExtensionLoader(AddExt1.class);

        loader.getAdaptiveExtension();

        try {
            loader.addExtension(null, AddExt1_ManualAdaptive.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Adaptive Extension already exists (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt1)!"));
        }
    }

    @Test
    public void test_replaceExtension() throws Exception {
        try {
            getExtensionLoader(AddExt1.class).getExtension("Manual2");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext8_add.AddExt1 by name Manual"));
        }

        {
            AddExt1 ext = getExtensionLoader(AddExt1.class).getExtension("impl1");

            assertThat(ext, instanceOf(AddExt1Impl1.class));
            assertEquals("impl1", getExtensionLoader(AddExt1.class).getExtensionName(AddExt1Impl1.class));
        }
        {
            getExtensionLoader(AddExt1.class).replaceExtension("impl1", AddExt1_ManualAdd2.class);
            AddExt1 ext = getExtensionLoader(AddExt1.class).getExtension("impl1");

            assertThat(ext, instanceOf(AddExt1_ManualAdd2.class));
            assertEquals("impl1", getExtensionLoader(AddExt1.class).getExtensionName(AddExt1_ManualAdd2.class));
        }
    }

    @Test
    public void test_replaceExtension_Adaptive() throws Exception {
        ExtensionLoader<AddExt3> loader = getExtensionLoader(AddExt3.class);

        AddExt3 adaptive = loader.getAdaptiveExtension();
        assertFalse(adaptive instanceof AddExt3_ManualAdaptive);

        loader.replaceExtension(null, AddExt3_ManualAdaptive.class);

        adaptive = loader.getAdaptiveExtension();
        assertTrue(adaptive instanceof AddExt3_ManualAdaptive);
    }

    @Test
    public void test_replaceExtension_ExceptionWhenNotExistedExtension() throws Exception {
        AddExt1 ext = getExtensionLoader(AddExt1.class).getExtension("impl1");

        try {
            getExtensionLoader(AddExt1.class).replaceExtension("NotExistedExtension", AddExt1_ManualAdd1.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Extension name NotExistedExtension doesn't exist (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt1)"));
        }
    }

    @Test
    public void test_replaceExtension_Adaptive_ExceptionWhenNotExistedExtension() throws Exception {
        ExtensionLoader<AddExt4> loader = getExtensionLoader(AddExt4.class);

        try {
            loader.replaceExtension(null, AddExt4_ManualAdaptive.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Adaptive Extension doesn't exist (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt4)"));
        }
    }

    @Test
    public void test_InitError() throws Exception {
        ExtensionLoader<InitErrorExt> loader = getExtensionLoader(InitErrorExt.class);

        loader.getExtension("ok");

        try {
            loader.getExtension("error");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class (interface: interface org.apache.dubbo.common.extension.ext7.InitErrorExt"));
            assertThat(expected.getCause(), instanceOf(ExceptionInInitializerError.class));
        }
    }

    @Test
    public void testLoadActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "default_group");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);

        // test group
        url = url.addParameter(GROUP_KEY, "group1");
        list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "group1");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), GroupActivateExtImpl.class);

        // test old @Activate group
        url = url.addParameter(GROUP_KEY, "old_group");
        list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "old_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.get(0).getClass() == OldActivateExt1Impl2.class
                || list.get(0).getClass() == OldActivateExt1Impl3.class);

        // test value
        url = url.removeParameter(GROUP_KEY);
        url = url.addParameter(GROUP_KEY, "value");
        url = url.addParameter("value", "value");
        list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "value");
        Assertions.assertEquals(1, list.size());
        Assertions.assertSame(list.get(0).getClass(), ValueActivateExtImpl.class);

        // test order
        url = URL.valueOf("test://localhost/test");
        url = url.addParameter(GROUP_KEY, "order");
        list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "order");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        Assertions.assertSame(list.get(1).getClass(), OrderActivateExtImpl2.class);
    }

    @Test
    public void testLoadDefaultActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test?ext=order1,default");
        List<ActivateExt1> list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        Assertions.assertSame(list.get(1).getClass(), ActivateExt1Impl1.class);

        url = URL.valueOf("test://localhost/test?ext=default,order1");
        list = getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        Assertions.assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);
        Assertions.assertSame(list.get(1).getClass(), OrderActivateExtImpl1.class);
    }

    @Test
    public void testInjectExtension() {
        // test default
        InjectExt injectExt = getExtensionLoader(InjectExt.class).getExtension("injection");
        InjectExtImpl injectExtImpl = (InjectExtImpl) injectExt;
        Assertions.assertNotNull(injectExtImpl.getSimpleExt());
        Assertions.assertNull(injectExtImpl.getSimpleExt1());
        Assertions.assertNull(injectExtImpl.getGenericType());
    }

    @Test
    void testMultiNames() {
        Ext10MultiNames ext10MultiNames = getExtensionLoader(Ext10MultiNames.class).getExtension("impl");
        Assertions.assertNotNull(ext10MultiNames);
        ext10MultiNames = getExtensionLoader(Ext10MultiNames.class).getExtension("implMultiName");
        Assertions.assertNotNull(ext10MultiNames);
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> getExtensionLoader(Ext10MultiNames.class).getExtension("impl,implMultiName")
        );
    }

    @Test
    public void testGetOrDefaultExtension() {
        ExtensionLoader<InjectExt> loader = getExtensionLoader(InjectExt.class);
        InjectExt injectExt = loader.getOrDefaultExtension("non-exists");
        assertEquals(InjectExtImpl.class, injectExt.getClass());
        assertEquals(InjectExtImpl.class, loader.getOrDefaultExtension("injection").getClass());
    }

    @Test
    public void testGetSupported() {
        ExtensionLoader<InjectExt> loader = getExtensionLoader(InjectExt.class);
        assertEquals(1, loader.getSupportedExtensions().size());
        assertEquals(Collections.singleton("injection"), loader.getSupportedExtensions());
    }

    /**
     * @since 2.7.7
     */
    @Test
    public void testOverridden() {
        ExtensionLoader<Converter> loader = getExtensionLoader(Converter.class);

        Converter converter = loader.getExtension("string-to-boolean");
        assertEquals(String2BooleanConverter.class, converter.getClass());

        converter = loader.getExtension("string-to-double");
        assertEquals(String2DoubleConverter.class, converter.getClass());

        converter = loader.getExtension("string-to-integer");
        assertEquals(String2IntegerConverter.class, converter.getClass());

        assertEquals("string-to-boolean", loader.getExtensionName(String2BooleanConverter.class));
        assertEquals("string-to-boolean", loader.getExtensionName(StringToBooleanConverter.class));

        assertEquals("string-to-double", loader.getExtensionName(String2DoubleConverter.class));
        assertEquals("string-to-double", loader.getExtensionName(StringToDoubleConverter.class));

        assertEquals("string-to-integer", loader.getExtensionName(String2IntegerConverter.class));
        assertEquals("string-to-integer", loader.getExtensionName(StringToIntegerConverter.class));
    }

    /**
     * @since 2.7.7
     */
    @Test
    public void testGetLoadingStrategies() {
        List<LoadingStrategy> strategies = getLoadingStrategies();

        assertEquals(4, strategies.size());

        int i = 0;

        LoadingStrategy loadingStrategy = strategies.get(i++);
        assertEquals(DubboInternalLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.MAX_PRIORITY, loadingStrategy.getPriority());

        loadingStrategy = strategies.get(i++);
        assertEquals(DubboExternalLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.MAX_PRIORITY + 1, loadingStrategy.getPriority());


        loadingStrategy = strategies.get(i++);
        assertEquals(DubboLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.NORMAL_PRIORITY, loadingStrategy.getPriority());

        loadingStrategy = strategies.get(i++);
        assertEquals(ServicesLoadingStrategy.class, loadingStrategy.getClass());
        assertEquals(Prioritized.MIN_PRIORITY, loadingStrategy.getPriority());
    }
}
