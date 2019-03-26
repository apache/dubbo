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
package org.apache.dubbo.config.spring.convert.converter;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link StringArrayToMapConverter} Test
 */
public class StringArrayToMapConverterTest {

    @Test
    public void testConvert() {

        StringArrayToMapConverter converter = new StringArrayToMapConverter();

        Map<String, String> value = converter.convert(new String[]{"Hello", "World"});

        Map<String, String> expected = new LinkedHashMap<String, String>();

        expected.put("Hello", "World");

        Assert.assertEquals(expected, value);

        value = converter.convert(new String[]{});

        Assert.assertNull(value);

        value = converter.convert(null);

        Assert.assertNull(value);

    }
}
