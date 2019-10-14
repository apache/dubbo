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
package org.apache.dubbo.metadata.annotation.processing.util;

import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getAllDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getDeclaredField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getDeclaredFields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link FieldUtils} Test
 *
 * @since 2.7.5
 */
public class FieldUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    public void testDeclaredFields() {
        TypeElement type = getType(Model.class);
        List<VariableElement> fields = getDeclaredFields(type.asType());
        assertEquals(6, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());

        fields = getAllDeclaredFields(type.asType());
        assertEquals(11, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());
        assertEquals("b", fields.get(6).getSimpleName().toString());
        assertEquals("s", fields.get(7).getSimpleName().toString());
        assertEquals("i", fields.get(8).getSimpleName().toString());
        assertEquals("l", fields.get(9).getSimpleName().toString());
        assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    @Test
    public void testGetDeclaredField() {
        TypeElement type = getType(Model.class);
        testGetDeclaredField(type, "f", float.class);
        testGetDeclaredField(type, "d", double.class);
        testGetDeclaredField(type, "tu", TimeUnit.class);
        testGetDeclaredField(type, "str", String.class);
        testGetDeclaredField(type, "bi", BigInteger.class);
        testGetDeclaredField(type, "bd", BigDecimal.class);

        assertNull(getDeclaredField(type.asType(), "b"));
        assertNull(getDeclaredField(type.asType(), "s"));
        assertNull(getDeclaredField(type.asType(), "i"));
        assertNull(getDeclaredField(type.asType(), "l"));
        assertNull(getDeclaredField(type.asType(), "z"));
    }

    @Test
    public void testFindField() {
        TypeElement type = getType(Model.class);
        testFindField(type, "f", float.class);
        testFindField(type, "d", double.class);
        testFindField(type, "tu", TimeUnit.class);
        testFindField(type, "str", String.class);
        testFindField(type, "bi", BigInteger.class);
        testFindField(type, "bd", BigDecimal.class);
        testFindField(type, "b", byte.class);
        testFindField(type, "s", short.class);
        testFindField(type, "i", int.class);
        testFindField(type, "l", long.class);
        testFindField(type, "z", boolean.class);
    }

    private void testGetDeclaredField(TypeElement type, String fieldName, Type fieldType) {
        VariableElement field = getDeclaredField(type.asType(), fieldName);
        assertField(field, fieldName, fieldType);
    }

    private void testFindField(TypeElement type, String fieldName, Type fieldType) {
        VariableElement field = findField(type.asType(), fieldName);
        assertField(field, fieldName, fieldType);
    }

    private void assertField(VariableElement field, String fieldName, Type fieldType) {
        assertEquals(fieldName, field.getSimpleName().toString());
        assertEquals(fieldType.getTypeName(), field.asType().toString());
    }
}
