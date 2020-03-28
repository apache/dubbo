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
import org.apache.dubbo.common.extension.adaptive.AdaptiveExt_HasMethods;
import org.apache.dubbo.common.extension.adaptive.AdaptiveExt_InnerUrl;
import org.apache.dubbo.common.extension.adaptive.AdaptiveExt_NoneUrl;
import org.apache.dubbo.common.extension.adaptive.HasAdaptiveExt;
import org.apache.dubbo.common.extension.adaptive.NoneAdaptiveExt;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.Invocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link AdaptiveClassCodeGenerator} Test
 *
 * @since 2.7.5
 */
public class AdaptiveClassCodeGeneratorTest {

    @Test
    public void testFail() {
        AdaptiveClassCodeGenerator generate = new AdaptiveClassCodeGenerator(NoneAdaptiveExt.class, "adaptive");
        Assertions.assertThrows(IllegalStateException.class, generate::generate);

        generate = new AdaptiveClassCodeGenerator(AdaptiveExt_NoneUrl.class, "adaptive");
        Assertions.assertThrows(IllegalStateException.class, generate::generate);
    }

    @Test
    public void testGenerate() throws Exception {
        AdaptiveClassCodeGenerator generator = new AdaptiveClassCodeGenerator(HasAdaptiveExt.class, "adaptive");
        String code = generator.generate();
        // compile and test
        Object object = compileClass(code);
        Assertions.assertEquals("Hello Echo1",
                object.getClass().getDeclaredMethod("echo", URL.class, String.class)
                        .invoke(object,
                                URL.valueOf("dubbo://1.2.3.4:1234/HasAdaptiveExt?has.adaptive.ext=impl1"),
                                "Echo1"));

        generator = new AdaptiveClassCodeGenerator(AdaptiveExt_HasMethods.class, "impl");
        code = generator.generate();
        // compile and test
        object = compileClass(code);
        Assertions.assertEquals("Hello Echo2",
                object.getClass().getDeclaredMethod("echo1", URL.class, String.class)
                        .invoke(object,
                                URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_HasMethods?adaptive.ext_.has.methods=impl"),
                                "Echo2"));
        Assertions.assertEquals("Hello Echo2",
                object.getClass().getDeclaredMethod("echo2", URL.class, String.class)
                        .invoke(object,
                                URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_HasMethods?adaptive.ext_.has.methods=impl"),
                                "Echo2"));
        Assertions.assertEquals("Hello Echo3",
                object.getClass().getDeclaredMethod("echo3", URL.class, String.class, Invocation.class)
                        .invoke(object,
                                URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_HasMethods?adaptive.ext_.has.methods=impl"),
                                "Echo3",
                                (Invocation) () -> "AdaptiveExt_HasMethods"));
        // void return
        object.getClass().getDeclaredMethod("echo4", URL.class, String.class)
                .invoke(object,
                        URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_HasMethods?adaptive.ext_.has.methods=impl"),
                        "Echo4");

        generator = new AdaptiveClassCodeGenerator(AdaptiveExt_InnerUrl.class, "impl");
        code = generator.generate();
        // compile and test
        object = compileClass(code);
        AdaptiveExt_InnerUrl.InnerUrl1 innerUrl1 = new AdaptiveExt_InnerUrl.InnerUrl1();
        innerUrl1.setUrl(URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_InnerUrl?adaptive.ext_.inner_url=impl"));
        Assertions.assertEquals("Hello Echo1",
                object.getClass().getDeclaredMethod("echo1", AdaptiveExt_InnerUrl.InnerUrl1.class, String.class)
                        .invoke(object,
                                innerUrl1,
                                "Echo1"));
        AdaptiveExt_InnerUrl.InnerUrl2 innerUrl2 = new AdaptiveExt_InnerUrl.InnerUrl2();
        innerUrl2.setUrl(URL.valueOf("dubbo://1.2.3.4:1234/AdaptiveExt_InnerUrl?adaptive=impl"));
        Assertions.assertEquals("Hello Echo2",
                object.getClass().getDeclaredMethod("echo2", String.class, AdaptiveExt_InnerUrl.InnerUrl2.class)
                        .invoke(object,
                                "Echo2",
                                innerUrl2));
    }

    private Object compileClass(String code) throws InstantiationException, IllegalAccessException {
        ClassLoader classLoader = ClassUtils.getClassLoader(AdaptiveClassCodeGeneratorTest.class);
        org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        Class<?> aClass = compiler.compile(code, classLoader);
        return aClass.newInstance();
    }
}
