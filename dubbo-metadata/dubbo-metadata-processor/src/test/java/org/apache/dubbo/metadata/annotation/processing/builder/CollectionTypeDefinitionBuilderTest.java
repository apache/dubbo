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
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.util.AnnotationProcessorUtils.getFields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CollectionTypeDefinitionBuilder} Test
 *
 * @since 2.7.5
 */
public class CollectionTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private CollectionTypeDefinitionBuilder builder;

    private List<VariableElement> fields;

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(CollectionTypeModel.class);
    }

    @Override
    protected void beforeEach() {
        builder = new CollectionTypeDefinitionBuilder();
        fields = getFields(processingEnv, getType(CollectionTypeModel.class));
        assertEquals(5, fields.size());
        testType = getType(CollectionTypeModel.class);
    }

    @Test
    public void testAccept() {
        fields.forEach(field -> {
            assertTrue(builder.accept(processingEnv, field.asType()));
        });
    }

    @Test
    public void testBuild() {

    }
}
