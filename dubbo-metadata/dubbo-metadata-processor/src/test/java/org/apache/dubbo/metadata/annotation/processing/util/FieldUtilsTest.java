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
import org.apache.dubbo.metadata.annotation.processing.model.Color;
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getAllDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getAllNonStaticFields;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getDeclaredField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.getNonStaticFields;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.isEnumMemberField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.isField;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.isNonStaticField;

/**
 * {@link FieldUtils} Test
 *
 * @since 2.7.6
 */
class FieldUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {}

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    void testGetDeclaredFields() {
        TypeElement type = getType(Model.class);
        List<VariableElement> fields = getDeclaredFields(type);
        assertModelFields(fields);

        fields = getDeclaredFields(type.asType());
        assertModelFields(fields);

        Assertions.assertTrue(getDeclaredFields((Element) null).isEmpty());
        Assertions.assertTrue(getDeclaredFields((TypeMirror) null).isEmpty());

        fields = getDeclaredFields(type, f -> "f".equals(f.getSimpleName().toString()));
        Assertions.assertEquals(1, fields.size());
        Assertions.assertEquals("f", fields.get(0).getSimpleName().toString());
    }

    @Test
    void testGetAllDeclaredFields() {
        TypeElement type = getType(Model.class);

        List<VariableElement> fields = getAllDeclaredFields(type);

        assertModelAllFields(fields);

        Assertions.assertTrue(getAllDeclaredFields((Element) null).isEmpty());
        Assertions.assertTrue(getAllDeclaredFields((TypeMirror) null).isEmpty());

        fields = getAllDeclaredFields(type, f -> "f".equals(f.getSimpleName().toString()));
        Assertions.assertEquals(1, fields.size());
        Assertions.assertEquals("f", fields.get(0).getSimpleName().toString());
    }

    @Test
    void testGetDeclaredField() {
        TypeElement type = getType(Model.class);
        testGetDeclaredField(type, "f", float.class);
        testGetDeclaredField(type, "d", double.class);
        testGetDeclaredField(type, "tu", TimeUnit.class);
        testGetDeclaredField(type, "str", String.class);
        testGetDeclaredField(type, "bi", BigInteger.class);
        testGetDeclaredField(type, "bd", BigDecimal.class);

        Assertions.assertNull(getDeclaredField(type, "b"));
        Assertions.assertNull(getDeclaredField(type, "s"));
        Assertions.assertNull(getDeclaredField(type, "i"));
        Assertions.assertNull(getDeclaredField(type, "l"));
        Assertions.assertNull(getDeclaredField(type, "z"));

        Assertions.assertNull(getDeclaredField((Element) null, "z"));
        Assertions.assertNull(getDeclaredField((TypeMirror) null, "z"));
    }

    @Test
    void testFindField() {
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

        Assertions.assertNull(findField((Element) null, "f"));
        Assertions.assertNull(findField((Element) null, null));

        Assertions.assertNull(findField((TypeMirror) null, "f"));
        Assertions.assertNull(findField((TypeMirror) null, null));

        Assertions.assertNull(findField(type, null));
        Assertions.assertNull(findField(type.asType(), null));
    }

    @Test
    void testIsEnumField() {
        TypeElement type = getType(Color.class);
        VariableElement field = findField(type, "RED");
        Assertions.assertTrue(isEnumMemberField(field));

        field = findField(type, "YELLOW");
        Assertions.assertTrue(isEnumMemberField(field));

        field = findField(type, "BLUE");
        Assertions.assertTrue(isEnumMemberField(field));

        type = getType(Model.class);
        field = findField(type, "f");
        Assertions.assertFalse(isEnumMemberField(field));

        Assertions.assertFalse(isEnumMemberField(null));
    }

    @Test
    void testIsNonStaticField() {
        TypeElement type = getType(Model.class);
        Assertions.assertTrue(isNonStaticField(findField(type, "f")));

        type = getType(Color.class);
        Assertions.assertFalse(isNonStaticField(findField(type, "BLUE")));
    }

    @Test
    void testIsField() {
        TypeElement type = getType(Model.class);
        Assertions.assertTrue(isField(findField(type, "f")));
        Assertions.assertTrue(isField(findField(type, "f"), PRIVATE));

        type = getType(Color.class);
        Assertions.assertTrue(isField(findField(type, "BLUE"), PUBLIC, STATIC, FINAL));

        Assertions.assertFalse(isField(null));
        Assertions.assertFalse(isField(null, PUBLIC, STATIC, FINAL));
    }

    @Test
    void testGetNonStaticFields() {
        TypeElement type = getType(Model.class);

        List<VariableElement> fields = getNonStaticFields(type);
        assertModelFields(fields);

        fields = getNonStaticFields(type.asType());
        assertModelFields(fields);

        Assertions.assertTrue(getAllNonStaticFields((Element) null).isEmpty());
        Assertions.assertTrue(getAllNonStaticFields((TypeMirror) null).isEmpty());
    }

    @Test
    void testGetAllNonStaticFields() {
        TypeElement type = getType(Model.class);

        List<VariableElement> fields = getAllNonStaticFields(type);
        assertModelAllFields(fields);

        fields = getAllNonStaticFields(type.asType());
        assertModelAllFields(fields);

        Assertions.assertTrue(getAllNonStaticFields((Element) null).isEmpty());
        Assertions.assertTrue(getAllNonStaticFields((TypeMirror) null).isEmpty());
    }

    private void assertModelFields(List<VariableElement> fields) {
        Assertions.assertEquals(6, fields.size());
        Assertions.assertEquals("d", fields.get(1).getSimpleName().toString());
        Assertions.assertEquals("tu", fields.get(2).getSimpleName().toString());
        Assertions.assertEquals("str", fields.get(3).getSimpleName().toString());
        Assertions.assertEquals("bi", fields.get(4).getSimpleName().toString());
        Assertions.assertEquals("bd", fields.get(5).getSimpleName().toString());
    }

    private void assertModelAllFields(List<VariableElement> fields) {
        Assertions.assertEquals(11, fields.size());
        Assertions.assertEquals("f", fields.get(0).getSimpleName().toString());
        Assertions.assertEquals("d", fields.get(1).getSimpleName().toString());
        Assertions.assertEquals("tu", fields.get(2).getSimpleName().toString());
        Assertions.assertEquals("str", fields.get(3).getSimpleName().toString());
        Assertions.assertEquals("bi", fields.get(4).getSimpleName().toString());
        Assertions.assertEquals("bd", fields.get(5).getSimpleName().toString());
        Assertions.assertEquals("b", fields.get(6).getSimpleName().toString());
        Assertions.assertEquals("s", fields.get(7).getSimpleName().toString());
        Assertions.assertEquals("i", fields.get(8).getSimpleName().toString());
        Assertions.assertEquals("l", fields.get(9).getSimpleName().toString());
        Assertions.assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    private void testGetDeclaredField(TypeElement type, String fieldName, Type fieldType) {
        VariableElement field = getDeclaredField(type, fieldName);
        assertField(field, fieldName, fieldType);
    }

    private void testFindField(TypeElement type, String fieldName, Type fieldType) {
        VariableElement field = findField(type, fieldName);
        assertField(field, fieldName, fieldType);
    }

    private void assertField(VariableElement field, String fieldName, Type fieldType) {
        Assertions.assertEquals(fieldName, field.getSimpleName().toString());
        Assertions.assertEquals(fieldType.getTypeName(), field.asType().toString());
    }
}
