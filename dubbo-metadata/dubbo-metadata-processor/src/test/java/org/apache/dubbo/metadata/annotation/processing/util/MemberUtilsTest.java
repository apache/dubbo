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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.util.ElementFilter.fieldsIn;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.getAllDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.hasModifiers;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.isPublicNonStatic;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.matchParameterTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.MethodUtils.findMethod;

/**
 * {@link MemberUtils} Test
 *
 * @since 2.7.6
 */
class MemberUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {}

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    void testIsPublicNonStatic() {
        Assertions.assertFalse(isPublicNonStatic(null));
        methodsIn(getDeclaredMembers(testType.asType()))
                .forEach(method -> Assertions.assertTrue(isPublicNonStatic(method)));
    }

    @Test
    void testHasModifiers() {
        Assertions.assertFalse(hasModifiers(null));
        List<? extends Element> members = getAllDeclaredMembers(testType.asType());
        List<VariableElement> fields = fieldsIn(members);
        Assertions.assertTrue(hasModifiers(fields.get(0), PRIVATE));
    }

    @Test
    void testDeclaredMembers() {
        TypeElement type = getType(Model.class);
        List<? extends Element> members = getDeclaredMembers(type.asType());
        List<VariableElement> fields = fieldsIn(members);
        Assertions.assertEquals(19, members.size());
        Assertions.assertEquals(6, fields.size());
        Assertions.assertEquals("f", fields.get(0).getSimpleName().toString());
        Assertions.assertEquals("d", fields.get(1).getSimpleName().toString());
        Assertions.assertEquals("tu", fields.get(2).getSimpleName().toString());
        Assertions.assertEquals("str", fields.get(3).getSimpleName().toString());
        Assertions.assertEquals("bi", fields.get(4).getSimpleName().toString());
        Assertions.assertEquals("bd", fields.get(5).getSimpleName().toString());

        members = getAllDeclaredMembers(type.asType());
        fields = fieldsIn(members);
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

    @Test
    void testMatchParameterTypes() {
        ExecutableElement method = findMethod(testType, "echo", "java.lang.String");
        Assertions.assertTrue(matchParameterTypes(method.getParameters(), "java.lang.String"));
        Assertions.assertFalse(matchParameterTypes(method.getParameters(), "java.lang.Object"));
    }
}
