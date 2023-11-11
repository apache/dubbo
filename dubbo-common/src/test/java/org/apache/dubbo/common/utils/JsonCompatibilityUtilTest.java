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

import org.apache.dubbo.common.utils.json.Service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonCompatibilityUtilTest {

    private static Class<?> service = Service.class;

    private static final Logger logger = LoggerFactory.getLogger(JsonCompatibilityUtil.class);

    @Test
    public void testCheckClassCompatibility() {
        boolean res = JsonCompatibilityUtil.checkClassCompatibility(service);
        assertFalse(res);
    }

    @Test
    public void testGetUnsupportedMethods() {
        List<String> res = JsonCompatibilityUtil.getUnsupportedMethods(service);
        assert res != null;
        logger.info(res.toString());
        assert (res.size() != 0);
    }

    @Test
    public void testInt() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testInt"));
        assertTrue(res);
    }

    @Test
    public void testIntArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testIntArr"));
        assertTrue(res);
    }

    @Test
    public void testInteger() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testInteger"));
        assertTrue(res);
    }

    @Test
    public void testIntegerArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testIntegerArr"));
        assertTrue(res);
    }

    @Test
    public void testIntegerList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testIntegerList"));
        assertTrue(res);
    }

    @Test
    public void testShort() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testShort"));
        assertTrue(res);
    }

    @Test
    public void testShortArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testShortArr"));
        assertTrue(res);
    }

    @Test
    public void testSShort() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testSShort"));
        assertTrue(res);
    }

    @Test
    public void testSShortArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testSShortArr"));
        assertTrue(res);
    }

    @Test
    public void testShortList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testShortList"));
        assertTrue(res);
    }

    @Test
    public void testByte() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testByte"));
        assertTrue(res);
    }

    @Test
    public void testByteArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testByteArr"));
        assertTrue(res);
    }

    @Test
    public void testBByte() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBByte"));
        assertTrue(res);
    }

    @Test
    public void testBByteArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBByteArr"));
        assertTrue(res);
    }

    @Test
    public void testByteList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testByteList"));
        assertTrue(res);
    }

    @Test
    public void testFloat() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testFloat"));
        assertTrue(res);
    }

    @Test
    public void testFloatArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testFloatArr"));
        assertTrue(res);
    }

    @Test
    public void testFFloat() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testFFloat"));
        assertTrue(res);
    }

    @Test
    public void testFloatArray() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testFloatArray"));
        assertTrue(res);
    }

    @Test
    public void testFloatList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testFloatList"));
        assertTrue(res);
    }

    @Test
    public void testBoolean() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBoolean"));
        assertTrue(res);
    }

    @Test
    public void testBooleanArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBooleanArr"));
        assertTrue(res);
    }

    @Test
    public void testBBoolean() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBBoolean"));
        assertTrue(res);
    }

    @Test
    public void testBooleanArray() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBooleanArray"));
        assertTrue(res);
    }

    @Test
    public void testBooleanList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testBooleanList"));
        assertTrue(res);
    }

    @Test
    public void testChar() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testChar"));
        assertTrue(res);
    }

    @Test
    public void testCharArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCharArr"));
        assertTrue(res);
    }

    @Test
    public void testCharacter() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCharacter"));
        assertTrue(res);
    }

    @Test
    public void testCharacterArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCharacterArr"));
        assertTrue(res);
    }

    @Test
    public void testCharacterList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCharacterList"));
        assertTrue(res);
    }

    @Test
    public void testCharacterListArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCharacterListArr"));
        assertTrue(res);
    }

    @Test
    public void testString() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testString"));
        assertTrue(res);
    }

    @Test
    public void testStringArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testStringArr"));
        assertTrue(res);
    }

    @Test
    public void testStringList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testStringList"));
        assertTrue(res);
    }

    @Test
    public void testStringListArr() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testStringListArr"));
        assertTrue(res);
    }

    @Test
    public void testDate() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testDate"));
        assertTrue(res);
    }

    @Test
    public void testCalendar() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testCalendar"));
        assertFalse(res);
    }

    @Test
    public void testLocalTime() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testLocalTime"));
        assertTrue(res);
    }

    @Test
    public void testLocalDate() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testLocalDate"));
        assertTrue(res);
    }

    @Test
    public void testLocalDateTime() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testLocalDateTime"));
        assertTrue(res);
    }

    @Test
    public void testZoneDateTime() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testZoneDateTime"));
        assertTrue(res);
    }

    @Test
    public void testMap() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testMap"));
        assertTrue(res);
    }

    @Test
    public void testSet() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testSet"));
        assertTrue(res);
    }

    @Test
    public void testOptionalEmpty() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testOptionalEmpty"));
        assertFalse(res);
    }

    @Test
    public void testOptionalInteger() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testOptionalInteger"));
        assertFalse(res);
    }

    @Test
    public void testOptionalString() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testOptionalString"));
        assertFalse(res);
    }

    @Test
    public void testEnum() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testEnum"));
        assertTrue(res);
    }

    @Test
    public void testRecord() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testRecord"));
        assertTrue(res);
    }

    @Test
    public void testInterface() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testInterface"));
        assertFalse(res);
    }

    @Test
    public void testObject() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testObject"));
        assertTrue(res);
    }

    @Test
    public void testObjectList() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testObjectList"));
        assertTrue(res);
    }

    @Test
    public void testTemplate() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testTemplate"));
        assertTrue(res);
    }

    @Test
    public void testStream() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testStream"));
        assertFalse(res);
    }

    @Test
    public void testIterator() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testIterator"));
        assertFalse(res);
    }

    @Test
    public void testAbstract() throws NoSuchMethodException {
        boolean res = JsonCompatibilityUtil.checkMethodCompatibility(service.getDeclaredMethod("testAbstract"));
        assertFalse(res);
    }
}
