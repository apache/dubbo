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
import org.apache.dubbo.metadata.annotation.processing.model.ArrayTypeModel;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ArrayTypeDefinitionBuilder} Test
 *
 * @since 2.7.5
 */
public class ArrayTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private ArrayTypeDefinitionBuilder builder;

    private TypeElement testType;

    private VariableElement integersField;

    private VariableElement stringsField;

    private VariableElement primitiveTypeModelsField;

    private VariableElement modelsField;

    private VariableElement colorsField;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(ArrayTypeModel.class);
    }

    @Override
    protected void beforeEach() {
        builder = new ArrayTypeDefinitionBuilder();
        testType = getType(ArrayTypeModel.class);
        integersField = getField(processingEnv, testType, "integers");
        stringsField = getField(processingEnv, testType, "strings");
        primitiveTypeModelsField = getField(processingEnv, testType, "primitiveTypeModels");
        modelsField = getField(processingEnv, testType, "models");
        colorsField = getField(processingEnv, testType, "colors");
    }

    @Test
    public void testAccept() {
        assertTrue(builder.accept(processingEnv, integersField.asType()));
        assertTrue(builder.accept(processingEnv, stringsField.asType()));
        assertTrue(builder.accept(processingEnv, primitiveTypeModelsField.asType()));
        assertTrue(builder.accept(processingEnv, modelsField.asType()));
        assertTrue(builder.accept(processingEnv, colorsField.asType()));
    }

    @Test
    public void testBuild() {

        TypeDefinition typeDefinition = TypeDefinitionBuilder.build(processingEnv, integersField);
        TypeDefinition subTypeDefinition = typeDefinition.getItems().get(0);
        assertEquals("int[]", typeDefinition.getType());
        assertEquals("int", subTypeDefinition.getType());

        typeDefinition = TypeDefinitionBuilder.build(processingEnv, stringsField);
        subTypeDefinition = typeDefinition.getItems().get(0);
        assertEquals("java.lang.String[]", typeDefinition.getType());
        assertEquals("java.lang.String", subTypeDefinition.getType());

        typeDefinition = TypeDefinitionBuilder.build(processingEnv, primitiveTypeModelsField);
        subTypeDefinition = typeDefinition.getItems().get(0);
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel[]", typeDefinition.getType());
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel", subTypeDefinition.getType());

        subTypeDefinition.getItems().forEach(def -> {
            
        });

        typeDefinition = TypeDefinitionBuilder.build(processingEnv, modelsField);
        subTypeDefinition = typeDefinition.getItems().get(0);
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.Model[]", typeDefinition.getType());
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.Model", subTypeDefinition.getType());

        typeDefinition = TypeDefinitionBuilder.build(processingEnv, colorsField);
        subTypeDefinition = typeDefinition.getItems().get(0);
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.Color[]", typeDefinition.getType());
        assertEquals("org.apache.dubbo.metadata.annotation.processing.model.Color", subTypeDefinition.getType());
    }
}
