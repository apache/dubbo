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
package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;

import junit.framework.TestCase;

public class LocaleSerializerTest extends SerializeTestBase {

    /** {@linkplain LocaleSerializer#writeObject(Object, AbstractHessianOutput)} */
    @Test
    public void locale() throws IOException {
        assertLocale(null);
        assertLocale(new Locale(""));
        assertLocale(new Locale("zh"));
        assertLocale(new Locale("zh", "CN"));
        assertLocale(new Locale("zh-hant", "CN"));
        assertLocale(new Locale("zh-hant", "CN", "GBK"));
    }

    private void assertLocale(Locale locale) throws IOException {
        TestCase.assertEquals(locale, baseHessian2Serialize(locale));
        TestCase.assertEquals(locale, baseHessianSerialize(locale));
    }
}