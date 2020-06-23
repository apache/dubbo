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
import org.apache.dubbo.metadata.annotation.processing.model.CollectionTypeModel;
import org.apache.dubbo.metadata.annotation.processing.model.Color;
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.annotation.processing.model.PrimitiveTypeModel;
import org.apache.dubbo.metadata.annotation.processing.model.SimpleTypeModel;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GeneralTypeDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class GeneralTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private GeneralTypeDefinitionBuilder builder;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(Model.class);
    }

    @Override
    protected void beforeEach() {
        builder = new GeneralTypeDefinitionBuilder();
    }

    @Test
    public void testAccept() {
        assertTrue(builder.accept(processingEnv, getType(Model.class).asType()));
        assertTrue(builder.accept(processingEnv, getType(PrimitiveTypeModel.class).asType()));
        assertTrue(builder.accept(processingEnv, getType(SimpleTypeModel.class).asType()));
        assertTrue(builder.accept(processingEnv, getType(ArrayTypeModel.class).asType()));
        assertTrue(builder.accept(processingEnv, getType(CollectionTypeModel.class).asType()));
        assertFalse(builder.accept(processingEnv, getType(Color.class).asType()));
    }

    @Test
    public void testBuild() {

    }
}
