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
import org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PrimitiveTypeDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class PrimitiveTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private PrimitiveTypeDefinitionBuilder builder;

    private VariableElement zField;

    private VariableElement bField;

    private VariableElement cField;

    private VariableElement sField;

    private VariableElement iField;

    private VariableElement lField;

    private VariableElement fField;

    private VariableElement dField;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(PrimitiveTypeModel.class);
    }

    @Override
    protected void beforeEach() {

        builder = new PrimitiveTypeDefinitionBuilder();

        TypeElement testType = getType(PrimitiveTypeModel.class);

        zField = findField( testType, "z");
        bField = findField( testType, "b");
        cField = findField( testType, "c");
        sField = findField( testType, "s");
        iField = findField( testType, "i");
        lField = findField( testType, "l");
        fField = findField( testType, "f");
        dField = findField( testType, "d");

        assertEquals("boolean", zField.asType().toString());
        assertEquals("byte", bField.asType().toString());
        assertEquals("char", cField.asType().toString());
        assertEquals("short", sField.asType().toString());
        assertEquals("int", iField.asType().toString());
        assertEquals("long", lField.asType().toString());
        assertEquals("float", fField.asType().toString());
        assertEquals("double", dField.asType().toString());
    }

    @Test
    public void testAccept() {
        assertTrue(builder.accept(processingEnv, zField.asType()));
        assertTrue(builder.accept(processingEnv, bField.asType()));
        assertTrue(builder.accept(processingEnv, cField.asType()));
        assertTrue(builder.accept(processingEnv, sField.asType()));
        assertTrue(builder.accept(processingEnv, iField.asType()));
        assertTrue(builder.accept(processingEnv, lField.asType()));
        assertTrue(builder.accept(processingEnv, fField.asType()));
        assertTrue(builder.accept(processingEnv, dField.asType()));
    }

    @Test
    public void testBuild() {
        buildAndAssertTypeDefinition(processingEnv, zField, builder);
        buildAndAssertTypeDefinition(processingEnv, bField, builder);
        buildAndAssertTypeDefinition(processingEnv, cField, builder);
        buildAndAssertTypeDefinition(processingEnv, sField, builder);
        buildAndAssertTypeDefinition(processingEnv, iField, builder);
        buildAndAssertTypeDefinition(processingEnv, lField, builder);
        buildAndAssertTypeDefinition(processingEnv, zField, builder);
        buildAndAssertTypeDefinition(processingEnv, fField, builder);
        buildAndAssertTypeDefinition(processingEnv, dField, builder);
    }

    static void buildAndAssertTypeDefinition(ProcessingEnvironment processingEnv, VariableElement field, TypeBuilder builder) {
        Map<String, TypeDefinition> typeCache = new HashMap<>();
        TypeDefinition typeDefinition = TypeDefinitionBuilder.build(processingEnv, field, typeCache);
        assertBasicTypeDefinition(typeDefinition, field.asType().toString(), builder);
//        assertEquals(field.getSimpleName().toString(), typeDefinition.get$ref());
    }

    static void assertBasicTypeDefinition(TypeDefinition typeDefinition, String type, TypeBuilder builder) {
        assertEquals(type, typeDefinition.getType());
//        assertEquals(builder.getClass().getName(), typeDefinition.getTypeBuilderName());
        assertTrue(typeDefinition.getProperties().isEmpty());
        assertTrue(typeDefinition.getItems().isEmpty());
        assertTrue(typeDefinition.getEnums().isEmpty());
    }
}
