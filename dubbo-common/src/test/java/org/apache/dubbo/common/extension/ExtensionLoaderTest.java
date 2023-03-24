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
import org.apache.dubbo.common.extension.duplicated.DuplicatedOverriddenExt;
import org.apache.dubbo.common.extension.duplicated.DuplicatedWithoutOverriddenExt;
import org.apache.dubbo.common.extension.ext1.SimpleExt;
import org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl1;
import org.apache.dubbo.common.extension.ext1.impl.SimpleExtImpl2;
import org.apache.dubbo.common.extension.ext10_multi_names.Ext10MultiNames;
import org.apache.dubbo.common.extension.ext11_no_adaptive.NoAdaptiveExt;
import org.apache.dubbo.common.extension.ext11_no_adaptive.NoAdaptiveExtImpl;
import org.apache.dubbo.common.extension.ext2.Ext2;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExt;
import org.apache.dubbo.common.extension.ext6_wrap.WrappedExtWrapper;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Impl1;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Impl3;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Wrapper1;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Wrapper2;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Wrapper3;
import org.apache.dubbo.common.extension.ext6_wrap.impl.Ext6Wrapper4;
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
import org.apache.dubbo.common.extension.wrapper.Demo;
import org.apache.dubbo.common.extension.wrapper.impl.DemoImpl;
import org.apache.dubbo.common.extension.wrapper.impl.DemoWrapper;
import org.apache.dubbo.common.extension.wrapper.impl.DemoWrapper2;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.extension.ExtensionLoader.getLoadingStrategies;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ExtensionLoaderTest {

    private <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return ApplicationModel.defaultModel().getExtensionDirector().getExtensionLoader(type);
    }

    @Test
    void test_getExtensionLoader_Null() {
        try {
            getExtensionLoader(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                containsString("Extension type == null"));
        }
    }

    @Test
    void test_getExtensionLoader_NotInterface() {
        try {
            getExtensionLoader(ExtensionLoaderTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                containsString("Extension type (class org.apache.dubbo.common.extension.ExtensionLoaderTest) is not an interface"));
        }
    }

    @Test
    void test_getExtensionLoader_NotSpiAnnotation() {
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
    void test_getDefaultExtension() {
        SimpleExt ext = getExtensionLoader(SimpleExt.class).getDefaultExtension();
        assertThat(ext, instanceOf(SimpleExtImpl1.class));

        String name = getExtensionLoader(SimpleExt.class).getDefaultExtensionName();
        assertEquals("impl1", name);
    }

    @Test
    void test_getDefaultExtension_NULL() {
        Ext2 ext = getExtensionLoader(Ext2.class).getDefaultExtension();
        assertNull(ext);

        String name = getExtensionLoader(Ext2.class).getDefaultExtensionName();
        assertNull(name);
    }

    @Test
    void test_getExtension() {
        assertTrue(getExtensionLoader(SimpleExt.class).getExtension("impl1") instanceof SimpleExtImpl1);
        assertTrue(getExtensionLoader(SimpleExt.class).getExtension("impl2") instanceof SimpleExtImpl2);
    }

    @Test
    void test_getExtension_WithWrapper() {
        WrappedExt impl1 = getExtensionLoader(WrappedExt.class).getExtension("impl1");
        assertThat(impl1, anyOf(instanceOf(Ext6Wrapper1.class), instanceOf(Ext6Wrapper2.class)));
        assertThat(impl1, instanceOf(WrappedExtWrapper.class));
        // get origin instance from wrapper
        WrappedExt originImpl1 = impl1;
        while (originImpl1 instanceof WrappedExtWrapper) {
            originImpl1 = ((WrappedExtWrapper) originImpl1).getOrigin();
        }

        // test unwrapped instance
        WrappedExt unwrappedImpl1 = getExtensionLoader(WrappedExt.class).getExtension("impl1", false);
        assertThat(unwrappedImpl1, instanceOf(Ext6Impl1.class));
        assertNotSame(unwrappedImpl1, impl1);
        assertSame(unwrappedImpl1, originImpl1);


        WrappedExt impl2 = getExtensionLoader(WrappedExt.class).getExtension("impl2");
        assertThat(impl2, anyOf(instanceOf(Ext6Wrapper1.class), instanceOf(Ext6Wrapper2.class)));


        URL url = new ServiceConfigURL("p1", "1.2.3.4", 1010, "path1");
        int echoCount1 = Ext6Wrapper1.echoCount.get();
        int echoCount2 = Ext6Wrapper2.echoCount.get();

        assertEquals("Ext6Impl1-echo", impl1.echo(url, "ha"));
        assertEquals(echoCount1 + 1, Ext6Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext6Wrapper2.echoCount.get());
    }

    @Test
    void test_getExtension_withWrapperAnnotation() {
        WrappedExt impl3 = getExtensionLoader(WrappedExt.class).getExtension("impl3");
        assertThat(impl3, instanceOf(Ext6Wrapper3.class));
        WrappedExt originImpl3 = impl3;
        while (originImpl3 instanceof WrappedExtWrapper) {
            originImpl3 = ((WrappedExtWrapper) originImpl3).getOrigin();
        }

        // test unwrapped instance
        WrappedExt unwrappedImpl3 = getExtensionLoader(WrappedExt.class).getExtension("impl3", false);
        assertThat(unwrappedImpl3, instanceOf(Ext6Impl3.class));
        assertNotSame(unwrappedImpl3, impl3);
        assertSame(unwrappedImpl3, originImpl3);

        WrappedExt impl4 = getExtensionLoader(WrappedExt.class).getExtension("impl4");
        assertThat(impl4, instanceOf(Ext6Wrapper4.class));

        URL url = new ServiceConfigURL("p1", "1.2.3.4", 1010, "path1");
        int echoCount3 = Ext6Wrapper3.echoCount.get();
        int echoCount4 = Ext6Wrapper4.echoCount.get();

        assertEquals("Ext6Impl4-echo", impl4.echo(url, "haha"));
        assertEquals(echoCount3, Ext6Wrapper3.echoCount.get());
        assertEquals(echoCount4 + 1, Ext6Wrapper4.echoCount.get());
    }

    @Test
    void test_getActivateExtension_WithWrapper() {
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, new String[]{}, "order");
        assertEquals(2, list.size());
    }

    @Test
    void test_getExtension_ExceptionNoExtension() {
        try {
            getExtensionLoader(SimpleExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext1.SimpleExt by name XXX"));
        }
    }

    @Test
    void test_getExtension_ExceptionNoExtension_WrapperNotAffactName() {
        try {
            getExtensionLoader(WrappedExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension org.apache.dubbo.common.extension.ext6_wrap.WrappedExt by name XXX"));
        }
    }

    @Test
    void test_getExtension_ExceptionNullArg() {
        try {
            getExtensionLoader(SimpleExt.class).getExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    void test_hasExtension() {
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
    void test_hasExtension_wrapperIsNotExt() {
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
    void test_getSupportedExtensions() {
        Set<String> exts = getExtensionLoader(SimpleExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");

        assertEquals(expected, exts);
    }

    @Test
    void test_getSupportedExtensions_wrapperIsNotExt() {
        Set<String> exts = getExtensionLoader(WrappedExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");
        expected.add("impl4");

        assertEquals(expected, exts);
    }

    @Test
    void test_AddExtension() {
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
    void test_AddExtension_NoExtend() {
        getExtensionLoader(Ext9Empty.class).addExtension("ext9", Ext9EmptyImpl.class);
        Ext9Empty ext = getExtensionLoader(Ext9Empty.class).getExtension("ext9");

        assertThat(ext, instanceOf(Ext9Empty.class));
        assertEquals("ext9", getExtensionLoader(Ext9Empty.class).getExtensionName(Ext9EmptyImpl.class));
    }

    @Test
    void test_AddExtension_ExceptionWhenExistedExtension() {
        SimpleExt ext = getExtensionLoader(SimpleExt.class).getExtension("impl1");

        try {
            getExtensionLoader(AddExt1.class).addExtension("impl1", AddExt1_ManualAdd1.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Extension name impl1 already exists (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt1)!"));
        }
    }

    @Test
    void test_AddExtension_Adaptive() {
        ExtensionLoader<AddExt2> loader = getExtensionLoader(AddExt2.class);
        loader.addExtension(null, AddExt2_ManualAdaptive.class);

        AddExt2 adaptive = loader.getAdaptiveExtension();
        assertTrue(adaptive instanceof AddExt2_ManualAdaptive);
    }

    @Test
    void test_AddExtension_Adaptive_ExceptionWhenExistedAdaptive() {
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
    void test_addExtension_with_error_class() {
        try {
            getExtensionLoader(SimpleExt.class).addExtension("impl1", ExtensionLoaderTest.class);
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                containsString("Input type class org.apache.dubbo.common.extension.ExtensionLoaderTest " +
                    "doesn't implement the Extension interface org.apache.dubbo.common.extension.ext1.SimpleExt"));
        }
    }

    @Test
    void test_addExtension_with_interface() {
        try {
            getExtensionLoader(SimpleExt.class).addExtension("impl1", SimpleExt.class);
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                containsString("Input type interface org.apache.dubbo.common.extension.ext1.SimpleExt " +
                    "can't be interface!"));
        }
    }

    @Test
    void test_addExtension_without_adaptive_annotation() {
        try {
            getExtensionLoader(NoAdaptiveExt.class).addExtension(null, NoAdaptiveExtImpl.class);
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                containsString("Extension name is blank " +
                    "(Extension interface org.apache.dubbo.common.extension.ext11_no_adaptive.NoAdaptiveExt)!"));
        }
    }

    @Test
    void test_getLoadedExtension_name_with_null() {
        try {
            getExtensionLoader(SimpleExt.class).getLoadedExtension(null);
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    void test_getLoadedExtension_null() {
        SimpleExt impl1 = getExtensionLoader(SimpleExt.class).getLoadedExtension("XXX");
        assertNull(impl1);
    }

    @Test
    void test_getLoadedExtension() {
        SimpleExt simpleExt = getExtensionLoader(SimpleExt.class).getExtension("impl1");
        assertThat(simpleExt, instanceOf(SimpleExtImpl1.class));

        SimpleExt simpleExt1 = getExtensionLoader(SimpleExt.class).getLoadedExtension("impl1");
        assertThat(simpleExt1, instanceOf(SimpleExtImpl1.class));
    }

    @Test
    void test_getLoadedExtensions() {
        SimpleExt simpleExt1 = getExtensionLoader(SimpleExt.class).getExtension("impl1");
        assertThat(simpleExt1, instanceOf(SimpleExtImpl1.class));

        SimpleExt simpleExt2 = getExtensionLoader(SimpleExt.class).getExtension("impl2");
        assertThat(simpleExt2, instanceOf(SimpleExtImpl2.class));

        Set<String> loadedExtensions = getExtensionLoader(SimpleExt.class).getLoadedExtensions();
        Assertions.assertNotNull(loadedExtensions);
    }

    @Test
    void test_getLoadedExtensionInstances() {
        SimpleExt simpleExt1 = getExtensionLoader(SimpleExt.class).getExtension("impl1");
        assertThat(simpleExt1, instanceOf(SimpleExtImpl1.class));

        SimpleExt simpleExt2 = getExtensionLoader(SimpleExt.class).getExtension("impl2");
        assertThat(simpleExt2, instanceOf(SimpleExtImpl2.class));

        List<SimpleExt> loadedExtensionInstances = getExtensionLoader(SimpleExt.class).getLoadedExtensionInstances();
        Assertions.assertNotNull(loadedExtensionInstances);
    }

    @Test
    void test_replaceExtension_with_error_class() {
        try {
            getExtensionLoader(SimpleExt.class).replaceExtension("impl1", ExtensionLoaderTest.class);
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                containsString("Input type class org.apache.dubbo.common.extension.ExtensionLoaderTest " +
                    "doesn't implement Extension interface org.apache.dubbo.common.extension.ext1.SimpleExt"));
        }
    }

    @Test
    void test_replaceExtension_with_interface() {
        try {
            getExtensionLoader(SimpleExt.class).replaceExtension("impl1", SimpleExt.class);
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                containsString("Input type interface org.apache.dubbo.common.extension.ext1.SimpleExt " +
                    "can't be interface!"));
        }
    }

    @Test
    void test_replaceExtension() {
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
    void test_replaceExtension_Adaptive() {
        ExtensionLoader<AddExt3> loader = getExtensionLoader(AddExt3.class);

        AddExt3 adaptive = loader.getAdaptiveExtension();
        assertFalse(adaptive instanceof AddExt3_ManualAdaptive);

        loader.replaceExtension(null, AddExt3_ManualAdaptive.class);

        adaptive = loader.getAdaptiveExtension();
        assertTrue(adaptive instanceof AddExt3_ManualAdaptive);
    }

    @Test
    void test_replaceExtension_ExceptionWhenNotExistedExtension() {
        AddExt1 ext = getExtensionLoader(AddExt1.class).getExtension("impl1");

        try {
            getExtensionLoader(AddExt1.class).replaceExtension("NotExistedExtension", AddExt1_ManualAdd1.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Extension name NotExistedExtension doesn't exist (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt1)"));
        }
    }

    @Test
    void test_replaceExtension_Adaptive_ExceptionWhenNotExistedExtension() {
        ExtensionLoader<AddExt4> loader = getExtensionLoader(AddExt4.class);

        try {
            loader.replaceExtension(null, AddExt4_ManualAdaptive.class);
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Adaptive Extension doesn't exist (Extension interface org.apache.dubbo.common.extension.ext8_add.AddExt4)"));
        }
    }

    @Test
    void test_InitError() {
        ExtensionLoader<InitErrorExt> loader = getExtensionLoader(InitErrorExt.class);

        loader.getExtension("ok");

        try {
            loader.getExtension("error");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class (interface: interface org.apache.dubbo.common.extension.ext7.InitErrorExt"));
            assertThat(expected.getMessage(), containsString("java.lang.ExceptionInInitializerError"));
        }
    }

    @Test
    void testLoadActivateExtension() {
        // test default
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, new String[]{}, "default_group");
        Assertions.assertEquals(1, list.size());
        assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);

        // test group
        url = url.addParameter(GROUP_KEY, "group1");
        list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, new String[]{}, "group1");
        Assertions.assertEquals(1, list.size());
        assertSame(list.get(0).getClass(), GroupActivateExtImpl.class);

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
        assertSame(list.get(0).getClass(), ValueActivateExtImpl.class);

        // test order
        url = URL.valueOf("test://localhost/test");
        url = url.addParameter(GROUP_KEY, "order");
        list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, new String[]{}, "order");
        Assertions.assertEquals(2, list.size());
        assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        assertSame(list.get(1).getClass(), OrderActivateExtImpl2.class);
    }

    @Test
    void testLoadDefaultActivateExtension() {
        // test default
        URL url = URL.valueOf("test://localhost/test?ext=order1,default");
        List<ActivateExt1> list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        assertSame(list.get(0).getClass(), OrderActivateExtImpl1.class);
        assertSame(list.get(1).getClass(), ActivateExt1Impl1.class);

        url = URL.valueOf("test://localhost/test?ext=default,order1");
        list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);
        assertSame(list.get(1).getClass(), OrderActivateExtImpl1.class);

        url = URL.valueOf("test://localhost/test?ext=order1");
        list = getExtensionLoader(ActivateExt1.class)
            .getActivateExtension(url, "ext", "default_group");
        Assertions.assertEquals(2, list.size());
        assertSame(list.get(0).getClass(), ActivateExt1Impl1.class);
        assertSame(list.get(1).getClass(), OrderActivateExtImpl1.class);
    }

    @Test
    void testInjectExtension() {
        // register bean for test ScopeBeanExtensionInjector
        DemoImpl demoBean = new DemoImpl();
        ApplicationModel.defaultModel().getBeanFactory().registerBean(demoBean);
        // test default
        InjectExt injectExt = getExtensionLoader(InjectExt.class).getExtension("injection");
        InjectExtImpl injectExtImpl = (InjectExtImpl) injectExt;
        Assertions.assertNotNull(injectExtImpl.getSimpleExt());
        Assertions.assertNull(injectExtImpl.getSimpleExt1());
        Assertions.assertNull(injectExtImpl.getGenericType());
        Assertions.assertSame(demoBean, injectExtImpl.getDemo());
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
    void testGetOrDefaultExtension() {
        ExtensionLoader<InjectExt> loader = getExtensionLoader(InjectExt.class);
        InjectExt injectExt = loader.getOrDefaultExtension("non-exists");
        assertEquals(InjectExtImpl.class, injectExt.getClass());
        assertEquals(InjectExtImpl.class, loader.getOrDefaultExtension("injection").getClass());
    }

    @Test
    void testGetSupported() {
        ExtensionLoader<InjectExt> loader = getExtensionLoader(InjectExt.class);
        assertEquals(1, loader.getSupportedExtensions().size());
        assertEquals(Collections.singleton("injection"), loader.getSupportedExtensions());
    }

    /**
     * @since 2.7.7
     */
    @Test
    void testOverridden() {
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
    void testGetLoadingStrategies() {
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

    @Test
    void testDuplicatedImplWithoutOverriddenStrategy() {
        List<LoadingStrategy> loadingStrategies = ExtensionLoader.getLoadingStrategies();
        ExtensionLoader.setLoadingStrategies(new DubboExternalLoadingStrategyTest(false),
            new DubboInternalLoadingStrategyTest(false));
        ExtensionLoader<DuplicatedWithoutOverriddenExt> extensionLoader = getExtensionLoader(DuplicatedWithoutOverriddenExt.class);
        try {
            extensionLoader.getExtension("duplicated");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class (interface: interface org.apache.dubbo.common.extension.duplicated.DuplicatedWithoutOverriddenExt"));
            assertThat(expected.getMessage(), containsString("cause: Duplicate extension org.apache.dubbo.common.extension.duplicated.DuplicatedWithoutOverriddenExt name duplicated"));
        } finally {
            //recover the loading strategies
            ExtensionLoader.setLoadingStrategies(loadingStrategies.toArray(new LoadingStrategy[loadingStrategies.size()]));
        }
    }

    @Test
    void testDuplicatedImplWithOverriddenStrategy() {
        List<LoadingStrategy> loadingStrategies = ExtensionLoader.getLoadingStrategies();
        ExtensionLoader.setLoadingStrategies(new DubboExternalLoadingStrategyTest(true),
            new DubboInternalLoadingStrategyTest(true));
        ExtensionLoader<DuplicatedOverriddenExt> extensionLoader = getExtensionLoader(DuplicatedOverriddenExt.class);
        DuplicatedOverriddenExt duplicatedOverriddenExt = extensionLoader.getExtension("duplicated");
        assertEquals("DuplicatedOverriddenExt1", duplicatedOverriddenExt.echo());
        //recover the loading strategies
        ExtensionLoader.setLoadingStrategies(loadingStrategies.toArray(new LoadingStrategy[loadingStrategies.size()]));
    }

    @Test
    void testLoadByDubboInternalSPI() {
        ExtensionLoader<SPI1> extensionLoader = getExtensionLoader(SPI1.class);
        SPI1 spi1 = extensionLoader.getExtension("1", true);
        assertNotNull(spi1);

        ExtensionLoader<SPI2> extensionLoader2 = getExtensionLoader(SPI2.class);
        SPI2 spi2 = extensionLoader2.getExtension("2", true);
        assertNotNull(spi2);

        try {
            ExtensionLoader<SPI3> extensionLoader3 = getExtensionLoader(SPI3.class);
            SPI3 spi3 = extensionLoader3.getExtension("3", true);
            assertNotNull(spi3);
        } catch (IllegalStateException illegalStateException) {
            if (!illegalStateException.getMessage().contains("No such extension")) {
                fail();
            }
        }

        ExtensionLoader<SPI4> extensionLoader4 = getExtensionLoader(SPI4.class);
        SPI4 spi4 = extensionLoader4.getExtension("4", true);
        assertNotNull(spi4);

    }

    @Test
    void isWrapperClass() {
        assertFalse(getExtensionLoader(Demo.class).isWrapperClass(DemoImpl.class));
        assertTrue(getExtensionLoader(Demo.class).isWrapperClass(DemoWrapper.class));
        assertTrue(getExtensionLoader(Demo.class).isWrapperClass(DemoWrapper2.class));
    }

    /**
     * The external {@link LoadingStrategy}, which can set if it supports overriding.
     */
    private static class DubboExternalLoadingStrategyTest implements LoadingStrategy {

        public DubboExternalLoadingStrategyTest(boolean overridden) {
            this.overridden = overridden;
        }

        private boolean overridden;

        @Override
        public String directory() {
            return "META-INF/dubbo/external/";
        }

        @Override
        public boolean overridden() {
            return this.overridden;
        }

        @Override
        public int getPriority() {
            return MAX_PRIORITY + 1;
        }
    }

    /**
     * The internal {@link LoadingStrategy}, which can set if it support overridden
     */
    private static class DubboInternalLoadingStrategyTest implements LoadingStrategy {

        public DubboInternalLoadingStrategyTest(boolean overridden) {
            this.overridden = overridden;
        }

        private boolean overridden;

        @Override
        public String directory() {
            return "META-INF/dubbo/internal/";
        }

        @Override
        public boolean overridden() {
            return this.overridden;
        }

        @Override
        public int getPriority() {
            return MAX_PRIORITY;
        }
    }
}
