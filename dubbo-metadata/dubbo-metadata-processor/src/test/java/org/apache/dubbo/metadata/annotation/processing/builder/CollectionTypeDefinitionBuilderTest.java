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
import org.apache.dubbo.metadata.annotation.processing.model.CollectionTypeModel;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.builder.ArrayTypeDefinitionBuilderTest.buildAndAssertTypeDefinition;
import static org.apache.dubbo.metadata.annotation.processing.util.FieldUtils.findField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CollectionTypeDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class CollectionTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private CollectionTypeDefinitionBuilder builder;

    private VariableElement stringsField;

    private VariableElement colorsField;

    private VariableElement primitiveTypeModelsField;

    private VariableElement modelsField;

    private VariableElement modelArraysField;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(CollectionTypeModel.class);
    }

    @Override
    protected void beforeEach() {
        builder = new CollectionTypeDefinitionBuilder();
        TypeElement testType = getType(CollectionTypeModel.class);
        stringsField = findField( testType, "strings");
        colorsField = findField( testType, "colors");
        primitiveTypeModelsField = findField( testType, "primitiveTypeModels");
        modelsField = findField( testType, "models");
        modelArraysField = findField( testType, "modelArrays");

        assertEquals("strings", stringsField.getSimpleName().toString());
        assertEquals("colors", colorsField.getSimpleName().toString());
        assertEquals("primitiveTypeModels", primitiveTypeModelsField.getSimpleName().toString());
        assertEquals("models", modelsField.getSimpleName().toString());
        assertEquals("modelArrays", modelArraysField.getSimpleName().toString());
    }

    @Test
    public void testAccept() {
        assertTrue(builder.accept(processingEnv, stringsField.asType()));
        assertTrue(builder.accept(processingEnv, colorsField.asType()));
        assertTrue(builder.accept(processingEnv, primitiveTypeModelsField.asType()));
        assertTrue(builder.accept(processingEnv, modelsField.asType()));
        assertTrue(builder.accept(processingEnv, modelArraysField.asType()));
    }

    @Test
    public void testBuild() {

        buildAndAssertTypeDefinition(processingEnv, stringsField, "java.util.Collection<java.lang.String>", "java.lang.String", builder);

        buildAndAssertTypeDefinition(processingEnv, colorsField, "java.util.List<org.apache.dubbo.metadata.annotation.processing.model.Color>",
                "org.apache.dubbo.metadata.annotation.processing.model.Color", builder);

        buildAndAssertTypeDefinition(processingEnv, primitiveTypeModelsField,
                "java.util.Queue<org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel>",
                "org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel", builder);

        buildAndAssertTypeDefinition(processingEnv, modelsField,
                "java.util.Deque<org.apache.dubbo.metadata.annotation.processing.model.Model>",
                "org.apache.dubbo.metadata.annotation.processing.model.Model", builder);

        buildAndAssertTypeDefinition(processingEnv, modelArraysField,
                "java.util.Set<org.apache.dubbo.metadata.annotation.processing.model.Model[]>",
                "org.apache.dubbo.metadata.annotation.processing.model.Model[]", builder);


    }
}
