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
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.metadata.annotation.processing.builder.ServiceDefinitionBuilder.build;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ServiceDefinitionBuilder} Test
 *
 * @since 2.7.6
 */
public class ServiceDefinitionBuilderTest extends AbstractAnnotationProcessingTest {


    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
        classesToBeCompiled.add(TestServiceImpl.class);
    }

    @Override
    protected void beforeEach() {
    }

    @Test
    public void testBuild() {
        ServiceDefinition serviceDefinition = build(processingEnv, getType(TestServiceImpl.class));
        assertEquals(TestServiceImpl.class.getTypeName(), serviceDefinition.getCanonicalName());
        assertEquals("org/apache/dubbo/metadata/tools/TestServiceImpl.class", serviceDefinition.getCodeSource());

        // types
        List<String> typeNames = Arrays.asList(
                "org.apache.dubbo.metadata.tools.TestServiceImpl",
                "org.apache.dubbo.metadata.tools.GenericTestService",
                "org.apache.dubbo.metadata.tools.DefaultTestService",
                "org.apache.dubbo.metadata.tools.TestService",
                "java.lang.AutoCloseable",
                "java.io.Serializable",
                "java.util.EventListener"
        );
        for (String typeName : typeNames) {
            String gotTypeName = getTypeName(typeName, serviceDefinition.getTypes());
            assertEquals(typeName, gotTypeName);
        }

        // methods
        assertEquals(14, serviceDefinition.getMethods().size());
    }

    private static String getTypeName(String type, List<TypeDefinition> types) {
        for (TypeDefinition typeDefinition : types) {
            if (type.equals(typeDefinition.getType())) {
                return typeDefinition.getType();
            }
        }
        return type;
    }
}
