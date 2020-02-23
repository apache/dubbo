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
import org.apache.dubbo.metadata.annotation.processing.model.Color;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.TypeElement;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.dubbo.metadata.annotation.processing.builder.TypeDefinitionBuilder.build;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EnumTypeDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class EnumTypeDefinitionBuilderTest extends AbstractAnnotationProcessingTest {

    private EnumTypeDefinitionBuilder builder;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(Color.class);
    }

    @Override
    protected void beforeEach() {
        builder = new EnumTypeDefinitionBuilder();
    }

    @Test
    public void testAccept() {
        TypeElement typeElement = getType(Color.class);
        assertTrue(builder.accept(processingEnv, typeElement.asType()));
    }

    @Test
    public void testBuild() {
        TypeElement typeElement = getType(Color.class);
        TypeDefinition typeDefinition = build(processingEnv, typeElement);
        assertEquals(Color.class.getName(), typeDefinition.getType());
        assertEquals(asList("RED", "YELLOW", "BLUE"), typeDefinition.getEnums());
//        assertEquals(typeDefinition.getTypeBuilderName(), builder.getClass().getName());
    }

}
