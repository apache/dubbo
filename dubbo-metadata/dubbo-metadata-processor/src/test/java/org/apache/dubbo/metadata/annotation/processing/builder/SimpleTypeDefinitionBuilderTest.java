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
package org.apache.dubbo.metadata.annotation.processing.builder;

import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.annotation.processing.model.SimpleTypeModel;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.builder.PrimitiveTypeDefinitionBuilderTest.buildAndAssertTypeDefinition;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SimpleTypeDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class SimpleTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private SimpleTypeDefinitionBuilder builder;

    private VariableElement vField;

    private VariableElement zField;

    private VariableElement cField;

    private VariableElement bField;

    private VariableElement sField;

    private VariableElement iField;

    private VariableElement lField;

    private VariableElement fField;

    private VariableElement dField;

    private VariableElement strField;

    private VariableElement bdField;

    private VariableElement biField;

    private VariableElement dtField;

    private VariableElement invalidField;


    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(SimpleTypeModel.class);
    }

    @Override
    protected void beforeEach() {
        builder = new SimpleTypeDefinitionBuilder();
        TypeElement testType = getType(SimpleTypeModel.class);
        vField = findField(testType, "v");
        zField = findField(testType, "z");
        cField = findField(testType, "c");
        bField = findField(testType, "b");
        sField = findField(testType, "s");
        iField = findField(testType, "i");
        lField = findField(testType, "l");
        fField = findField(testType, "f");
        dField = findField(testType, "d");
        strField = findField(testType, "str");
        bdField = findField(testType, "bd");
        biField = findField(testType, "bi");
        dtField = findField(testType, "dt");
        invalidField = findField(testType, "invalid");

        assertEquals("java.lang.Void", vField.asType().toString());
        assertEquals("java.lang.Boolean", zField.asType().toString());
        assertEquals("java.lang.Character", cField.asType().toString());
        assertEquals("java.lang.Byte", bField.asType().toString());
        assertEquals("java.lang.Short", sField.asType().toString());
        assertEquals("java.lang.Integer", iField.asType().toString());
        assertEquals("java.lang.Long", lField.asType().toString());
        assertEquals("java.lang.Float", fField.asType().toString());
        assertEquals("java.lang.Double", dField.asType().toString());
        assertEquals("java.lang.String", strField.asType().toString());
        assertEquals("java.math.BigDecimal", bdField.asType().toString());
        assertEquals("java.math.BigInteger", biField.asType().toString());
        assertEquals("java.util.Date", dtField.asType().toString());
        assertEquals("int", invalidField.asType().toString());
    }

    @Test
    public void testAccept() {
        assertTrue(builder.accept(processingEnv, vField.asType()));
        assertTrue(builder.accept(processingEnv, zField.asType()));
        assertTrue(builder.accept(processingEnv, cField.asType()));
        assertTrue(builder.accept(processingEnv, bField.asType()));
        assertTrue(builder.accept(processingEnv, sField.asType()));
        assertTrue(builder.accept(processingEnv, iField.asType()));
        assertTrue(builder.accept(processingEnv, lField.asType()));
        assertTrue(builder.accept(processingEnv, fField.asType()));
        assertTrue(builder.accept(processingEnv, dField.asType()));
        assertTrue(builder.accept(processingEnv, strField.asType()));
        assertTrue(builder.accept(processingEnv, bdField.asType()));
        assertTrue(builder.accept(processingEnv, biField.asType()));
        assertTrue(builder.accept(processingEnv, dtField.asType()));
        // false condition
        assertFalse(builder.accept(processingEnv, invalidField.asType()));
    }

    @Test
    public void testBuild() {
        buildAndAssertTypeDefinition(processingEnv, vField, builder);
        buildAndAssertTypeDefinition(processingEnv, zField, builder);
        buildAndAssertTypeDefinition(processingEnv, cField, builder);
        buildAndAssertTypeDefinition(processingEnv, sField, builder);
        buildAndAssertTypeDefinition(processingEnv, iField, builder);
        buildAndAssertTypeDefinition(processingEnv, lField, builder);
        buildAndAssertTypeDefinition(processingEnv, fField, builder);
        buildAndAssertTypeDefinition(processingEnv, dField, builder);
        buildAndAssertTypeDefinition(processingEnv, strField, builder);
        buildAndAssertTypeDefinition(processingEnv, bdField, builder);
        buildAndAssertTypeDefinition(processingEnv, biField, builder);
        buildAndAssertTypeDefinition(processingEnv, dtField, builder);
    }
}
